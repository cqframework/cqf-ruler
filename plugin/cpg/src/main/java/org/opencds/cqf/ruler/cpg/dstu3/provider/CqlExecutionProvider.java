package org.opencds.cqf.ruler.cpg.dstu3.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Clients;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * This class is used to provide an {@link DaoRegistryOperationProvider
 * OperationProvider}
 * implementation that supports cql expression evaluation
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider extends DaoRegistryOperationProvider {

//	@Autowired
//	private CqlProperties myCqlProperties;
	@Autowired
	private LibraryLoaderFactory libraryLoaderFactory;
	@Autowired
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
	@Autowired
	private FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory;
	@Autowired
	private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;
	@Autowired
	private JpaFhirDalFactory jpaFhirDalFactory;
	@Autowired
	ModelResolver myModelResolver;
	@Autowired
	Map<VersionedIdentifier, Library> globalLibraryCache;

	private String subject;
	private String expression;
	private Parameters parameters;
	private List<Parameters> includedLibraries;
	private boolean useServerData;
	private Bundle data;
	private Endpoint dataEndpoint;
	private Endpoint libraryContentEndpoint;
	private Endpoint terminologyEndpoint;
	private String content;
	private AdapterFactory adapterFactory = new AdapterFactory();

//	/**
//	 * Data to be made available to the library evaluation, organized as prefetch
//	 * response bundles. Each prefetchData parameter specifies either the name of
//	 * the prefetchKey it is satisfying, a DataRequirement describing the prefetch,
//	 * or both.
//	 */
//	class PrefetchData {
//		/**
//		 * The key of the prefetch item. This typically corresponds to the name of a
//		 * parameter in a library, or the name of a prefetch item in a CDS Hooks
//		 * discovery response
//		 */
//		String key;
//		/**
//		 * A {@link DataRequirement} DataRequirement describing the content of the
//		 * prefetch item.
//		 */
//		DataRequirement descriptor;
//		/**
//		 * The prefetch data as a {@link Bundle} Bundle. If the prefetchData has no
//		 * prefetchResult part, it indicates there is no data associated with this
//		 * prefetch item.
//		 */
//		Bundle data;
//
//		public PrefetchData withKey(String key) {
//			this.key = key;
//			return this;
//		}
//
//		public PrefetchData withDescriptor(DataRequirement descriptor) {
//			this.descriptor = descriptor;
//			return this;
//		}
//
//		public PrefetchData withData(Bundle data) {
//			this.data = data;
//			return this;
//		}
//	}

	/**
	 * Evaluates a CQL expression and returns the results as a Parameters resource.
	 *
	 * @param theRequestDetails   the {@link RequestDetails RequestDetails}
	 * @param subject             Subject for which the expression will be
	 *                            evaluated. This corresponds to the context in
	 *                            which the expression will be evaluated and is
	 *                            represented as a relative FHIR id (e.g.
	 *                            Patient/123), which establishes both the context
	 *                            and context value for the evaluation
	 * @param expression          Expression to be evaluated. Note that this is an
	 *                            expression of CQL, not the text of a library with
	 *                            definition statements. If the content parameter is
	 *                            set, the expression will be the name of the
	 *                            expression to be evaluated.
	 * @param parameters          Any input parameters for the expression.
	 *                            {@link Parameters} Parameters defined in this
	 *                            input will be made available by name to the CQL
	 *                            expression. Parameter types are mapped to CQL as
	 *                            specified in the Using CQL section of the CPG
	 *                            Implementation guide. If a parameter appears more
	 *                            than once in the input Parameters resource, it is
	 *                            represented with a List in the input CQL. If a
	 *                            parameter has parts, it is represented as a Tuple
	 *                            in the input CQL.
	 * @param library             A library to be included. The {@link org.hl7.fhir.dstu3.model.Library}
	 *                            library is resolved by url and made available by
	 *                            name within the expression to be evaluated.
	 * @param useServerData       Whether to use data from the server performing the
	 *                            evaluation. If this parameter is true (the
	 *                            default), then the operation will use data first
	 *                            from any bundles provided as parameters (through
	 *                            the data and prefetch parameters), second data
	 *                            from the server performing the operation, and
	 *                            third, data from the dataEndpoint parameter (if
	 *                            provided). If this parameter is false, the
	 *                            operation will use data first from the bundles
	 *                            provided in the data or prefetch parameters, and
	 *                            second from the dataEndpoint parameter (if
	 *                            provided).
	 * @param data                Data to be made available to the library
	 *                            evaluation. This parameter is exclusive with the
	 *                            prefetchData parameter (i.e. either provide all
	 *                            data as a single bundle, or provide data using
	 *                            multiple bundles with prefetch descriptions).
	 * @param prefetchData        ***Not Yet Implemented***
	 * @param dataEndpoint        An {@link Endpoint} endpoint to use to access data
	 *                            referenced by retrieve operations in the library.
	 *                            If provided, this endpoint is used after the data
	 *                            or prefetchData bundles, and the server, if the
	 *                            useServerData parameter is true.
	 * @param contentEndpoint     An {@link Endpoint} endpoint to use to access
	 *                            content (i.e. libraries) referenced by the
	 *                            library. If no content endpoint is supplied, the
	 *                            evaluation will attempt to retrieve content from
	 *                            the server on which the operation is being
	 *                            performed.
	 * @param terminologyEndpoint An {@link Endpoint} endpoint to use to access
	 *                            terminology (i.e. valuesets, codesystems, and
	 *                            membership testing) referenced by the library. If
	 *                            no terminology endpoint is supplied, the
	 *                            evaluation will attempt to use the server on which
	 *                            the operation is being performed as the
	 *                            terminology server.
	 * @param content             The CQL library content. If this and the
	 *                            expression
	 *                            parameter are set, only the expression specified
	 *                            will be evaluated.
	 * @return The result of evaluating the given expression, returned as a FHIR
	 *         type, either a {@link org.hl7.fhir.dstu3.model.Resource} resource, or a FHIR-defined type
	 *         corresponding to the CQL return type, as defined in the Using CQL
	 *         section of the CPG Implementation guide. If the result is a List of
	 *         resources, the result will be a {@link Bundle} Bundle . If the result
	 *         is a CQL system-defined or FHIR-defined type, the result is returned
	 *         as a {@link Parameters} Parameters resource
	 */
	@Operation(name = "$cql")
	@Description(shortDefinition = "$cql", value = "Evaluates a CQL expression and returns the results as a Parameters resource. Defined: http://build.fhir.org/ig/HL7/cqf-recommendations/OperationDefinition-cpg-cql.html", example = "$cql?expression=5*5")
	public Parameters evaluate(
		RequestDetails theRequestDetails,
		@OperationParam(name = "subject", max = 1) String subject,
		@OperationParam(name = "expression", max = 1) String expression,
		@OperationParam(name = "parameters", max = 1) Parameters parameters,
		@OperationParam(name = "library") List<Parameters> library,
		@OperationParam(name = "useServerData", max = 1) BooleanType useServerData,
		@OperationParam(name = "data", max = 1) Bundle data,
		@OperationParam(name = "prefetchData") List<Parameters> prefetchData,
		@OperationParam(name = "dataEndpoint", max = 1) Endpoint dataEndpoint,
		@OperationParam(name = "contentEndpoint", max = 1) Endpoint contentEndpoint,
		@OperationParam(name = "terminologyEndpoint", max = 1) Endpoint terminologyEndpoint,
		@OperationParam(name = "content", max = 1) String content) {
		if (prefetchData != null) {
			throw new NotImplementedException("prefetchData is not yet supported.");
		}

		if (useServerData == null) {
			useServerData = new BooleanType(true);
		}

		if (expression == null && content == null) {
			throw new IllegalArgumentException("The $cql operation requires the expression parameter and/or content parameter to exist");
		}

		// TODO: Evaluator requires headers... remove once addressed
		Endpoint defaultEndpoint = new Endpoint().setAddress(theRequestDetails.getFhirServerBase()).setHeader(Collections.singletonList(new StringType("Content-Type: application/json")));

		this.subject = subject;
		this.expression = expression;
		this.parameters = parameters;
		this.includedLibraries = library;
		this.useServerData = useServerData.booleanValue();
		this.data = data;
		this.dataEndpoint = dataEndpoint == null ? defaultEndpoint : dataEndpoint;
		this.libraryContentEndpoint = contentEndpoint == null ? defaultEndpoint : contentEndpoint;
		this.terminologyEndpoint = terminologyEndpoint == null ? defaultEndpoint : terminologyEndpoint;
		this.content = content;

		/*
		 *
		 * If expression and no content:
		 * Evaluate the specified expression (raw CQL)
		 *
		 * If content and no expression:
		 * Evaluate all expressions in the content
		 *
		 * If content and expression:
		 * Evaluate only the expression named by the expression parameter
		 *
		 */

		return StringUtils.isBlank(this.content) ? evaluateExpression() : evaluateLibrary(theRequestDetails);
	}

	private Parameters evaluateLibrary(RequestDetails requestDetails) {
		LibraryLoader libraryLoader = resolveLibraryLoader(requestDetails);
		TerminologyProvider terminologyProvider = resolveTerminologyProvider(requestDetails);
		DataProvider dataProvider = resolveDataProvider(terminologyProvider);
		CqlEvaluator cqlEvaluator = new CqlEvaluator(libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider), terminologyProvider, Collections.singleton(CqlEngine.Options.EnableExpressionCaching));
		LibraryEvaluator libraryEvaluator = new LibraryEvaluator(new CqlFhirParametersConverter(this.getFhirContext(), this.adapterFactory, new FhirTypeConverterFactory().create(FhirVersionEnum.DSTU3)), cqlEvaluator);
		return (Parameters) libraryEvaluator.evaluate(resolveLibraryIdentifier(), resolveContextParameter(), this.parameters, this.expression == null ? null : Collections.singleton(this.expression));
	}

	private Parameters evaluateExpression() {
		return (Parameters) resolveExpressionEvaluator().evaluate(
			this.expression, this.parameters, this.subject, resolveIncludedLibraries(), this.useServerData,
			this.data, null, this.dataEndpoint, this.libraryContentEndpoint, this.terminologyEndpoint
		);
	}


	private LibraryLoader resolveLibraryLoader(RequestDetails requestDetails) {
		List<LibraryContentProvider> libraryProviders = new ArrayList<>();
		libraryProviders.add(jpaLibraryContentProviderFactory.create(requestDetails));

		if (this.libraryContentEndpoint != null) {
			libraryProviders.add(
				fhirRestLibraryContentProviderFactory.create(
					this.libraryContentEndpoint.getAddress(),
					this.libraryContentEndpoint.getHeader().stream().map(PrimitiveType::asStringValue).collect(Collectors.toList())
				)
			);
		}

		if (!StringUtils.isBlank(this.content)) {
			libraryProviders.add(
				new InMemoryLibraryContentProvider(Collections.singletonList(this.content))
			);
		}

		return libraryLoaderFactory.create(new ArrayList<>(libraryProviders));
	}

	private TerminologyProvider resolveTerminologyProvider(RequestDetails requestDetails) {
		return terminologyEndpoint != null
			? new Dstu3FhirTerminologyProvider(Clients.forEndpoint(getFhirContext(), this.terminologyEndpoint))
			: jpaTerminologyProviderFactory.create(requestDetails);
	}

	private DataProvider resolveDataProvider(TerminologyProvider terminologyProvider) {
		List<RetrieveProvider> retrieveProviderList = new ArrayList<>();
		if (useServerData) {
			JpaFhirRetrieveProvider jpaRetriever = new JpaFhirRetrieveProvider(getDaoRegistry(),
				new SearchParameterResolver(getFhirContext()));
			jpaRetriever.setTerminologyProvider(terminologyProvider);
			if (terminologyEndpoint != null) {
				jpaRetriever.setExpandValueSets(true);
			}
			retrieveProviderList.add(jpaRetriever);
		}

		if (dataEndpoint != null) {
			IGenericClient client = Clients.forEndpoint(dataEndpoint);
			RestFhirRetrieveProvider restRetriever = new RestFhirRetrieveProvider(
				new SearchParameterResolver(getFhirContext()), client);
			restRetriever.setTerminologyProvider(terminologyProvider);

			if (terminologyEndpoint == null || !terminologyEndpoint.getAddress().equals(dataEndpoint.getAddress())) {
				restRetriever.setExpandValueSets(true);
			}

			retrieveProviderList.add(restRetriever);
		}

		if (data != null) {
			BundleRetrieveProvider bundleRetriever = new BundleRetrieveProvider(getFhirContext(), data);
			bundleRetriever.setTerminologyProvider(terminologyProvider);
			retrieveProviderList.add(bundleRetriever);
		}

		PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(retrieveProviderList);
		return new CompositeDataProvider(myModelResolver, priorityProvider);
	}

	private VersionedIdentifier resolveLibraryIdentifier() {
		ModelManager manager = new ModelManager();
		TranslatedLibrary library = CqlTranslator.fromText(this.content, manager, new LibraryManager(manager))
			.getTranslatedLibrary();
		return new VersionedIdentifier().withId(library.getIdentifier().getId()).withVersion(library.getIdentifier().getVersion());
	}

	private Pair<String, Object> resolveContextParameter() {
		if (StringUtils.isBlank(this.subject)) return null;
		// little hacky using an R4 reference here
		org.hl7.fhir.r4.model.Reference subjectReference = new org.hl7.fhir.r4.model.Reference(subject);
		// TODO: this needs work for non-patient references
		return Pair.of(subjectReference.getType(), subjectReference.getReference());
	}

	private ExpressionEvaluator resolveExpressionEvaluator() {
		return new ExpressionEvaluator(
			this.getFhirContext(),
			new CqlFhirParametersConverter(
				this.getFhirContext(), this.adapterFactory, new FhirTypeConverterFactory().create(FhirVersionEnum.DSTU3)
			),
			resolveLibraryContentProviderFactory(), resolveDataProviderFactory(), resolveTerminologyProviderFactory(),
			new EndpointConverter(this.adapterFactory), new FhirModelResolverFactory(), CqlEvaluatorBuilder::new);
	}

	private LibraryContentProviderFactory resolveLibraryContentProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory(
			this.getFhirContext(), this.adapterFactory,
			Collections.singleton(resolveFhirRestLibraryContentProviderFactory()),
			new LibraryVersionSelector(this.adapterFactory)
		);
	}

	private FhirRestLibraryContentProviderFactory resolveFhirRestLibraryContentProviderFactory() {
		return new FhirRestLibraryContentProviderFactory(
			new ClientFactory(this.getFhirContext()), this.adapterFactory, new LibraryVersionSelector(this.adapterFactory)
		);
	}

	private DataProviderFactory resolveDataProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(
			this.getFhirContext(), Collections.singleton(new FhirModelResolverFactory()),
			Collections.singleton(new FhirRestRetrieveProviderFactory(this.getFhirContext(), new ClientFactory(this.getFhirContext())))
		);
	}

	private TerminologyProviderFactory resolveTerminologyProviderFactory() {
		return new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
			this.getFhirContext(),
			Collections.singleton(new FhirRestTerminologyProviderFactory(this.getFhirContext(), new ClientFactory(this.getFhirContext())))
		);
	}

	private List<Pair<String, String>> resolveIncludedLibraries() {
		if (this.includedLibraries != null) {
			List<Pair<String, String>> libraries = new ArrayList<>();
			String name = null;
			String url = null;
			for (Parameters parameters : this.includedLibraries) {
				for (ParametersParameterComponent parameterComponent : parameters.getParameter()) {
					if (parameterComponent.getName().equalsIgnoreCase("url")) url = parameterComponent.getValue().primitiveValue();
					if (parameterComponent.getName().equalsIgnoreCase("name")) name = parameterComponent.getValue().primitiveValue();
				}
				libraries.add(Pair.of(url, name));
			}
			return libraries;
		}
		return null;
	}

//	private DebugMap getDebugMap() {
//		DebugMap debugMap = new DebugMap();
//		if (myCqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
//			debugMap.setIsLoggingEnabled(true);
//		}
//		return debugMap;
//	}
}
