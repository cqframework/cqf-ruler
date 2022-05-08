package org.opencds.cqf.ruler.cpg.r4.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.ruler.cpg.r4.util.FhirMeasureBundler;
import org.opencds.cqf.ruler.cql.CqlProperties;
import org.opencds.cqf.ruler.cql.JpaFhirDal;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Clients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(CqlExecutionProvider.class);

	private FhirMeasureBundler bundler = new FhirMeasureBundler();
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
	private CqlProperties myCqlProperties;
	@Autowired
	Map<VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache;

	/**
	 * A library to be included. The library is resolved by url and made available
	 * by name within the expression to be evaluated.
	 */
	class LibraryParameter {
		/**
		 * The {@link CanonicalType} canonical url (with optional version) of the
		 * library to be included
		 */
		CanonicalType url;
		/**
		 * The name of the library to be used to reference the library within the CQL
		 * expression. If no name is provided, the name of the library will be used
		 */
		String name;

		public LibraryParameter withUrl(CanonicalType url) {
			this.url = url;
			return this;
		}

		public LibraryParameter withName(String name) {
			this.name = name;
			return this;
		}
	}

	/**
	 * Data to be made available to the library evaluation, organized as prefetch
	 * response bundles. Each prefetchData parameter specifies either the name of
	 * the prefetchKey it is satisfying, a DataRequirement describing the prefetch,
	 * or both.
	 */
	class PrefetchData {
		/**
		 * The key of the prefetch item. This typically corresponds to the name of a
		 * parameter in a library, or the name of a prefetch item in a CDS Hooks
		 * discovery response
		 */
		String key;
		/**
		 * A {@link DataRequirement} DataRequirement describing the content of the
		 * prefetch item.
		 */
		DataRequirement descriptor;
		/**
		 * The prefetch data as a {@link Bundle} Bundle. If the prefetchData has no
		 * prefetchResult part, it indicates there is no data associated with this
		 * prefetch item.
		 */
		Bundle data;

		public PrefetchData withKey(String key) {
			this.key = key;
			return this;
		}

		public PrefetchData withDescriptor(DataRequirement descriptor) {
			this.descriptor = descriptor;
			return this;
		}

		public PrefetchData withData(Bundle data) {
			this.data = data;
			return this;
		}
	}

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
	 *                            definition statements.
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
	 * @param library             A library to be included. The {@link Library}
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
	 * @return The result of evaluating the given expression, returned as a FHIR
	 *         type, either a {@link Resource} resource, or a FHIR-defined type
	 *         corresponding to the CQL return type, as defined in the Using CQL
	 *         section of the CPG Implementation guide. If the result is a List of
	 *         resources, the result will be a {@link Bundle} Bundle . If the result
	 *         is a CQL system-defined or FHIR-defined type, the result is returned
	 *         as a {@link Parameters} Parameters resource
	 */
	@Operation(name = "$cql")
	@Description(shortDefinition = "$cql", value = "Evaluates a CQL expression and returns the results as a Parameters resource. Defined: http://build.fhir.org/ig/HL7/cqf-recommendations/OperationDefinition-cpg-cql.html", example = "$cql?expression=5*5")
	public Parameters evaluate(RequestDetails theRequestDetails,
			@OperationParam(name = "subject", max = 1) String subject,
			@OperationParam(name = "expression", min = 1, max = 1) String expression,
			@OperationParam(name = "parameters", max = 1) Parameters parameters,
			@OperationParam(name = "library") List<Parameters> library,
			@OperationParam(name = "useServerData", max = 1) BooleanType useServerData,
			@OperationParam(name = "data", max = 1) Bundle data,
			@OperationParam(name = "prefetchData") List<Parameters> prefetchData,
			@OperationParam(name = "dataEndpoint", max = 1) Endpoint dataEndpoint,
			@OperationParam(name = "contentEndpoint", max = 1) Endpoint contentEndpoint,
			@OperationParam(name = "terminologyEndpoint", max = 1) Endpoint terminologyEndpoint) {

		if (prefetchData != null) {
			throw new NotImplementedException("prefetchData is not yet supported.");
		}
		if (useServerData == null) {
			useServerData = new BooleanType(true);
		}
		List<LibraryParameter> libraryParameters = new ArrayList<>();
		if (library != null) {
			for (Parameters libraryParameter : library) {
				CanonicalType url = null;
				String name = null;
				for (ParametersParameterComponent param : libraryParameter.getParameter()) {
					switch (param.getName()) {
						case "url":
							url = ((CanonicalType) param.getValue());
							break;
						case "name":
							name = ((StringType) param.getValue()).asStringValue();
							break;
						default:
							throw new IllegalArgumentException("Only url and name parts are allowed for Parameter: library");
					}
				}
				if (url == null) {
					throw new IllegalArgumentException("If library parameter must provide a url parameter part.");
				}
				libraryParameters.add(new LibraryParameter().withUrl(url).withName(name));
			} // Remove LocalLibrary from cache first...
		}
		VersionedIdentifier localLibraryIdentifier = new VersionedIdentifier().withId("LocalLibrary")
				.withVersion("1.0.0");
		globalLibraryCache.remove(localLibraryIdentifier);

		CqlEngine engine = setupEngine(localLibraryIdentifier, expression, libraryParameters, subject, parameters,
				contentEndpoint,
				dataEndpoint, terminologyEndpoint, data, useServerData.booleanValue(), theRequestDetails);
		Map<String, Object> resolvedParameters = new HashMap<>();
		if (parameters != null) {
			for (Parameters.ParametersParameterComponent pc : parameters.getParameter()) {
				resolvedParameters.put(pc.getName(), pc.getValue());
			}
		}

		String contextType = subject != null ? subject.substring(0, subject.lastIndexOf("/") - 1) : null;
		String subjectId = subject != null ? subject.substring(0, subject.lastIndexOf("/") - 1) : null;
		EvaluationResult evalResult = engine.evaluate(localLibraryIdentifier, null,
				Pair.of(contextType != null ? contextType : "Unspecified", subjectId == null ? "null" : subject),
				resolvedParameters, this.getDebugMap());

		if (evalResult != null && evalResult.expressionResults != null) {
			if (evalResult.expressionResults.size() > 1) {
				logger.debug("Evaluation resulted in more than one expression result.  ");
			}

			Parameters result = new Parameters();
			resolveResult(theRequestDetails, evalResult, result);
			return result;
		}
		return null;
	}

	private CqlEngine setupEngine(VersionedIdentifier localLibraryIdentifier, String expression,
			List<LibraryParameter> library, String subject,
			Parameters parameters, Endpoint contentEndpoint, Endpoint dataEndpoint, Endpoint terminologyEndpoint,
			Bundle data, boolean useServerData,
			RequestDetails theRequestDetails) {
		JpaFhirDal jpaFhirDal = jpaFhirDalFactory.create(theRequestDetails);

		// temporary LibraryLoader to resolve library dependencies when building
		// includes
		List<LibraryContentProvider> libraryProviders = new ArrayList<>();
		libraryProviders.add(jpaLibraryContentProviderFactory.create(theRequestDetails));
		if (contentEndpoint != null) {
			libraryProviders.add(fhirRestLibraryContentProviderFactory.create(contentEndpoint.getAddress(), contentEndpoint
					.getHeader().stream().map(PrimitiveType::asStringValue).collect(Collectors.toList())));
		}
		LibraryLoader tempLibraryLoader = libraryLoaderFactory.create(
				new ArrayList<>(libraryProviders));

		String cql = buildCqlLibrary(library, jpaFhirDal, tempLibraryLoader, expression, parameters, theRequestDetails);

		libraryProviders.add(new InMemoryLibraryContentProvider(Arrays.asList(cql)));
		LibraryLoader libraryLoader = libraryLoaderFactory.create(
				new ArrayList<>(libraryProviders));

		return setupEngine(subject, parameters, dataEndpoint, terminologyEndpoint, data, useServerData, libraryLoader,
				localLibraryIdentifier, theRequestDetails);
	}

	private CqlEngine setupEngine(String subject, Parameters parameters, Endpoint dataEndpoint,
			Endpoint terminologyEndpoint, Bundle data, boolean useServerData, LibraryLoader libraryLoader,
			VersionedIdentifier libraryIdentifier, RequestDetails theRequestDetails) {
		TerminologyProvider terminologyProvider;

		if (terminologyEndpoint != null) {
			IGenericClient client = Clients.forEndpoint(getFhirContext(), terminologyEndpoint);
			terminologyProvider = new R4FhirTerminologyProvider(client);
		} else {
			terminologyProvider = jpaTerminologyProviderFactory.create(theRequestDetails);
		}

		DataProvider dataProvider;
		List<RetrieveProvider> retrieveProviderList = new ArrayList<>();
		if (useServerData) {
			JpaFhirRetrieveProvider jpaRetriever = new JpaFhirRetrieveProvider(getDaoRegistry(),
					new SearchParameterResolver(getFhirContext()));
			jpaRetriever.setTerminologyProvider(terminologyProvider);
			// Assume it's a different server, therefore need to expand.
			if (terminologyEndpoint != null) {
				jpaRetriever.setExpandValueSets(true);
			}
			retrieveProviderList.add(jpaRetriever);
		}
		if (dataEndpoint != null) {
			IGenericClient client = Clients.forEndpoint(dataEndpoint);
			RestFhirRetrieveProvider restRetriever = new RestFhirRetrieveProvider(
					new SearchParameterResolver(getFhirContext()),
					client);
			restRetriever.setTerminologyProvider(terminologyProvider);
			if (terminologyEndpoint == null || (terminologyEndpoint != null
					&& !terminologyEndpoint.getAddress().equals(dataEndpoint.getAddress()))) {
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
		dataProvider = new CompositeDataProvider(myModelResolver, priorityProvider);
		return new CqlEngine(libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider),
				terminologyProvider);
	}

	private String buildCqlLibrary(List<LibraryParameter> library, JpaFhirDal jpaFhirDal, LibraryLoader libraryLoader,
			String expression,
			Parameters parameters,
			RequestDetails theRequestDetails) {
		String cql = null;
		logger.debug("Constructing expression for local evaluation");

		StringBuilder sb = new StringBuilder();

		constructHeader(sb);
		constructUsings(sb);
		constructIncludes(sb, jpaFhirDal, library, libraryLoader, theRequestDetails);
		constructParameters(sb, parameters);
		constructExpression(sb, expression);

		cql = sb.toString();

		logger.debug(cql);
		return cql;
	}

	private void constructHeader(StringBuilder sb) {
		sb.append("library LocalLibrary version \'1.0.0\'\n\n");
	}

	private void constructUsings(StringBuilder sb) {
		sb.append(String.format("using FHIR version \'%s\'\n\n",
				getFhirVersion()));
	}

	private String getFhirVersion() {
		return this.getFhirContext().getVersion().getVersion().getFhirVersionString();
	}

	private void constructParameters(StringBuilder sb, Parameters parameters) {
		if (parameters == null) {
			return;
		}
		for (ParametersParameterComponent param : parameters.getParameter()) {
			sb.append("parameter \"" + param.getName() + "\" " + param.getValue().fhirType() + "\n");
		}
	}

	private void constructIncludes(StringBuilder sb, JpaFhirDal jpaFhirDal, List<LibraryParameter> library,
			LibraryLoader libraryLoader,
			RequestDetails requestDetails) {
		StringBuilder builder = new StringBuilder();
		builder.append("include FHIRHelpers version " + "\'" + getFhirVersion() + "\'" + "\n");
		for (LibraryParameter libraryParameter : library) {

			builder.append("include ");

			String libraryName = resolveLibraryName(requestDetails, jpaFhirDal, libraryParameter, libraryLoader);

			builder.append(libraryName);

			if (Canonicals.getVersion(libraryParameter.url) != null) {
				builder.append(" version '");
				builder.append(Canonicals.getVersion(libraryParameter.url));
				builder.append("'");
			}

			builder.append(" called ");
			builder.append(libraryName + "\n");
		}
		sb.append(builder.toString());
	}

	private void constructExpression(StringBuilder sb, String expression) {
		sb.append(String.format("\ndefine \"return\":\n       %s", expression));
	}

	private String resolveLibraryName(RequestDetails requestDetails, JpaFhirDal jpaFhirDal,
			LibraryParameter libraryParameter,
			LibraryLoader libraryLoader) {
		String libraryName;
		if (libraryParameter.name == null) {
			VersionedIdentifier libraryIdentifier = new VersionedIdentifier()
					.withId(Canonicals.getIdPart(libraryParameter.url));
			String version = Canonicals.getVersion(libraryParameter.url);
			if (version != null) {
				libraryIdentifier.setVersion(version);
			}
			org.cqframework.cql.elm.execution.Library executionLibrary = null;
			try {
				executionLibrary = libraryLoader.load(libraryIdentifier);
			} catch (Exception e) {
				logger.debug("Unable to load executable library {}", libraryParameter.name);
			}
			if (executionLibrary != null) {
				libraryName = executionLibrary.getIdentifier().getId();
			} else {
				Library library = (Library) jpaFhirDal.read(new IdType("Library", libraryIdentifier.getId()));
				libraryName = library.getName();
			}
		} else {
			libraryName = libraryParameter.name;
		}
		return libraryName;
	}

	@SuppressWarnings("unchecked")
	private void resolveResult(RequestDetails theRequestDetails, EvaluationResult evalResult, Parameters result) {
		try {
			Object res = evalResult.forExpression("return");
			// String location = String.format("[%d:%d]",
			// locations.get(def.getName()).get(0),
			// locations.get(def.getName()).get(1));
			// result.addParameter().setName("location").setValue(new StringType(location));

			// Object res = def instanceof org.cqframework.cql.elm.execution.FunctionDef
			// ? "Definition successfully validated"
			// : def.getExpression().evaluate(context);

			if (res == null) {
				result.addParameter().setName("value").setValue(new StringType("null"));
			} else if (res instanceof List<?>) {
				if (!((List<?>) res).isEmpty() && ((List<?>) res).get(0) instanceof Resource) {
					result.addParameter().setName("value")
							.setResource(bundler.bundle((Iterable<Resource>) res, theRequestDetails.getFhirServerBase()));
				} else {
					result.addParameter().setName("value").setValue(new StringType(res.toString()));
				}
			} else if (res instanceof Iterable) {
				result.addParameter().setName("value")
						.setResource(bundler.bundle((Iterable<Resource>) res, theRequestDetails.getFhirServerBase()));
			} else if (res instanceof Resource) {
				result.addParameter().setName("value").setResource((Resource) res);
			} else if (res instanceof Type) {
				result.addParameter().setName("value").setValue((Type) res);
			} else {
				result.addParameter().setName("value").setValue(new StringType(res.toString()));
			}
			result.addParameter().setName("resultType").setValue(new StringType(resolveType(res)));
		} catch (RuntimeException re) {
			re.printStackTrace();

			String message = re.getMessage() != null ? re.getMessage() : re.getClass().getName();
			result.addParameter().setName("error").setValue(new StringType(message));
		}
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
			default:
				return type;
		}
	}

	private DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (myCqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}
}
