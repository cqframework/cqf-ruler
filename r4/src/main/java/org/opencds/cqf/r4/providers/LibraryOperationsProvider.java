package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.common.providers.LibrarySourceProvider;
import org.opencds.cqf.library.r4.NarrativeProvider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.StringParam;

import org.hl7.fhir.instance.model.api.IBaseResource;

public class LibraryOperationsProvider implements LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> {

    private NarrativeProvider narrativeProvider;
    private DataRequirementsProvider dataRequirementsProvider;
    private LibraryResourceProvider libraryResourceProvider;

    public LibraryOperationsProvider(LibraryResourceProvider libraryResourceProvider, NarrativeProvider narrativeProvider) {
        this.narrativeProvider = narrativeProvider;
        this.dataRequirementsProvider = new DataRequirementsProvider();
        this.libraryResourceProvider = libraryResourceProvider;
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

    private LibrarySourceProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment> librarySourceProvider;

    private LibrarySourceProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment> getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new LibrarySourceProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment>(
                getLibraryResourceProvider(),
                x -> x.getContent(),
                x -> x.getContentType(),
                x -> x.getData());
        }
        return librarySourceProvider;
    }

    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> getLibraryResourceProvider() {
        return this;
    }

    @Operation(name = "$refresh-generated-content", type = Library.class)
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
            @IdParam IdType theId) {
        Library theResource = this.libraryResourceProvider.getDao().read(theId);
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

		try {
			Narrative n = this.narrativeProvider.getNarrative(this.libraryResourceProvider.getContext(), theResource);
			theResource.setText(n);
		} catch (Exception e) {
			//Ignore the exception so the resource still gets updated
		}

        return this.libraryResourceProvider.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-elm", idempotent = true, type = Library.class)
    public Parameters getElm(@IdParam IdType theId, @OptionalParam(name="format") String format) {
        Library theResource = this.libraryResourceProvider.getDao().read(theId);
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

    @Operation(name = "$get-narrative", idempotent = true, type = Library.class)
    public Parameters getNarrative(@IdParam IdType theId) {
        Library theResource = this.libraryResourceProvider.getDao().read(theId);
        Narrative n = this.narrativeProvider.getNarrative(this.libraryResourceProvider.getContext(), theResource);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }

    // TODO: Figure out if we should throw an exception or something here.
    @Override
    public void update(Library library) {
        this.libraryResourceProvider.getDao().update(library);
    }

    @Override
    public Library resolveLibraryById(String libraryId) {
        try {
            return this.libraryResourceProvider.getDao().read(new IdType(libraryId));
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("Could not resolve library id %s", libraryId));
        }
    }

    @Override
    public Library resolveLibraryByName(String libraryName, String libraryVersion) {
        Iterable<org.hl7.fhir.r4.model.Library> libraries = getLibrariesByName(libraryName);
        org.hl7.fhir.r4.model.Library library = LibraryResolutionProvider.selectFromList(libraries, libraryVersion, x -> x.getVersion());

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }

    private Iterable<org.hl7.fhir.r4.model.Library> getLibrariesByName(String name) {
        // Search for libraries by name
        SearchParameterMap map = new SearchParameterMap();
        map.add("name", new StringParam(name, true));
        ca.uhn.fhir.rest.api.server.IBundleProvider bundleProvider = this.libraryResourceProvider.getDao().search(map);

        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getResources(0, bundleProvider.size());
        return resolveLibraries(resourceList);
    }
    
    private Iterable<org.hl7.fhir.r4.model.Library> resolveLibraries(List< IBaseResource > resourceList) {
        List<org.hl7.fhir.r4.model.Library> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class<?> clazz = res.getClass();
            ret.add((org.hl7.fhir.r4.model.Library)clazz.cast(res));
        }
        return ret;
    }
}