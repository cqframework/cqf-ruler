package org.opencds.cqf.ruler.cpg.r4.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
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
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.ruler.cpg.r4.util.FhirMeasureBundler;
import org.opencds.cqf.ruler.cql.CqlProperties;
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProvider;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Clients;
import org.opencds.cqf.ruler.utility.Operations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;

// TODO: Swap cqf-ruler Libary evaluate implementation to the cql-evaluator one.
public class LibraryEvaluationProvider extends DaoRegistryOperationProvider {

	private static final Logger log = LoggerFactory.getLogger(LibraryEvaluationProvider.class);

	@Autowired
	private CqlProperties myCqlProperties;

	@Autowired
	JpaTerminologyProviderFactory myJpaTerminologyProviderFactory;

	@Autowired
	JpaLibraryContentProviderFactory myJpaLibraryContentProviderFactory;

	@Autowired
	LibraryLoaderFactory myLibraryLoaderFactory;

	@Autowired
	ModelResolver myModelResolver;

	@Autowired
	AdapterFactory adapterFactory;

	@Autowired
	LibraryVersionSelector libraryVersionSelector;

	@SuppressWarnings({ "unchecked" })
	@Operation(name = "$evaluate", idempotent = true, type = Library.class)
	public Bundle evaluate(@IdParam IdType theId, @OperationParam(name = "patientId") String patientId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd,
			@OperationParam(name = "productLine") String productLine,
			@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
			@OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
			@OperationParam(name = "context") String contextParam,
			@OperationParam(name = "executionResults") String executionResults,
			@OperationParam(name = "parameters") Parameters parameters,
			@OperationParam(name = "additionalData") Bundle additionalData,
			RequestDetails theRequestDetails) {

		log.info("Library evaluation started..");
		if (patientId == null && contextParam != null && contextParam.equals("Patient")) {
			log.error("Patient id null");
			throw new IllegalArgumentException("Must specify a patientId when executing in Patient context.");
		}

		Bundle libraryBundle = new Bundle();
		Library theResource = null;
		if (additionalData != null) {
			for (Bundle.BundleEntryComponent entry : additionalData.getEntry()) {
				if (entry.getResource().fhirType().equals("Library")) {
					libraryBundle.addEntry(entry);
					if (entry.getResource().getIdElement().equals(theId)) {
						theResource = (Library) entry.getResource();
					}
				}
			}
		}

		if (theResource == null) {
			theResource = read(theId, theRequestDetails);
		}

		VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId(theResource.getName())
				.withVersion(theResource.getVersion());

		TerminologyProvider terminologyProvider;

		if (terminologyEndpoint != null) {
			IGenericClient client = Clients.forEndpoint(getFhirContext(), terminologyEndpoint);
			terminologyProvider = new R4FhirTerminologyProvider(client);
		} else {
			terminologyProvider = myJpaTerminologyProviderFactory.create(new SystemRequestDetails());
		}

		DataProvider dataProvider;
		if (dataEndpoint != null) {
			List<RetrieveProvider> retrieveProviderList = new ArrayList<>();
			IGenericClient client = Clients.forEndpoint(dataEndpoint);
			RestFhirRetrieveProvider retriever = new RestFhirRetrieveProvider(
					new SearchParameterResolver(getFhirContext()),
					client);
			retriever.setTerminologyProvider(terminologyProvider);
			if (terminologyEndpoint == null || (terminologyEndpoint != null
					&& !terminologyEndpoint.getAddress().equals(dataEndpoint.getAddress()))) {
				retriever.setExpandValueSets(true);
			}
			retrieveProviderList.add(retriever);

			if (additionalData != null) {
				BundleRetrieveProvider bundleProvider = new BundleRetrieveProvider(getFhirContext(), additionalData);
				bundleProvider.setTerminologyProvider(terminologyProvider);
				retrieveProviderList.add(bundleProvider);
				PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(retrieveProviderList);
				dataProvider = new CompositeDataProvider(myModelResolver, priorityProvider);
			} else {
				dataProvider = new CompositeDataProvider(myModelResolver, retriever);
			}

		} else {
			List<RetrieveProvider> retrieveProviderList = new ArrayList<>();
			JpaFhirRetrieveProvider retriever = new JpaFhirRetrieveProvider(getDaoRegistry(),
					new SearchParameterResolver(getFhirContext()));
			retriever.setTerminologyProvider(terminologyProvider);
			// Assume it's a different server, therefore need to expand.
			if (terminologyEndpoint != null) {
				retriever.setExpandValueSets(true);
			}
			retrieveProviderList.add(retriever);

			if (additionalData != null) {
				BundleRetrieveProvider bundleProvider = new BundleRetrieveProvider(getFhirContext(), additionalData);
				bundleProvider.setTerminologyProvider(terminologyProvider);
				retrieveProviderList.add(bundleProvider);
				PriorityRetrieveProvider priorityProvider = new PriorityRetrieveProvider(retrieveProviderList);
				dataProvider = new CompositeDataProvider(myModelResolver, priorityProvider);
			} else {
				dataProvider = new CompositeDataProvider(myModelResolver, retriever);
			}
		}

		LibraryContentProvider bundleLibraryProvider = new BundleFhirLibraryContentProvider(this.getFhirContext(),
				libraryBundle, adapterFactory, libraryVersionSelector);
		LibraryContentProvider jpaLibraryContentProvider = this.myJpaLibraryContentProviderFactory
				.create(theRequestDetails);

		List<LibraryContentProvider> sourceProviders = new ArrayList<LibraryContentProvider>(
				Arrays.asList(bundleLibraryProvider, jpaLibraryContentProvider));
		LibraryLoader libraryLoader = this.myLibraryLoaderFactory.create(sourceProviders);

		CqlEngine engine = new CqlEngine(libraryLoader, Collections.singletonMap("http://hl7.org/fhir", dataProvider),
				terminologyProvider);

		Map<String, Object> resolvedParameters = new HashMap<>();

		if (parameters != null) {
			for (Parameters.ParametersParameterComponent pc : parameters.getParameter()) {
				resolvedParameters.put(pc.getName(), pc.getValue());
			}
		}

		if (periodStart != null && periodEnd != null) {
			// resolve the measurement period
			Interval measurementPeriod = new Interval(Operations.resolveRequestDate(periodStart, true), true,
					Operations.resolveRequestDate(periodEnd, false), true);

			resolvedParameters.put("Measurement Period",
					new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
							DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
		}

		if (productLine != null) {
			resolvedParameters.put("Product Line", productLine);
		}

		EvaluationResult evalResult = engine.evaluate(libraryIdentifier, null,
				Pair.of(contextParam != null ? contextParam : "Unspecified", patientId == null ? "null" : patientId),
				resolvedParameters, this.getDebugMap());

		List<Resource> results = new ArrayList<>();
		FhirMeasureBundler bundler = new FhirMeasureBundler();

		if (evalResult != null && evalResult.expressionResults != null) {
			for (Map.Entry<String, Object> def : evalResult.expressionResults.entrySet()) {

				Parameters result = new Parameters();

				try {
					result.setId(def.getKey());
					Object res = def.getValue();
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
						if (((List<?>) res).size() > 0 && ((List<?>) res).get(0) instanceof Resource) {
							if (executionResults != null && executionResults.equals("Summary")) {
								result.addParameter().setName("value")
										.setValue(new StringType(((Resource) ((List<?>) res).get(0)).getIdElement()
												.getResourceType() + "/"
												+ ((Resource) ((List<?>) res).get(0)).getIdElement().getIdPart()));
							} else {
								result.addParameter().setName("value").setResource(
										bundler.bundle((Iterable<Resource>) res, theRequestDetails.getFhirServerBase()));
							}
						} else {
							result.addParameter().setName("value").setValue(new StringType(res.toString()));
						}
					} else if (res instanceof Iterable) {
						result.addParameter().setName("value")
								.setResource(bundler.bundle((Iterable<Resource>) res, theRequestDetails.getFhirServerBase()));
					} else if (res instanceof Resource) {
						if (executionResults != null && executionResults.equals("Summary")) {
							result.addParameter().setName("value")
									.setValue(new StringType(((Resource) res).getIdElement().getResourceType() + "/"
											+ ((Resource) res).getIdElement().getIdPart()));
						} else {
							result.addParameter().setName("value").setResource((Resource) res);
						}
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
				results.add(result);
			}
		}

		return bundler.bundle(results, theRequestDetails.getFhirServerBase());
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

	public DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (myCqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}
}
