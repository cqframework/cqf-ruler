package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;

import javax.servlet.http.HttpServletRequest;

public class NarrativeLibraryResourceProvider extends LibraryResourceProvider {

    private NarrativeProvider narrativeProvider;

    public NarrativeLibraryResourceProvider(NarrativeProvider narrativeProvider) {
        this.narrativeProvider = narrativeProvider;
    }

    @Operation(name="refresh-generated-content")
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails, @IdParam IdType theId) {
        Library theResource = this.getDao().read(theId);
        this.narrativeProvider.generateNarrative(this.getContext(), theResource);
        return super.update(theRequest, theResource, theId, theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }
}