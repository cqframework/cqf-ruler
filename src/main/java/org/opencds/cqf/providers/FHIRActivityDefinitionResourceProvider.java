package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.exceptions.ActivityDefinitionApplyException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Bryn on 1/16/2017.
 */
public class FHIRActivityDefinitionResourceProvider extends JpaResourceProviderDstu3<ActivityDefinition> {

    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public FHIRActivityDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

    @Operation(name = "$apply", idempotent = true)
    public Resource apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                          @OptionalParam(name="encounter") String encounterId,
                          @OptionalParam(name="practitioner") String practitionerId,
                          @OptionalParam(name="organization") String organizationId,
                          @OptionalParam(name="userType") String userType,
                          @OptionalParam(name="userLanguage") String userLanguage,
                          @OptionalParam(name="userTaskContext") String userTaskContext,
                          @OptionalParam(name="setting") String setting,
                          @OptionalParam(name="settingContext") String settingContext)
            throws InternalErrorException, FHIRException, ClassNotFoundException, IllegalAccessException, InstantiationException, ActivityDefinitionApplyException {
        ActivityDefinition activityDefinition = this.getDao().read(theId);

        Resource result = null;
        try {
            // This is a little hacky...
            result = (Resource) Class.forName("org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode()).newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new FHIRException("Could not find org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode());
        }

        switch (result.fhirType()) {
            case "ProcedureRequest":
                result = resolveProcedureRequest(activityDefinition, patientId, practitionerId, organizationId);
                break;

            case "MedicationRequest":
                result = resolveMedicationRequest(activityDefinition, patientId);
                break;

            case "SupplyRequest":
                result = resolveSupplyRequest(activityDefinition, practitionerId, organizationId);
                break;
        }

        // TODO: Apply expression extensions on any element?

        for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : activityDefinition.getDynamicValue())
        {
            if (dynamicValue.getExpression() != null) {
                /*
                    TODO: Passing the activityDefinition as context here because that's what will have the libraries,
                    but perhaps the "context" here should be the result resource?
                */
                Object value =
                        executionProvider.evaluateInContext(activityDefinition, dynamicValue.getExpression(), patientId);
                this.provider.setValue(result, dynamicValue.getPath(), value);
            }
        }

        return result;
    }

    private ProcedureRequest resolveProcedureRequest(ActivityDefinition activityDefinition, String patientId,
                                                     String practitionerId, String organizationId)
            throws ActivityDefinitionApplyException
    {
        // status, intent, code, and subject are required
        ProcedureRequest procedureRequest = new ProcedureRequest();
        procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
        procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.ORDER);
        procedureRequest.setSubject(new Reference(patientId));

        if (practitionerId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent()
                            .setAgent(new Reference(practitionerId))
            );
        }

        else if (organizationId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent()
                            .setAgent(new Reference(organizationId))
            );
        }

        if (activityDefinition.hasCode()) {
            procedureRequest.setCode(activityDefinition.getCode());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            procedureRequest.setBodySite( activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to "+activityDefinition.getKind());
        }

        return procedureRequest;
    }

    private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition, String patientId)
            throws ActivityDefinitionApplyException
    {
        // intent, medication, and subject are required
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(patientId));

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication( activityDefinition.getProduct());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required product property");
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction( activityDefinition.getDosage());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("Bodysite does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new ActivityDefinitionApplyException("Code does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new ActivityDefinitionApplyException("Quantity does not map to " + activityDefinition.getKind());
        }

        return medicationRequest;
    }

    private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition, String practionerId,
                                               String organizationId) throws ActivityDefinitionApplyException
    {
        SupplyRequest supplyRequest = new SupplyRequest();

        if (practionerId != null) {
            supplyRequest.setRequester(
                    new SupplyRequest.SupplyRequestRequesterComponent()
                            .setAgent(new Reference(practionerId))
            );
        }

        if (organizationId != null) {
            supplyRequest.setRequester(
                    new SupplyRequest.SupplyRequestRequesterComponent()
                            .setAgent(new Reference(organizationId))
            );
        }

        if (activityDefinition.hasQuantity()){
            supplyRequest.setOrderedItem(
                    new SupplyRequest.SupplyRequestOrderedItemComponent()
                            .setQuantity( activityDefinition.getQuantity())
            );
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.getOrderedItem().setItem(activityDefinition.getCode());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("Bodysite does not map to "+activityDefinition.getKind());
        }

        return supplyRequest;
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
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="composed-of", targetTypes={  } )
                    ReferenceAndListParam theComposed_of,
            @Description(shortDefinition="The activity definition publication date")
            @OptionalParam(name="date")
                    DateRangeParam theDate,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="depends-on", targetTypes={  } )
                    ReferenceAndListParam theDepends_on,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="derived-from", targetTypes={  } )
                    ReferenceAndListParam theDerived_from,
            @Description(shortDefinition="The description of the activity definition")
            @OptionalParam(name="description")
                    StringAndListParam theDescription,
            @Description(shortDefinition="The time during which the activity definition is intended to be in use")
            @OptionalParam(name="effective")
                    DateRangeParam theEffective,
            @Description(shortDefinition="External identifier for the activity definition")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition="Intended jurisdiction for the activity definition")
            @OptionalParam(name="jurisdiction")
                    TokenAndListParam theJurisdiction,
            @Description(shortDefinition="Computationally friendly name of the activity definition")
            @OptionalParam(name="name")
                    StringAndListParam theName,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="predecessor", targetTypes={  } )
                    ReferenceAndListParam thePredecessor,
            @Description(shortDefinition="Name of the publisher of the activity definition")
            @OptionalParam(name="publisher")
                    StringAndListParam thePublisher,
            @Description(shortDefinition="The current status of the activity definition")
            @OptionalParam(name="status")
                    TokenAndListParam theStatus,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="successor", targetTypes={  } )
                    ReferenceAndListParam theSuccessor,
            @Description(shortDefinition="The human-friendly name of the activity definition")
            @OptionalParam(name="title")
                    StringAndListParam theTitle,
            @Description(shortDefinition="Topics associated with the module")
            @OptionalParam(name="topic")
                    TokenAndListParam theTopic,
            @Description(shortDefinition="The uri that identifies the activity definition")
            @OptionalParam(name="url")
                    UriAndListParam theUrl,
            @Description(shortDefinition="The business version of the activity definition")
            @OptionalParam(name="version")
                    TokenAndListParam theVersion,
            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,
            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,
            @IncludeParam(allow= {
                    "ActivityDefinition:composed-of" , 					"ActivityDefinition:depends-on" , 					"ActivityDefinition:derived-from" , 					"ActivityDefinition:predecessor" , 					"ActivityDefinition:successor" , 						"ActivityDefinition:composed-of" , 					"ActivityDefinition:depends-on" , 					"ActivityDefinition:derived-from" , 					"ActivityDefinition:predecessor" , 					"ActivityDefinition:successor" , 						"ActivityDefinition:composed-of" , 					"ActivityDefinition:depends-on" , 					"ActivityDefinition:derived-from" , 					"ActivityDefinition:predecessor" , 					"ActivityDefinition:successor" , 						"ActivityDefinition:composed-of" , 					"ActivityDefinition:depends-on" , 					"ActivityDefinition:derived-from" , 					"ActivityDefinition:predecessor" , 					"ActivityDefinition:successor" , 						"ActivityDefinition:composed-of" , 					"ActivityDefinition:depends-on" , 					"ActivityDefinition:derived-from" , 					"ActivityDefinition:predecessor" , 					"ActivityDefinition:successor" 					, "*"
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
            paramMap.add("composed-of", theComposed_of);
            paramMap.add("date", theDate);
            paramMap.add("depends-on", theDepends_on);
            paramMap.add("derived-from", theDerived_from);
            paramMap.add("description", theDescription);
            paramMap.add("effective", theEffective);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("jurisdiction", theJurisdiction);
            paramMap.add("name", theName);
            paramMap.add("predecessor", thePredecessor);
            paramMap.add("publisher", thePublisher);
            paramMap.add("status", theStatus);
            paramMap.add("successor", theSuccessor);
            paramMap.add("title", theTitle);
            paramMap.add("topic", theTopic);
            paramMap.add("url", theUrl);
            paramMap.add("version", theVersion);
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
