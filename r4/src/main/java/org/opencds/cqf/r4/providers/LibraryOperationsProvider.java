package org.opencds.cqf.r4.providers;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.codesystems.EndpointConnectionType;
import org.opencds.cqf.cds.providers.PriorityRetrieveProvider;
import org.opencds.cqf.common.helpers.ClientHelperDos;
import org.opencds.cqf.common.helpers.DateHelper;
import org.opencds.cqf.common.helpers.LoggingHelper;
import org.opencds.cqf.common.helpers.TranslatorHelper;
//import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.common.providers.LibraryContentProvider;
import org.opencds.cqf.common.providers.R4ApelonFhirTerminologyProvider;
import org.opencds.cqf.common.retrieve.JpaFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.r4.helpers.CanonicalHelper;
import org.opencds.cqf.r4.helpers.ParametersHelper;
import org.opencds.cqf.tooling.library.r4.NarrativeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.opencds.cqf.r4.helpers.FhirMeasureBundler;

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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
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
//    private LibraryContentProvider libraryContentProvider;

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
//        this.libraryContentProvider = libraryContentProvider;
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

//        CqlTranslator translator = CqlTranslator.fromFile(namespaceInfo, translationTestFile, getModelManager(), getLibraryManager(), getUcumService(), options);

        Library library = this.libraryResourceProvider.getDao().read(theId);
        if (library == null) {
            throw new RuntimeException("Could not load library.");
        }

//        for (RelatedArtifact relatedArtifact : library.getRelatedArtifact()) {
//            Library relatedLibrary = this.resolveLibraryByCanonicalUrl(relatedArtifact.getUrl());
//
//        }

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

    // NOTICE: This is trash code that needs to be removed. Don't fix this. It's for
    // a one-off
//    @SuppressWarnings({"unchecked", "rawtypes" })
//    @Operation(name = "$evaluate", idempotent = true, type = Library.class)
//    public Bundle evaluateOLD(
//            @IdParam IdType theId,
//            @OperationParam(name = "patientId") String patientId,
//            @OperationParam(name = "periodStart") String periodStart,
//            @OperationParam(name = "periodEnd") String periodEnd,
//            @OperationParam(name = "productLine") String productLine,
//            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
//            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
//            @OperationParam(name = "context") String contextParam,
//            @OperationParam(name = "executionResults") String executionResults,
//            @OperationParam(name = "parameters") Parameters parameters,
//            @OperationParam(name = "additionalData") Bundle additionalData) {
//
//        if (patientId == null && contextParam != null && contextParam.equals("Patient")) {
//            throw new IllegalArgumentException("Must specify a patientId when executing in Patient context.");
//        }
//
//        Bundle libraryBundle = new Bundle();
//        Library theResource = null;
//        if (additionalData != null) {
//            for (BundleEntryComponent entry : additionalData.getEntry()) {
//                if (entry.getResource().fhirType().equals("Library")) {
//                    libraryBundle.addEntry(entry);
//                    if (entry.getResource().getIdElement().equals(theId)) {
//                        theResource = (Library) entry.getResource();
//                    }
//                }
//            }
//        }
//
//        if (theResource == null) {
//            theResource = this.libraryResourceProvider.getDao().read(theId);
//        }
//
//        VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId(theResource.getName())
//                .withVersion(theResource.getVersion());
//
//        FhirModelResolver resolver = new R4FhirModelResolver();
//        TerminologyProvider terminologyProvider;
//
//        if (terminologyEndpoint != null) {
//            IGenericClient client = ClientHelperDos.getClient(resolver.getFhirContext(), terminologyEndpoint);
//            if (terminologyEndpoint.getAddress().contains("apelon")) {
//                terminologyProvider = new R4ApelonFhirTerminologyProvider(client);
//            } else {
//                terminologyProvider = new R4FhirTerminologyProvider(client);
//            }
//        } else {
//            terminologyProvider = this.defaultTerminologyProvider;
//        }
//
//        DataProvider dataProvider;
//        if (dataEndpoint != null) {
//            IGenericClient client = ClientHelperDos.getClient(resolver.getFhirContext(), dataEndpoint);
//            RestFhirRetrieveProvider retriever = new RestFhirRetrieveProvider(new SearchParameterResolver(resolver.getFhirContext()), client);
//            retriever.setTerminologyProvider(terminologyProvider);
//            if (terminologyEndpoint == null ||(terminologyEndpoint != null && !terminologyEndpoint.getAddress().equals(dataEndpoint.getAddress()))) {
//                retriever.setExpandValueSets(true);
//            }
//
//            if (additionalData != null) {
//                BundleRetrieveProvider bundleProvider = new BundleRetrieveProvider(resolver.getFhirContext(), additionalData);
//                bundleProvider.setTerminologyProvider(terminologyProvider);
//                PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(bundleProvider, retriever);
//                dataProvider = new CompositeDataProvider(resolver, priorityProvider);
//            }
//            else
//            {
//                dataProvider = new CompositeDataProvider(resolver, retriever);
//            }
//
//
//        } else {
//            JpaFhirRetrieveProvider retriever = new JpaFhirRetrieveProvider(this.registry,
//                    new SearchParameterResolver(resolver.getFhirContext()));
//            retriever.setTerminologyProvider(terminologyProvider);
//            // Assume it's a different server, therefore need to expand.
//            if (terminologyEndpoint != null) {
//                retriever.setExpandValueSets(true);
//            }
//
//            if (additionalData != null) {
//                BundleRetrieveProvider bundleProvider = new BundleRetrieveProvider(resolver.getFhirContext(), additionalData);
//                bundleProvider.setTerminologyProvider(terminologyProvider);
//                PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(bundleProvider, retriever);
//                dataProvider = new CompositeDataProvider(resolver, priorityProvider);
//            }
//            else
//            {
//                dataProvider = new CompositeDataProvider(resolver, retriever);
//            }
//        }
//
//
//        ModelManager modelManager = this.libraryHelper.getModelManager();
//        org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider bundleLibraryProvider = new R4BundleLibraryContentProvider(libraryBundle);
//        org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider sourceProvider =  new LibraryContentProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment>(this.getLibraryResourceProvider(),
//        x -> x.getContent(), x -> x.getContentType(), x -> x.getData());
//
//        List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> sourceProviders = Arrays.asList(bundleLibraryProvider, sourceProvider);
//
//        LibraryLoader libraryLoader = new CacheAwareLibraryLoaderDecorator(new TranslatingLibraryLoader(modelManager, sourceProviders, this.libraryHelper.getTranslatorOptions()));
//
//        CqlEngine engine = new CqlEngine(libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider), terminologyProvider);
//
//        Map<String, Object> resolvedParameters = new HashMap<>();
//
//        if (parameters != null) {
//            for (Parameters.ParametersParameterComponent pc : parameters.getParameter()) {
//                resolvedParameters.put(pc.getName(), pc.getValue());
//            }
//        }
//
//        if (periodStart != null && periodEnd != null) {
//            // resolve the measurement period
//            Interval measurementPeriod = new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
//                    DateHelper.resolveRequestDate(periodEnd, false), true);
//
//            resolvedParameters.put("Measurement Period",
//                    new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
//                            DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
//        }
//
//        if (productLine != null) {
//            resolvedParameters.put("Product Line", productLine);
//        }
//
//        EvaluationResult evalResult = engine.evaluate(libraryIdentifier, null,
//                Pair.of(contextParam != null ? contextParam : "Unspecified", patientId == null ? "null" : patientId),
//                resolvedParameters, LoggingHelper.getDebugMap());
//
//        List<Resource> results = new ArrayList<>();
//        FhirMeasureBundler bundler = new FhirMeasureBundler();
//
//        if (evalResult != null && evalResult.expressionResults != null) {
//            for (Map.Entry<String, Object> def : evalResult.expressionResults.entrySet()) {
//
//                Parameters result = new Parameters();
//
//                try {
//                    result.setId(def.getKey());
//                    Object res = def.getValue();
//                    // String location = String.format("[%d:%d]",
//                    // locations.get(def.getName()).get(0),
//                    // locations.get(def.getName()).get(1));
//                    // result.addParameter().setName("location").setValue(new StringType(location));
//
//                    // Object res = def instanceof org.cqframework.cql.elm.execution.FunctionDef
//                    // ? "Definition successfully validated"
//                    // : def.getExpression().evaluate(context);
//
//                    if (res == null) {
//                        result.addParameter().setName("value").setValue(new StringType("null"));
//                    } else if (res instanceof List<?>) {
//                        if (((List<?>) res).size() > 0 && ((List<?>) res).get(0) instanceof Resource) {
//                            if (executionResults != null && executionResults.equals("Summary")) {
//                                result.addParameter().setName("value")
//                                        .setValue(new StringType(((Resource) ((List<?>) res).get(0)).getIdElement()
//                                                .getResourceType() + "/"
//                                                + ((Resource) ((List<?>) res).get(0)).getIdElement().getIdPart()));
//                            } else {
//                                result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
//                            }
//                        } else {
//                            result.addParameter().setName("value").setValue(new StringType(res.toString()));
//                        }
//                    } else if (res instanceof Iterable) {
//                        result.addParameter().setName("value").setResource(bundler.bundle((Iterable<Resource>) res));
//                    } else if (res instanceof Resource) {
//                        if (executionResults != null && executionResults.equals("Summary")) {
//                            result.addParameter().setName("value")
//                                    .setValue(new StringType(((Resource) res).getIdElement().getResourceType() + "/"
//                                            + ((Resource) res).getIdElement().getIdPart()));
//                        } else {
//                            result.addParameter().setName("value").setResource((Resource) res);
//                        }
//                    } else if (res instanceof Type) {
//                        result.addParameter().setName("value").setValue((Type) res);
//                    } else {
//                        result.addParameter().setName("value").setValue(new StringType(res.toString()));
//                    }
//
//                    result.addParameter().setName("resultType").setValue(new StringType(resolveType(res)));
//                } catch (RuntimeException re) {
//                    re.printStackTrace();
//
//                    String message = re.getMessage() != null ? re.getMessage() : re.getClass().getName();
//                    result.addParameter().setName("error").setValue(new StringType(message));
//                }
//                results.add(result);
//            }
//        }
//
//        return bundler.bundle(results);
//    }

    // NOTICE: vNext of this operation
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

        BooleanType useServerData = (BooleanType) parameters.getParameter("useServerData");
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

        Bundle libraryBundle = new Bundle();
        Library theResource = null;
        if (data != null) {
            for (BundleEntryComponent entry : data.getEntry()) {
                if (entry.getResource().fhirType().equals("Library")) {
                    libraryBundle.addEntry(entry);
                    if (entry.getResource().getIdElement().equals(theId)) {
                        theResource = (Library) entry.getResource();
                    }
                }
            }
        }
        Bundle evaluationData = data != null ? data : prefetchDataData;

//        if(theResource != null) {
//            this.update(theResource);
//        }
//
        if (theResource == null) {
            theResource = this.libraryResourceProvider.getDao().read(theId);
//            if (evaluationData != null) {
//                evaluationData.getEntry().add(new BundleEntryComponent().setResource(theResource));
//            }
        }

        VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId(theResource.getName())
                .withVersion(theResource.getVersion());

        //TODO: These are both probably too naive and basically even incorrect
        Endpoint libraryEndpoint = library != null ? new Endpoint().setAddress(library.toString()) : null;

        response = (Parameters)libraryProcessor.evaluate(libraryIdentifier, contextValue, parametersParameters, libraryEndpoint,
                terminologyEndpoint, dataEndpoint, evaluationData, expression);

        return response;
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