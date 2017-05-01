package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.runtime.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Christopher Schuler on 4/29/2017.
 */
public class MedicationRequestResourceProvider extends JpaResourceProviderDstu3<MedicationRequest> {

    private JpaFhirDataProvider provider;

    public MedicationRequestResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaFhirDataProvider(providers);
    }

    @Operation(name = "$evaluate", idempotent = true)
    public MedicationRequest evaluateMME(@IdParam IdType theId, @RequiredParam(name="patient") String patientId) throws FHIRException, IOException, JAXBException {

        // 1. retrieve MedicationRequest
        MedicationRequest request = this.getDao().read(theId);

        // 2. extract medication code (Medication reference or Concept), quantity (dispenseRequest), and daysSupply (dispenseRequest.expectedSupplyDuration)
        Code rxNormCode = resolveRxNormCode(request);
        Integer rxQuantity = resolveRxQuantity(request);
        Integer rxDaysSupply = resolveRxDaysSupply(request);

        // 3. create library resource for elm xml file
        Library library = CqlLibraryReader.read(new File(Paths.get("src/main/resources/OMTKLogic-0.1.0.xml").toAbsolutePath().toString()));

        // 4. set parameters
        //      RxNormCode -> medication code extracted in step 2
        //      RxQuantity -> quantity from step 2
        //      RxDaysSupply   -> daysSupply from step 2
        Context context = new Context(library);
        context.setParameter(null, "RxNormCode", rxNormCode);
        context.setParameter(null, "RxQuantity", rxQuantity);
        context.setParameter(null, "RxDaysSupply", rxDaysSupply);

        // 5. resolve data provider
        // *** NOTE *** you must provide this file!
        String path = Paths.get("src/main/resources/OpioidManagementTerminologyKnowledge.accdb").toAbsolutePath().toString().replace("\\", "/");
        String connString = "jdbc:ucanaccess://" + path + ";memory=false;keepMirror=true";
        OmtkDataProvider provider = new OmtkDataProvider(connString);
        context.registerDataProvider("http://org.opencds/opioid-cds", provider);

        // 5. get result from the EvaluateMMEs expression
        Object result = context.resolveExpressionRef("TestCalculateMMEs").getExpression().evaluate(context);

        // 6. Analyze result
        request = analyzeResult(request, result);

        // 7. return MedicationRequest with added detected issue if tapering is recommended
        return request;
    }

    private MedicationRequest analyzeResult(MedicationRequest request, Object result) {
        // TODO: maybe some type checking?
        Iterator it = ((Iterable) result).iterator();
        Tuple tuple = (Tuple) it.next();
        BigDecimal mme = ((org.opencds.cqf.cql.runtime.Quantity)tuple.getElements().get("mme")).getValue();

        if (mme.compareTo(new BigDecimal("50")) >= 0 && mme.compareTo(new BigDecimal("90")) < 0) {
            String detail = String.format("High risk for opioid overdose - consider tapering. Total morphine milligram equivalent (MME) is %s mg/day. Taper to < 50.", mme.toString());
            return attachIssue(request, detail);
        }

        else if (mme.compareTo(new BigDecimal("90")) > 0) {
            String detail = String.format("High risk for opioid overdose - taper now. Total morphine milligram equivalent (MME) is %s mg/day. Taper to < 50.", mme.toString());
            return attachIssue(request, detail);
        }

        return request;
    }

    private MedicationRequest attachIssue(MedicationRequest request, String detail) {
        DetectedIssue issue = new DetectedIssue().setDetail(detail);

        // this goes against the spirit of a JPA server, but creating resources with the DAO is a bummer...
        // TODO: change server base string when depoloying
        IIdType id = provider.getFhirContext()
                .newRestfulGenericClient("http://measure.eval.kanvix.com/cql-measure-processor/baseDstu3/")
                .create().resource(issue).execute().getId();

        // There is an issue here with Reference being returned with base URL stripped ... giving up
        Reference ref = new Reference().setReference(id.getValue());
        List<Reference> list = request.hasDetectedIssue() ? request.getDetectedIssue() : new ArrayList<>();
        list.add(ref);
        return request.setDetectedIssue(list);
    }

    private Code resolveRxNormCode(MedicationRequest request) throws FHIRException {

        if (request.hasMedicationCodeableConcept()) {
            Coding concept = request.getMedicationCodeableConcept().getCodingFirstRep();
            return new Code().withCode(concept.getCode()).withSystem(concept.getSystem());
        }

        else if (request.hasMedicationReference()) {
            Medication medication = (Medication) request.getMedicationReference().getResource();
            if (medication.hasCode()) {
                Coding concept = medication.getCode().getCodingFirstRep();
                return new Code().withCode(concept.getCode()).withSystem(concept.getSystem());
            }
        }

        throw new IllegalArgumentException("Medication code must be provided in resource!");
    }

    private Integer resolveRxQuantity(MedicationRequest request) {

        if (request.hasDispenseRequest()) {
            if (request.getDispenseRequest().hasQuantity()) {
                return request.getDispenseRequest().getQuantity().getValue().intValue();
            }
        }

        throw new IllegalArgumentException("dispenseRequest quantity must be provided in resource!");
    }

    private Integer resolveRxDaysSupply(MedicationRequest request) {

        if (request.hasDispenseRequest()) {
            if (request.getDispenseRequest().hasExpectedSupplyDuration()) {
                return request.getDispenseRequest().getExpectedSupplyDuration().getValue().intValue();
            }
        }

        throw new IllegalArgumentException("dispenseRequest expectedSupplyDuration must be provided in resource!");
    }

    @Search(allowUnknownParams=true)
    public ca.uhn.fhir.rest.server.IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,

            ca.uhn.fhir.rest.method.RequestDetails theRequestDetails,

            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT)
                    StringAndListParam theFtContent,

            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TEXT)
                    StringAndListParam theFtText,

            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TAG)
                    TokenAndListParam theSearchForTag,

            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY)
                    TokenAndListParam theSearchForSecurity,

            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE)
                    UriAndListParam theSearchForProfile,

            @Description(shortDefinition="Return resources linked to by the given target")
            @OptionalParam(name="_has")
                    HasAndListParam theHas,

            @Description(shortDefinition="The ID of the resource")
            @OptionalParam(name="_id")
                    TokenAndListParam the_id,

            @Description(shortDefinition="The language of the resource")
            @OptionalParam(name="_language")
                    StringAndListParam the_language,

            @Description(shortDefinition="Return prescriptions written on this date")
            @OptionalParam(name="authoredon")
                    DateRangeParam theAuthoredon,

            @Description(shortDefinition="Returns prescriptions with different categories")
            @OptionalParam(name="category")
                    TokenAndListParam theCategory,

            @Description(shortDefinition="Return prescriptions of this medication code")
            @OptionalParam(name="code")
                    TokenAndListParam theCode,

            @Description(shortDefinition="Return prescriptions with this encounter or episode of care identifier")
            @OptionalParam(name="context", targetTypes={  } )
                    ReferenceAndListParam theContext,

            @Description(shortDefinition="Returns medication request to be administered on a specific date")
            @OptionalParam(name="date")
                    DateRangeParam theDate,

            @Description(shortDefinition="Return prescriptions with this external identifier")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,

            @Description(shortDefinition="Returns prescriptions intended to be dispensed by this Organization")
            @OptionalParam(name="intended-dispenser", targetTypes={  } )
                    ReferenceAndListParam theIntended_dispenser,

            @Description(shortDefinition="Returns prescriptions with different intents")
            @OptionalParam(name="intent")
                    TokenAndListParam theIntent,

            @Description(shortDefinition="Return prescriptions of this medication reference")
            @OptionalParam(name="medication", targetTypes={  } )
                    ReferenceAndListParam theMedication,

            @Description(shortDefinition="Returns prescriptions for a specific patient")
            @OptionalParam(name="patient", targetTypes={  } )
                    ReferenceAndListParam thePatient,

            @Description(shortDefinition="Returns prescriptions with different priorities")
            @OptionalParam(name="priority")
                    TokenAndListParam thePriority,

            @Description(shortDefinition="Returns prescriptions prescribed by this prescriber")
            @OptionalParam(name="requester", targetTypes={  } )
                    ReferenceAndListParam theRequester,

            @Description(shortDefinition="Status of the prescription")
            @OptionalParam(name="status")
                    TokenAndListParam theStatus,

            @Description(shortDefinition="The identity of a patient to list orders  for")
            @OptionalParam(name="subject", targetTypes={  } )
                    ReferenceAndListParam theSubject,

            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,

            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,

            @IncludeParam(allow= {
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "MedicationRequest:context",
                    "MedicationRequest:intended-dispenser",
                    "MedicationRequest:medication",
                    "MedicationRequest:patient",
                    "MedicationRequest:requester",
                    "MedicationRequest:subject",
                    "*"
            })
                    Set<Include> theIncludes,

            @Sort
                    SortSpec theSort,

            @ca.uhn.fhir.rest.annotation.Count
                    Integer theCount
    ) {
        startRequest(theServletRequest);
        try {
            SearchParameterMap paramMap = new SearchParameterMap();
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
            paramMap.add("_has", theHas);
            paramMap.add("_id", the_id);
            paramMap.add("_language", the_language);
            paramMap.add("authoredon", theAuthoredon);
            paramMap.add("category", theCategory);
            paramMap.add("code", theCode);
            paramMap.add("context", theContext);
            paramMap.add("date", theDate);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("intended-dispenser", theIntended_dispenser);
            paramMap.add("intent", theIntent);
            paramMap.add("medication", theMedication);
            paramMap.add("patient", thePatient);
            paramMap.add("priority", thePriority);
            paramMap.add("requester", theRequester);
            paramMap.add("status", theStatus);
            paramMap.add("subject", theSubject);
            paramMap.setRevIncludes(theRevIncludes);
            paramMap.setLastUpdated(theLastUpdated);
            paramMap.setIncludes(theIncludes);
            paramMap.setSort(theSort);
            paramMap.setCount(theCount);
            paramMap.setRequestDetails(theRequestDetails);

            getDao().translateRawParameters(theAdditionalRawParams, paramMap);

            return getDao().search(paramMap);
        } finally {
            endRequest(theServletRequest);
        }
    }
}
