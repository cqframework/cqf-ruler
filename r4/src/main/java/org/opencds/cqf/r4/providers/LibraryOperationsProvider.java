package org.opencds.cqf.r4.providers;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.common.providers.LibraryContentProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.r4.helpers.CanonicalHelper;
import org.opencds.cqf.r4.helpers.ParametersHelper;
import org.opencds.cqf.tooling.library.r4.NarrativeProvider;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import org.opencds.cqf.r4.helpers.LibraryHelper;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;

@Component
public class LibraryOperationsProvider implements LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> {

    private NarrativeProvider narrativeProvider;
    private DataRequirementsProvider dataRequirementsProvider;
    private LibraryResourceProvider libraryResourceProvider;
    DaoRegistry registry;
    TerminologyProvider defaultTerminologyProvider;
    private LibraryHelper libraryHelper;
    private LibraryProcessor libraryProcessor;

    @Inject
    public LibraryOperationsProvider(LibraryResourceProvider libraryResourceProvider, NarrativeProvider narrativeProvider,
            DaoRegistry registry, TerminologyProvider defaultTerminologyProvider, DataRequirementsProvider dataRequirementsProvider,
            LibraryHelper libraryHelper, LibraryProcessor libraryProcessor) {
        this.narrativeProvider = narrativeProvider;
        this.dataRequirementsProvider = dataRequirementsProvider;
        this.libraryResourceProvider = libraryResourceProvider;
        this.registry = registry;
        this.defaultTerminologyProvider = defaultTerminologyProvider;
        this.libraryHelper = libraryHelper;
        this.libraryProcessor = libraryProcessor;
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

    private LibraryContentProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment> getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new LibraryContentProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment>(
                    getLibraryResourceProvider(), x -> x.getContent(), x -> x.getContentType(), x -> x.getData());
        }
        return librarySourceProvider;
    }

    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> getLibraryResourceProvider() {
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

        try {
            Narrative n = this.narrativeProvider.getNarrative(this.libraryResourceProvider.getContext(), theResource);
            theResource.setText(n);
        } catch (Exception e) {
            // Ignore the exception so the resource still gets updated
        }

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

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Operation(name = "$evaluate", idempotent = true, type = Library.class)
    public Parameters evaluate(@IdParam IdType theId, @ResourceParam Parameters parameters) {
        Parameters response = new Parameters();

        CanonicalType library = (CanonicalType) parameters.getParameter("library");
        CanonicalType subject = (CanonicalType) parameters.getParameter("subject");
        Set<String> expression = parameters.hasParameter("expression") ?
            parameters.getParameters("expression").stream().map(e -> (String)e.toString()).collect(Collectors.toSet())
            : null;
        Parameters parametersParameters = parameters.hasParameter("parameters") ?
            (Parameters) ParametersHelper.getParameter(parameters, "parameters").getResource()
            : null;

        boolean useServerData = parameters.hasParameter("useServerData") ?
                ((BooleanType) parameters.getParameter("useServerData")).booleanValue()
                : true;

        Bundle data = parameters.hasParameter("data") ?
            (Bundle) ParametersHelper.getParameter(parameters, "data").getResource()
            : null;

        /* prefetchData */
        StringType prefetchDataKey = (StringType) parameters.getParameter("prefetchData.key");

        DataRequirement prefetchDataDescription = (DataRequirement) parameters.getParameter("prefetchData.description");

        Bundle prefetchDataData = parameters.hasParameter("prefetchData.data") ?
            (Bundle) ParametersHelper.getParameter(parameters, "prefetchData.data").getResource()
            : null;

        Endpoint dataEndpoint = parameters.hasParameter("dataEndpoint") ?
            (Endpoint) ParametersHelper.getParameter(parameters, "dataEndpoint").getResource()
            : null;

        Endpoint contentEndpoint = parameters.hasParameter("contentEndpoint") ?
            (Endpoint) ParametersHelper.getParameter(parameters, "contentEndpoint").getResource()
            : null;

        Endpoint terminologyEndpoint = parameters.hasParameter("terminologyEndpoint") ?
            (Endpoint) ParametersHelper.getParameter(parameters, "terminologyEndpoint").getResource()
            : null;

        String contextParam = CanonicalHelper.getResourceName(subject);
        String contextValue = CanonicalHelper.getId(subject);
        if (subject != null && contextParam.equals("Patient") && contextValue == null) {
            throw new IllegalArgumentException("Must specify a patientId when executing in Patient context.");
        }

        Bundle evaluationData = data != null ? data : prefetchDataData;

        Bundle libraryBundle = new Bundle();
        Library libraryResource = fetchLibraryFromBundle(evaluationData, libraryBundle, theId);

        if (libraryResource != null && useServerData) {
            for (BundleEntryComponent entry : libraryBundle.getEntry()) {
                this.update((Library) entry.getResource());
            }
        }

        if (libraryResource == null && useServerData) {
            libraryResource = this.libraryResourceProvider.getDao().read(theId);
            if (libraryResource == null) {
                throw new IllegalArgumentException("Library is not provided or not found in the server");
            }
            if (evaluationData != null) {
                evaluationData.addEntry((new BundleEntryComponent().setResource(libraryResource)));
            }
        }

        VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId(libraryResource.getName())
                .withVersion(libraryResource.getVersion());

        //TODO: library endpoint
        Endpoint libraryEndpoint = null;

        response = (Parameters)libraryProcessor.evaluate(libraryIdentifier, contextValue, parametersParameters, libraryEndpoint,
                terminologyEndpoint, dataEndpoint, evaluationData, expression);

        return response;
    }

    private Library fetchLibraryFromBundle(Bundle evaluationData, Bundle libraryBundle, IdType theId) {
        if (evaluationData != null) {
            for (BundleEntryComponent entry : evaluationData.getEntry()) {
                if (entry.hasResource() && entry.getResource().fhirType().equals("Library")) {
                    Bundle.BundleEntryComponent newItem = new Bundle.BundleEntryComponent().setResource(entry.getResource());
                    libraryBundle.addEntry(newItem);
                    if (entry.getResource().hasIdElement() && entry.getResource().getIdElement().equals(theId)) {
                        return (Library) entry.getResource();
                    }
                }
            }
        }
        return null;
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
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Could not resolve library id %s", libraryId));
        }
    }

    @Override
    public Library resolveLibraryByName(String libraryName, String libraryVersion) {
        Iterable<org.hl7.fhir.r4.model.Library> libraries = getLibrariesByName(libraryName);
        org.hl7.fhir.r4.model.Library library = LibraryResolutionProvider.selectFromList(libraries, libraryVersion,
                x -> x.getVersion());

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }

    @Override 
    public Library resolveLibraryByCanonicalUrl(String url) {
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

    private Iterable<org.hl7.fhir.r4.model.Library> getLibrariesByName(String name) {
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

    private Iterable<org.hl7.fhir.r4.model.Library> resolveLibraries(List<IBaseResource> resourceList) {
        List<org.hl7.fhir.r4.model.Library> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class<?> clazz = res.getClass();
            ret.add((org.hl7.fhir.r4.model.Library) clazz.cast(res));
        }
        return ret;
    }

    // TODO: Merge this into the evaluator
    @SuppressWarnings("unused")
    private Map<String, List<Integer>> getLocations(org.hl7.elm.r1.Library library) {
        Map<String, List<Integer>> locations = new HashMap<>();

        if (library.getStatements() == null)
            return locations;

        for (org.hl7.elm.r1.ExpressionDef def : library.getStatements().getDef()) {
            int startLine = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartLine();
            int startChar = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartChar();
            List<Integer> loc = Arrays.asList(startLine, startChar);
            locations.put(def.getName(), loc);
        }

        return locations;
    }

    private String resolveType(Object result) {
        String type = result == null ? "Null" : result.getClass().getSimpleName();
        switch (type) {
            case "BigDecimal":
                return "Decimal";
            case "ArrayList":
                return "List";
            case "FhirBundleCursor":
                return "Retrieve";
        }
        return type;
    }
}