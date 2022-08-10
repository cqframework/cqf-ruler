package org.opencds.cqf.ruler.cpg;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.fhir.retrieve.BaseFhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.Dstu3FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator;
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProvider;
import org.opencds.cqf.ruler.cql.JpaTerminologyProvider;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Operations;

import java.util.*;

public class CqlEvaluationHelper {
	private final FhirContext fhirContext;
	private final ModelResolver modelResolver;
	private final AdapterFactory adapterFactory;
	private final boolean useServerData;
	private final IBaseBundle data;
	private final EndpointInfo dataEndpoint;
	private final EndpointInfo contentEndpoint;
	private final EndpointInfo terminologyEndpoint;
	private final String content;

	private final ClientFactory clientFactory;
	private final LibraryVersionSelector libraryVersionSelector;
	private final SearchParameterResolver searchParameterResolver;
	private final CqlFhirParametersConverter parametersConverter;
	private final Set<CqlEngine.Options> cqlEngineOptions;
	private final List<RetrieveProvider> retrieveProviders;
	private final List<LibraryContentProvider> libraryContentProviders;

	private BaseFhirQueryGenerator queryGenerator;

	private LibraryLoader libraryLoader;
	private TerminologyProvider terminologyProvider;
	private DataProvider dataProvider;

	public CqlEvaluationHelper(FhirContext fhirContext, ModelResolver modelResolver, AdapterFactory adapterFactory,
							   boolean useServerData, IBaseBundle data, EndpointInfo dataEndpoint,
							   EndpointInfo contentEndpoint, EndpointInfo terminologyEndpoint, String content,
							   LibraryLoaderFactory libraryLoaderFactory, JpaLibraryContentProvider jpaContentProvider,
							   LibraryContentProvider restContentProvider, JpaTerminologyProvider jpaTerminologyProvider,
							   DaoRegistry daoRegistry) {
		this.fhirContext = fhirContext;
		this.modelResolver = modelResolver;
		this.adapterFactory = adapterFactory;
		this.useServerData = useServerData;
		this.data = data;
		this.dataEndpoint = dataEndpoint;
		this.contentEndpoint = contentEndpoint;
		this.terminologyEndpoint = terminologyEndpoint;
		this.content = content;

		this.clientFactory = new ClientFactory(fhirContext);
		this.libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
		this.searchParameterResolver = new SearchParameterResolver(fhirContext);
		this.parametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory,
				new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion()));
		this.cqlEngineOptions = Collections.singleton(CqlEngine.Options.EnableExpressionCaching);
		this.retrieveProviders = new ArrayList<>();
		this.libraryContentProviders = new ArrayList<>();

		setup(libraryLoaderFactory, jpaContentProvider, restContentProvider, jpaTerminologyProvider, daoRegistry);
	}

	private void setup(LibraryLoaderFactory libraryLoaderFactory, JpaLibraryContentProvider jpaContentProvider,
					   LibraryContentProvider restContentProvider, JpaTerminologyProvider jpaTerminologyProvider,
					   DaoRegistry daoRegistry) {
		setupLibraryLoader(libraryLoaderFactory, jpaContentProvider, restContentProvider);
		setupTerminologyProvider(jpaTerminologyProvider);
		if (fhirContext.equals(FhirContext.forDstu3())) {
			this.queryGenerator = new Dstu3FhirQueryGenerator(
					searchParameterResolver, terminologyProvider, modelResolver);
		}
		else {
			this.queryGenerator = new R4FhirQueryGenerator(
					searchParameterResolver, terminologyProvider, modelResolver);
		}
		setupDataProvider(daoRegistry);
	}

	private void setupLibraryLoader(LibraryLoaderFactory libraryLoaderFactory,
									JpaLibraryContentProvider jpaContentProvider,
									LibraryContentProvider restContentProvider) {
		if (!StringUtils.isBlank(content)) {
			libraryContentProviders.add(new InMemoryLibraryContentProvider(Collections.singletonList(content)));
		}
		if (contentEndpoint != null) {
			libraryContentProviders.add(restContentProvider);
		}
		else {
			libraryContentProviders.add(jpaContentProvider);
		}
		libraryLoader = libraryLoaderFactory.create(libraryContentProviders);
	}

	private void setupTerminologyProvider(JpaTerminologyProvider jpaTerminologyProvider) {
		if (terminologyEndpoint != null) {
			IGenericClient remoteClient = resolveRemoteClient(terminologyEndpoint);
			terminologyProvider = fhirContext.equals(FhirContext.forDstu3())
					? new Dstu3FhirTerminologyProvider(remoteClient) : new R4FhirTerminologyProvider(remoteClient);
		}
		else {
			terminologyProvider = jpaTerminologyProvider;
		}
	}

	private void setupDataProvider(DaoRegistry daoRegistry) {
		if (data != null) {
			BundleRetrieveProvider bundleRetriever = new BundleRetrieveProvider(fhirContext, data);
			bundleRetriever.setTerminologyProvider(terminologyProvider);
			retrieveProviders.add(bundleRetriever);
		}
		if (useServerData) {
			JpaFhirRetrieveProvider jpaRetriever = new JpaFhirRetrieveProvider(daoRegistry, searchParameterResolver);
			jpaRetriever.setModelResolver(modelResolver);
			jpaRetriever.setFhirQueryGenerator(queryGenerator);
			jpaRetriever.setTerminologyProvider(terminologyProvider);
			if (terminologyEndpoint != null) jpaRetriever.setExpandValueSets(true);
			retrieveProviders.add(jpaRetriever);
		}
		if (dataEndpoint != null) {
			RestFhirRetrieveProvider restRetriever = new RestFhirRetrieveProvider(
					searchParameterResolver, resolveRemoteClient(dataEndpoint));
			restRetriever.setModelResolver(modelResolver);
			restRetriever.setFhirQueryGenerator(queryGenerator);
			restRetriever.setTerminologyProvider(terminologyProvider);
			if (terminologyEndpoint != null) restRetriever.setExpandValueSets(true);
			retrieveProviders.add(restRetriever);
		}
		dataProvider = new CompositeDataProvider(modelResolver, new PriorityRetrieveProvider(retrieveProviders));
	}

	public ExpressionEvaluator getExpressionEvaluator() {
		Set<TypedLibraryContentProviderFactory> libraryFactories = Collections.singleton(
				new FhirRestLibraryContentProviderFactory(clientFactory, adapterFactory, libraryVersionSelector));
		Set<TypedRetrieveProviderFactory> retrieveFactories = Collections.singleton(
				new FhirRestRetrieveProviderFactory(fhirContext, clientFactory));
		Set<TypedTerminologyProviderFactory> terminologyFactories = Collections.singleton(
				new FhirRestTerminologyProviderFactory(fhirContext, clientFactory));
		FhirModelResolverFactory fhirFactory = new FhirModelResolverFactory();
		EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);
		LibraryContentProviderFactory libraryContentFactory =
				new org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory(
						fhirContext, adapterFactory, libraryFactories, libraryVersionSelector);
		DataProviderFactory dataFactory =
				new DataProviderFactory(fhirContext, Collections.singleton(fhirFactory), retrieveFactories);
		TerminologyProviderFactory terminologyFactory =
				new TerminologyProviderFactory(fhirContext, terminologyFactories);
		return new ExpressionEvaluator(fhirContext, parametersConverter, libraryContentFactory, dataFactory,
				terminologyFactory, endpointConverter, fhirFactory, CqlEvaluatorBuilder::new);
	}

	public LibraryEvaluator getLibraryEvaluator() {
		CqlEvaluator cqlEvaluator = new CqlEvaluator(
				libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider),
				terminologyProvider, cqlEngineOptions);
		return new LibraryEvaluator(parametersConverter, cqlEvaluator);
	}

	public IGenericClient resolveRemoteClient(EndpointInfo endpoint) {
		IGenericClient remoteClient = fhirContext.newRestfulGenericClient(endpoint.getAddress());
		if (endpoint.getHeaders() != null) {
			AdditionalRequestHeadersInterceptor headerInterceptor = new AdditionalRequestHeadersInterceptor();
			for (HeaderInfo header : getHeaderNameValuePairs(endpoint.getHeaders())) {
				headerInterceptor.addHeaderValue(header.getName(), header.getValue());
			}
			remoteClient.registerInterceptor(headerInterceptor);
		}
		return remoteClient;
	}

	public Pair<String, Object> resolveContextParameter(String subject) {
		if (StringUtils.isBlank(subject)) return null;
		String[] reference = subject.split("/");
		return Pair.of(reference.length > 1 ? reference[0] : "Patient", reference.length > 1 ? reference[1] : subject);
	}

	public VersionedIdentifier resolveLibraryIdentifier(String content, IBaseResource library) {
		if (!StringUtils.isBlank(content)) {
			ModelManager manager = new ModelManager();
			TranslatedLibrary translatedLibrary =
					CqlTranslator.fromText(content, manager, new LibraryManager(manager)).getTranslatedLibrary();
			return new VersionedIdentifier()
					.withId(translatedLibrary.getIdentifier().getId())
					.withVersion(translatedLibrary.getIdentifier().getVersion());
		}
		else if (library == null) {
			return null;
		}
		else if (library instanceof org.hl7.fhir.dstu3.model.Library) {
			org.hl7.fhir.dstu3.model.Library dstu3Library = (org.hl7.fhir.dstu3.model.Library) library;
			return new VersionedIdentifier()
				.withId(dstu3Library.hasUrl()
						? Canonicals.getIdPart(dstu3Library.getUrl())
						: dstu3Library.hasName() ? dstu3Library.getName() : null)
				.withVersion(dstu3Library.hasVersion()
						? dstu3Library.getVersion() : dstu3Library.hasUrl()
						? Canonicals.getVersion(dstu3Library.getUrl()) : null);
		}
		else {
			org.hl7.fhir.r4.model.Library r4Library = (org.hl7.fhir.r4.model.Library) library;
			return new VersionedIdentifier()
				.withId(r4Library.hasUrl()
						? Canonicals.getIdPart(r4Library.getUrl())
						: r4Library.hasName() ? r4Library.getName() : null)
				.withVersion(r4Library.hasVersion()
						? r4Library.getVersion() : r4Library.hasUrl()
						? Canonicals.getVersion(r4Library.getUrl()) : null);
		}
	}

	public List<Pair<String, String>> resolveIncludedLibraries(List<?> includedLibraries) {
		if (includedLibraries != null) {
			List<Pair<String, String>> libraries = new ArrayList<>();
			String name = null;
			String url = null;
			for (Object parameters : includedLibraries) {
				if (parameters instanceof org.hl7.fhir.dstu3.model.Parameters) {
					for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent parameterComponent
							: ((org.hl7.fhir.dstu3.model.Parameters) parameters).getParameter()) {
						if (parameterComponent.getName().equalsIgnoreCase("url")) {
							url = parameterComponent.getValue().primitiveValue();
						}
						if (parameterComponent.getName().equalsIgnoreCase("name")) {
							name = parameterComponent.getValue().primitiveValue();
						}
					}
				}
				else {
					for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent parameterComponent
							: ((org.hl7.fhir.r4.model.Parameters) parameters).getParameter()) {
						if (parameterComponent.getName().equalsIgnoreCase("url")) {
							url = parameterComponent.getValue().primitiveValue();
						}
						if (parameterComponent.getName().equalsIgnoreCase("name")) {
							name = parameterComponent.getValue().primitiveValue();
						}
					}
				}
				libraries.add(Pair.of(url, name));
			}
			return libraries;
		}
		return null;
	}

	public IBaseOperationOutcome createIssue(String severity, String details) {
		if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
			return new org.hl7.fhir.dstu3.model.OperationOutcome()
				.addIssue(
					new org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent()
						.setSeverity(org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity.fromCode(severity))
						.setDetails(new org.hl7.fhir.dstu3.model.CodeableConcept().setText(details))
				);
		}
		else {
			return new org.hl7.fhir.r4.model.OperationOutcome()
				.addIssue(
					new org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent()
						.setSeverity(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.fromCode(severity))
						.setDetails(new org.hl7.fhir.r4.model.CodeableConcept().setText(details))
				);
		}
	}

	public IBaseOperationOutcome validateOperationParameters(RequestDetails requestDetails, String ... operationParmeters) {
		for (String operationParameter : operationParmeters) {
			try {
				Operations.validateCardinality(requestDetails, operationParameter, 0, 1);
			} catch (Exception e) {
				return createIssue("invalid parameters", e.getMessage());
			}
		}
		return null;
	}

	public List<HeaderInfo> getHeaderNameValuePairs(List<String> headers) {
		List<HeaderInfo> headerNameValuePairs = new ArrayList<>();
		for (String header : headers) {
			// NOTE: assuming the headers will be key value pairs separated by a colon (key: value)
			String[] headerNameAndValue = header.split("\\s*:\\s*");
			if (headerNameAndValue.length == 2) {
				headerNameValuePairs.add(new HeaderInfo(headerNameAndValue[0], headerNameAndValue[1]));
			}
		}
		return headerNameValuePairs;
	}

	static class HeaderInfo {
		private final String name;
		private final String value;

		public HeaderInfo(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
}
