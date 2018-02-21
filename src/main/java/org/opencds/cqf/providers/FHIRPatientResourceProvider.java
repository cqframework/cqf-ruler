package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Bryn on 1/16/2017.
 */
public class FHIRPatientResourceProvider extends PatientResourceProvider {

    private JpaDataProvider provider;

    public FHIRPatientResourceProvider(Collection<IResourceProvider> providers) {
        super();
        this.provider = new JpaDataProvider(providers);
    }


    /**
     * Patient/123/$export
     * @param theRequestDetails
     */
    //@formatter:off
    @Operation(name = "export", idempotent = true, bundleType=BundleTypeEnum.SEARCHSET)
    public IBaseResource patientInstanceEverything(

            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,

            @IdParam IdType theId,

            @Description(formalDefinition="Indicates the preferred output form for the call.")
            @OptionalParam(name="outputFormat") String outputFormat,

            @Description(formalDefinition="Resources updated after this period will be included in the response.")
            @OptionalParam(name="since") InstantType since,

            @Description(formalDefinition="string of comma-delimited FHIR resource types.")
            @OperationParam(name = "type", min=0, max=OperationParam.MAX_UNLIMITED) String  type

    ) {

        if (theRequestDetails.getHeader("Accept") == null) {
            return createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        DateRangeParam theLastUpdated = null;
        if ( since !=null ) {
            theLastUpdated = new DateRangeParam();
            theLastUpdated.setLowerBound(new DateParam().setValue(since.getValue()));
        }

        startRequest(theServletRequest);
        try {
//            return ((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceEverything(theServletRequest, theId, theCount, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), theRequestDetails);

            IBundleProvider iBundleProvider = ((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceEverything(theServletRequest, theId, null, theLastUpdated, null, null, null, theRequestDetails);
            return( createExportBundle( iBundleProvider, type, theRequestDetails.getFhirServerBase() ));
        } finally {
            endRequest(theServletRequest);
        }
    }



    /**
     * /Patient/$export
     * @param theRequestDetails
     */
    //@formatter:off
    @Operation(name = "export", idempotent = true, bundleType=BundleTypeEnum.SEARCHSET)
    public IBaseResource patientTypeEverything(

            javax.servlet.http.HttpServletRequest theServletRequest,

            @Description(formalDefinition="Indicates the preferred output form for the call.")
            @OptionalParam(name="outputFormat") String outputFormat,

            @Description(formalDefinition="Resources updated after this period will be included in the response.")
            @OptionalParam(name="since") InstantType since,

            @Description(formalDefinition="string of comma-delimited FHIR resource types.")
            @OperationParam(name = "type", min=0, max=OperationParam.MAX_UNLIMITED) String  type,

            RequestDetails theRequestDetails
    ) {
        if (theRequestDetails.getHeader("Accept") == null) {
            return createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        //@formatter:on
        DateRangeParam theLastUpdated = null;
        if ( since !=null ) {
            theLastUpdated = new DateRangeParam();
            theLastUpdated.setLowerBound(new DateParam().setValue(since.getValue()));
            throw new NotImplementedException("Since is not yet supported");
        }
        String cookieStr = null;
        if ( theServletRequest.getCookies()!=null && theServletRequest.getCookies().length>0 ){
            cookieStr = theServletRequest.getCookies()[0].getValue();
        }


        startRequest(theServletRequest);
        try {

            IBundleProvider iBundleProvider =((IFhirResourceDaoPatient<Patient>) getDao()).patientTypeEverything(theServletRequest, null, theLastUpdated, null, null, null, theRequestDetails);
            return( createExportBundle( iBundleProvider, type, theRequestDetails.getFhirServerBase() ));
        } finally {
            endRequest(theServletRequest);
        }

    }

    private Bundle createExportBundle(IBundleProvider iBundleProvider, String type, String fhirServerBase) {
        List<String> typeFilter = new ArrayList<>();
        typeFilter.add("Patient");
        if ( type!=null ) {
            for (String str : type.split(",")) {
                if (!str.isEmpty()) {
                    typeFilter.add(str);
                }
            }
        }
        Bundle bundle = new Bundle();
        List<Resource> resourceList = iBundleProvider.getResources(0,iBundleProvider.size()).stream()
                .filter( obj -> obj instanceof Resource )
                .map( obj -> (Resource)obj )
                .filter( resource -> type==null || typeFilter.stream().anyMatch( typeFilterStr -> typeFilterStr.equals( resource.fhirType())))
                .collect(Collectors.toList());
        bundle.setTotal(resourceList.size());
        resourceList.stream()
                .forEach( resource ->
                        bundle.addEntry()
                                .setResource(resource)
                                .setFullUrl(fhirServerBase+resource.getId())
                );

        return bundle;
    }

    public OperationOutcome createErrorOutcome(String display) {
        Coding code = new Coding().setDisplay(display);
        return new OperationOutcome().addIssue(
                new OperationOutcome.OperationOutcomeIssueComponent()
                        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                        .setCode(OperationOutcome.IssueType.PROCESSING)
                        .setDetails(new CodeableConcept().addCoding(code))
        );
    }
}
