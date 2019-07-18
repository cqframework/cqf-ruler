package org.opencds.cqf.providers;

import javax.servlet.http.HttpServletRequest;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.config.STU3LibrarySourceProvider;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class NarrativeLibraryResourceProvider extends LibraryResourceProvider {

    private NarrativeProvider narrativeProvider;
    private DataRequirementsProvider dataRequirementsProvider;

    public NarrativeLibraryResourceProvider(NarrativeProvider narrativeProvider) {
        this.narrativeProvider = narrativeProvider;
        this.dataRequirementsProvider = new DataRequirementsProvider();
    }

    private ModelManager getModelManager() {
        return new ModelManager();
    }


    private LibraryManager getLibraryManager(ModelManager modelManager)
    {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());

        return libraryManager;
    }

    private STU3LibrarySourceProvider librarySourceProvider;

    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return this;
    }

    @Operation(name = "$refresh-generated-content")
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
            @IdParam IdType theId) {
        Library theResource = this.getDao().read(theId);
        //this.formatCql(theResource);

        ModelManager modelManager = this.getModelManager();
        LibraryManager libraryManager = this.getLibraryManager(modelManager);

        CqlTranslator translator = this.dataRequirementsProvider.getTranslator(theResource, libraryManager, modelManager);
        if (translator.getErrors().size() > 0) {
            throw new RuntimeException("Errors during library compilation.");
        }
        
        this.dataRequirementsProvider.ensureElm(theResource, translator);
        this.dataRequirementsProvider.ensureRelatedArtifacts(theResource, translator, this);
        this.dataRequirementsProvider.ensureDataRequirements(theResource, translator);

        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), theResource);
        theResource.setText(n);

        return super.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-elm", idempotent = true)
    public Parameters getElm(@IdParam IdType theId, @OptionalParam(name="format") String format) {
        Library theResource = this.getDao().read(theId);
        // this.formatCql(theResource);

        ModelManager modelManager = this.getModelManager();
        LibraryManager libraryManager = this.getLibraryManager(modelManager);


        String elm = "";
        CqlTranslator translator = this.dataRequirementsProvider.getTranslator(theResource, libraryManager, modelManager);
        if (translator != null) {
            if (format.equals("json")) {
                elm = translator.toJson();
            }
            else {
                elm = translator.toXml();
            }
        }
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(elm));
        return p;
    }

    @Operation(name = "$get-narrative", idempotent = true)
    public Parameters getNarrative(@IdParam IdType theId) {
        Library theResource = this.getDao().read(theId);
        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), theResource);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }
}