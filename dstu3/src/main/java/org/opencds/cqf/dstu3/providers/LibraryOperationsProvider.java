package org.opencds.cqf.dstu3.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.common.providers.LibraryContentProvider;
import org.opencds.cqf.dstu3.helpers.LibraryHelper;
import org.opencds.cqf.tooling.library.stu3.NarrativeProvider;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;

@Component
public class LibraryOperationsProvider implements LibraryResolutionProvider<Library> {

    private NarrativeProvider narrativeProvider;
    private DataRequirementsProvider dataRequirementsProvider;
    private LibraryResourceProvider libraryResourceProvider;
    private LibraryHelper libraryHelper;

    @Inject
    public LibraryOperationsProvider(LibraryResourceProvider libraryResourceProvider,
            NarrativeProvider narrativeProvider, DataRequirementsProvider dataRequirementsProvider, LibraryHelper libraryHelper) {
        this.narrativeProvider = narrativeProvider;
        this.dataRequirementsProvider = dataRequirementsProvider;
        this.libraryResourceProvider = libraryResourceProvider;
        this.libraryHelper = libraryHelper;
    }

    private ModelManager getModelManager() {
        return new ModelManager();
    }

    private LibraryManager getLibraryManager(ModelManager modelManager) {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());

        return libraryManager;
    }

    private LibraryContentProvider<Library, Attachment> librarySourceProvider;

    private LibraryContentProvider<Library, Attachment> getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new LibraryContentProvider<Library, Attachment>(this.getLibraryResolutionProvider(),
                    x -> x.getContent(), x -> x.getContentType(), x -> x.getData());
        }
        return librarySourceProvider;
    }

    private LibraryResolutionProvider<Library> getLibraryResolutionProvider() {
        return this;
    }

    @Operation(name = "$data-requirements", idempotent = true, type = Library.class)
    public Library dataRequirements(@IdParam IdType theId, @OperationParam(name = "target") String target) throws InternalErrorException, FHIRException {
        ModelManager modelManager = libraryHelper.getModelManager();
        LibraryManager libraryManager = libraryHelper.getLibraryManager(this);

        Library library = this.libraryResourceProvider.getDao().read(theId);
        if (library == null) {
            throw new RuntimeException("Could not load library.");
        }

        CqlTranslator translator = TranslatorHelper.getTranslator(LibraryHelper.extractContentStream(library), libraryManager, modelManager);
        if (translator.getErrors().size() > 0) {
            throw new RuntimeException("Errors during library compilation.");
        }

        Library resultLibrary =
                this.dataRequirementsProvider.getModuleDefinitionLibrary(libraryManager,
                        translator.getTranslatedLibrary(), TranslatorHelper.getTranslatorOptions());

        return resultLibrary;
    }

    @Operation(name = "$refresh-generated-content", type = Library.class)
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
            @IdParam IdType theId) {
        Library theResource = this.libraryResourceProvider.getDao().read(theId);
        // this.formatCql(theResource);

        ModelManager modelManager = this.getModelManager();
        LibraryManager libraryManager = this.getLibraryManager(modelManager);

        CqlTranslator translator = this.dataRequirementsProvider.getTranslator(theResource, libraryManager,
                modelManager);
        if (translator.getErrors().size() > 0) {
            throw new RuntimeException("Errors during library compilation.");
        }

        this.dataRequirementsProvider.ensureElm(theResource, translator);
        this.dataRequirementsProvider.ensureRelatedArtifacts(theResource, translator, this);
        this.dataRequirementsProvider.ensureDataRequirements(theResource, translator);

        Narrative n = this.narrativeProvider.getNarrative(this.libraryResourceProvider.getContext(), theResource);
        theResource.setText(n);

        return this.libraryResourceProvider.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-elm", idempotent = true, type = Library.class)
    public Parameters getElm(@IdParam IdType theId, @OperationParam(name = "format") String format) {
        Library theResource = this.libraryResourceProvider.getDao().read(theId);
        // this.formatCql(theResource);

        ModelManager modelManager = this.getModelManager();
        LibraryManager libraryManager = this.getLibraryManager(modelManager);

        String elm = "";
        CqlTranslator translator = this.dataRequirementsProvider.getTranslator(theResource, libraryManager,
                modelManager);
        if (translator != null) {
            if (format.equals("json")) {
                elm = translator.toJson();
            } else {
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
    public Library resolveLibraryById(String libraryId, RequestDetails requestDetails) {
        try {
            return this.libraryResourceProvider.getDao().read(new IdType(libraryId), requestDetails);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Could not resolve library id %s", libraryId));
        }
    }

    @Override 
    public Library resolveLibraryByCanonicalUrl(String url, RequestDetails requestDetails) {
        Objects.requireNonNull(url, "url must not be null");

        String[] parts = url.split("\\|");
        String resourceUrl = parts[0];
        String version = null;
        if (parts.length > 1) {
            version = parts[1];
        }

        SearchParameterMap map = SearchParameterMap.newSynchronous();
        map.add("url", new UriParam(resourceUrl));
        if (version != null) {
            map.add("version", new TokenParam(version));
        }

        ca.uhn.fhir.rest.api.server.IBundleProvider bundleProvider = this.libraryResourceProvider.getDao().search(map);

        if (bundleProvider.size() == 0) {
            return null;
        }
        List<IBaseResource> resourceList = bundleProvider.getAllResources();
        return  LibraryResolutionProvider.selectFromList(resolveLibraries(resourceList), version, x -> x.getVersion());
    }

    @Override
    public Library resolveLibraryByName(String libraryName, String libraryVersion) {
        Iterable<org.hl7.fhir.dstu3.model.Library> libraries = getLibrariesByName(libraryName);
        org.hl7.fhir.dstu3.model.Library library = LibraryResolutionProvider.selectFromList(libraries, libraryVersion,
                x -> x.getVersion());

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }

    private Iterable<org.hl7.fhir.dstu3.model.Library> getLibrariesByName(String name) {
        // Search for libraries by name
        SearchParameterMap map = SearchParameterMap.newSynchronous();
        map.add("name", new StringParam(name, true));
        ca.uhn.fhir.rest.api.server.IBundleProvider bundleProvider = this.libraryResourceProvider.getDao().search(map);

        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getAllResources();
        return resolveLibraries(resourceList);
    }

    private Iterable<org.hl7.fhir.dstu3.model.Library> resolveLibraries(List<IBaseResource> resourceList) {
        List<org.hl7.fhir.dstu3.model.Library> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class<?> clazz = res.getClass();
            ret.add((org.hl7.fhir.dstu3.model.Library) clazz.cast(res));
        }
        return ret;
    }
}