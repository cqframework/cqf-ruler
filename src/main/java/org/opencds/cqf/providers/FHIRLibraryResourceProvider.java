package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.opencds.cqf.config.STU3LibraryLoader;

import javax.servlet.http.HttpServletRequest;

public class FHIRLibraryResourceProvider extends LibraryResourceProvider {

    private STU3LibraryLoader libraryLoader;

    public FHIRLibraryResourceProvider(STU3LibraryLoader libraryLoader) {
        this.libraryLoader = libraryLoader;
    }

    @Override
    @Create
    public MethodOutcome create(
            HttpServletRequest theRequest,
            @ResourceParam Library theResource,
            @ConditionalUrlParam String theConditional,
            RequestDetails theRequestDetails)
    {
        updateLibraryCache(theResource);
        return super.create(theRequest, theResource, theConditional, theRequestDetails);
    }

    @Override
    @Update
    public MethodOutcome update(
            HttpServletRequest theRequest,
            @ResourceParam Library theResource,
            @IdParam IdType theId,
            @ConditionalUrlParam String theConditional,
            RequestDetails theRequestDetails)
    {
        updateLibraryCache(theResource);
        return super.update(theRequest, theResource, theId, theConditional, theRequestDetails);
    }

    private void updateLibraryCache(Library library) {
        // libraryLoader
        //         .putLibrary(
        //                 library.getIdElement().getIdPart(),
        //                 libraryLoader.toElmLibrary(library)
        //         );
    }
}