package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.cr.common.HapiTerminologyProvider;
import ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.BundleUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CDSHooksTransactionInterceptor;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.r4.CardBuilder;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksUtil;
import org.opencds.cqf.ruler.cdshooks.r4.CqlExecutionHandler;
import org.opencds.cqf.ruler.cdshooks.r4.R4CqlExecution;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;
import org.opencds.cqf.ruler.cdshooks.response.Cards;
import org.opencds.cqf.ruler.cdshooks.response.ErrorHandling;
import org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*

This servlet employs a cache to bypass performance issues with the EPIC FHIR Server.
The cache will be populated using order-select requests prior to the order-sign request.

if order-select for recommendation 10
	then return empty set of cards
		and run the order-sign logic assuming the order is for chronic or subacute pain (this includes the MCL queries)
		and store the CDS response in the cache
else if order-sign for recommendation 10
	then
		if the patient is in the cache (if the CDS response in the cache is null, wait for the order-select request to finish)
			then if the order is for chronic or subacute pain
				then return the CDS response from the cache
			else return empty cards
		else evaluate the MCL queries and order-sign logic
else run normally
*/

@Configurable
public class EpicCacheCdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final Logger logger = LoggerFactory.getLogger(EpicCacheCdsHooksServlet.class);

	private final DaoRegistry daoRegistry;
	private final AppProperties appProperties;
	private final CqlExecutionProvider cqlExecutionProvider;
	private final LibraryEvaluationProvider libraryExecutionProvider;
	private final ActivityDefinitionOperationsProvider applyEvaluator;
	private final ModelResolver modelResolver;
	private final CdsServicesCache cdsServicesCache;
	private final CDSHooksTransactionInterceptor knowledgeArtifactCache;
	private final IValidationSupport validationSupport;
	private final ServletRequestDetails requestDetails;
	private final R4CqlExecution r4CqlExecution;

	// Cache from patientId → future of the *real* order-sign response JSON
	private final Cache<String, CompletableFuture<OrderSelectMetadata>> orderSelectResponseCache =
		Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS)
			.maximumSize(1000)
			.build();

	@Autowired
	public EpicCacheCdsHooksServlet(
		DaoRegistry daoRegistry, AppProperties appProperties,
		CqlExecutionProvider cqlExecutionProvider, LibraryEvaluationProvider libraryExecutionProvider,
		ActivityDefinitionOperationsProvider applyEvaluator,
		ModelResolver modelResolver, CdsServicesCache cdsServicesCache,
		CDSHooksTransactionInterceptor knowledgeArtifactCache, RestfulServer restfulServer,
		IValidationSupport validationSupport) {
		this.daoRegistry = daoRegistry;
		this.appProperties = appProperties;
		this.cqlExecutionProvider = cqlExecutionProvider;
		this.libraryExecutionProvider = libraryExecutionProvider;
		this.applyEvaluator = applyEvaluator;
		this.modelResolver = modelResolver;
		this.cdsServicesCache = cdsServicesCache;
		this.knowledgeArtifactCache = knowledgeArtifactCache;
		this.validationSupport = validationSupport;
		this.requestDetails = new ServletRequestDetails();
		requestDetails.setFhirServerBase(appProperties.getServer_address());
		requestDetails.setServer(restfulServer);
		this.r4CqlExecution = new R4CqlExecution(appProperties.getServer_address());
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
		ErrorHandling.setAccessControlHeaders(response, appProperties);
		setResponseHeaders(response);
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		logger.info("GET {}", request.getRequestURI());
		if (!request.getRequestURL().toString().endsWith("/cds-services") &&
			!request.getRequestURL().toString().endsWith("/cds-services/")) {
			// Simply log a warning and continue with the discovery response
			logger.warn("Expected discovery endpoint URL {}/cds-services, found {}",
				appProperties.getServer_address(), request.getRequestURI());
		}
		ErrorHandling.setAccessControlHeaders(response, appProperties);
		var servicesJson = new GsonBuilder().setPrettyPrinting().create()
			.toJson(getServices());
		try {
			setResponseHeaders(response);
			response.getWriter().println(servicesJson);
		} catch (IOException ioe) {
			logger.error("I/O Exception during discovery response: ", ioe);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		var logging = new EpicLogging(logger);
		// Kick off async processing immediately:
		var asyncContext = request.startAsync();
		// 60s timeout if something goes wrong
		asyncContext.setTimeout(60_000);

		asyncContext.start(() -> {
			try {
				// 1) Validate & parse request
				if (!"application/json".equals(request.getContentType())) {
					response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
					return;
				}
				var raw = request.getReader().lines().collect(Collectors.joining());
				logging.logInfo(raw);
				var mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
				var hookReq = mapper.readValue(raw, CdsHooksRequest.class);
				var requestHandler = new CdsHooksRequestHandler(hookReq, logging);
				var patientId = requestHandler.getPatientId();
				var serviceId = request.getPathInfo().replace("/", "");

				// 2) Distinguish OrderSelect vs. OrderSign
				if (hookReq instanceof CdsHooksRequest.OrderSelect) {
					// --- ORDER‑SELECT branch ---
					// Create or replace the future placeholder
					CompletableFuture<OrderSelectMetadata> future = new CompletableFuture<>();
					var startTime = System.currentTimeMillis();
					var medicationCodes = requestHandler.getDraftOrderMedicationCodes();
					orderSelectResponseCache.put(patientId, future);

					// Return empty cards immediately
					writeJson(response, CdsHooksUtil.emptyCards());
					// Complete this async context — response is done
					asyncContext.complete();

					// Meanwhile, in the background compute the *real* order‑sign payload
					CompletableFuture
						.supplyAsync(() -> computeOrderSignPayload(requestHandler, serviceId, patientId, mapper, logging), ForkJoinPool.commonPool())
						.whenComplete((json, ex) -> {
							if (ex != null) {
								future.completeExceptionally(ex);
							} else {
								var result = new OrderSelectMetadata();
								result.setResponse(json);
								result.setStartTime(startTime);
								result.setMedicationCodes(medicationCodes);
								future.complete(result);
							}
						});

				} else if (hookReq instanceof CdsHooksRequest.OrderSign) {
					// --- ORDER‑SIGN branch ---
					CompletableFuture<OrderSelectMetadata> future = orderSelectResponseCache.getIfPresent(patientId);
					if (future != null && CollectionUtils.containsAny(future.get().getMedicationCodes(),
						requestHandler.getDraftOrderMedicationCodes())) {
						// log time between the order-select and order-sign request
						logging.logTimeBetweenRequests(patientId, System.currentTimeMillis() - future.get().startTime);

						// When the select logic finishes (or already finished), write that JSON
						future.whenCompleteAsync((osm, ex) -> {
							try {
								if (ex != null) {
									response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									response.getWriter().println("{\"error\":\"" + ex.getMessage() + "\"}");
								} else {
									if (ensureChronicOrSubacutePainOrder(requestHandler)) {
										writeJson(response, osm.getResponse());
									} else {
										writeJson(response, CdsHooksUtil.emptyCards());
									}
								}
							} catch (IOException ioe) {
								logging.logError(ioe.getMessage(), ioe);
							} finally {
								asyncContext.complete();
							}
						}, ForkJoinPool.commonPool());
					} else {
						// No prior select or different medication → ignore the cache
						if (future == null) {
							logging.logInfo("No cache hit due to order-select not being called for patient: " + patientId);
							logging.logPerformanceInfo("No cache hit due to order-select not being called for patient: " + patientId);
						} else {
							logging.logInfo("No cache hit due to medication mismatch for patient: " + patientId);
							logging.logPerformanceInfo("No cache hit due to medication mismatch for patient: " + patientId);
						}
						var json = computeOrderSignPayload(requestHandler, serviceId, patientId, mapper, logging);
						writeJson(response, json);
						asyncContext.complete();
					}

				} else {
					// --- NORMAL branch for other hooks ---
					writeJson(response, CdsHooksUtil.emptyCards());
					asyncContext.complete();
				}
			} catch (Exception e) {
				// Ensure we always complete the async context
				try {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("{\"error\":\"" + e.getMessage() + "\"}");
				} catch (IOException ignored) {}
				asyncContext.complete();
			}
		});
	}

	private void writeJson(HttpServletResponse resp, Object payload) {
		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("Access-Control-Allow-Origin", "*");
		try {
			// if payload is already a JSON string, write it directly:
			if (payload instanceof String) {
				resp.getWriter().println((String)payload);
			} else {
				resp.getWriter().println(new GsonBuilder()
					.disableHtmlEscaping()
					.setPrettyPrinting()
					.create()
					.toJson(JsonParser.parseString(new ObjectMapper().writeValueAsString(payload))));
			}
			resp.getWriter().flush();
		} catch (IOException ignored) {}
	}

	private String computeOrderSignPayload(CdsHooksRequestHandler cdsHooksRequestHandler, String serviceId,
														String patientId, ObjectMapper mapper, EpicLogging logging) {
		// Prepare Library evaluation
		configureLibraryAndTerminologyProviders();

		// Prepare evaluation and card building
		PlanDefinition planDefinition;
		try {
			planDefinition = read(Ids.newId(PlanDefinition.class, serviceId));
		} catch (ResourceNotFoundException e) {
			logging.logError(String.format("Could not resolve PlanDefinition/%s", serviceId), e);
			throw e;
		}
		if (!planDefinition.hasLibrary()) {
			logging.logError(String.format("PlanDefinition for service %s does not specify a primary library", serviceId));
			throw new IllegalArgumentException(String.format("PlanDefinition for service %s does not specify a primary library", serviceId));
		}
		IdType primaryLibraryId = Ids.newId(Library.class, Canonicals.getIdPart(planDefinition.getLibrary().get(0)));
		var cqlExecutionHandler = new CqlExecutionHandler(r4CqlExecution, libraryExecutionProvider, cqlExecutionProvider,
			primaryLibraryId, cdsHooksRequestHandler.getDraftOrdersParameters(),
			cdsHooksRequestHandler.useServerData(), cdsHooksRequestHandler.getPrefetchBundle());
		var expressions = CdsHooksUtil.getExpressions(planDefinition);
		var evaluationResults = cqlExecutionHandler.evaluateLibrary(patientId, expressions, null);

		// Build cards
		var cardBuilder = new CardBuilder(patientId, evaluationResults, planDefinition, applyEvaluator,
			requestDetails, modelResolver, cqlExecutionHandler);
		var cards = cardBuilder.buildCards();
		var result = new Cards();
		result.cards = cards.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (result.cards.isEmpty()) {
			logging.logNoGuidance(cdsHooksRequestHandler.mrn, cdsHooksRequestHandler.request.hookInstance);
		}

		// Serialize cards into response
		String jsonResponse = null;
		try {
			jsonResponse = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
				.toJson(JsonParser.parseString(mapper.writeValueAsString(result)));
			logging.logInfo(jsonResponse);
			logging.logRequestDuration(cdsHooksRequestHandler.request.hookInstance);
			return jsonResponse;
		} catch (JsonProcessingException | JsonSyntaxException jpe) {
			logging.logError("Error serializing CDS Hooks response: ", jpe);
		}
		logging.logRequestDuration(cdsHooksRequestHandler.request.hookInstance);
		return jsonResponse;
	}

	private void setResponseHeaders(HttpServletResponse response) {
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		// This resolves some CORS issues experienced with apps like the sandbox... Should resolve in the AppProperties
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	private JsonObject getServices() {
		var services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	// Should really be calling the CQL, but this maximizes performance
	private Boolean ensureChronicOrSubacutePainOrder(CdsHooksRequestHandler requestHandler) {
		var draftOrders = requestHandler.getDraftOrders();
		var expectedSupply = false;
		var validityPeriod = false;
		var boundsPeriod = false;
		if (draftOrders != null && draftOrders.isJsonObject()) {
			var bundle = getFhirContext().newJsonParser().parseResource(draftOrders.toString());
			if (bundle instanceof Bundle) {
				var draftOrder = BundleUtil.toListOfResourcesOfType(
					getFhirContext(), (Bundle) bundle, MedicationRequest.class);
				if (draftOrder.isEmpty()) {
					return false;
				} else {
					var order = draftOrder.get(0);
					if (order.hasDispenseRequest()
						&& order.getDispenseRequest().hasExpectedSupplyDuration()
						&& order.getDispenseRequest().getExpectedSupplyDuration().hasValue()) {
						expectedSupply = order.getDispenseRequest().getExpectedSupplyDuration().getValue().doubleValue() >= 28.0;
					}
					if (order.hasDispenseRequest()
						&& order.getDispenseRequest().hasValidityPeriod()) {
						var start = order.getDispenseRequest().getValidityPeriod().getStart();
						var end = order.getDispenseRequest().getValidityPeriod().getEnd();
						if (start != null && end != null) {
							validityPeriod = Duration.between(start.toInstant(), end.toInstant()).toDays() >= 28;
						}
					}
					if (order.hasDosageInstruction()) {
						for (var dosage : order.getDosageInstruction()) {
							if (dosage.hasTiming() && dosage.getTiming().hasRepeat() && dosage.getTiming().getRepeat().hasBoundsPeriod()) {
								var start = dosage.getTiming().getRepeat().getBoundsPeriod().getStart();
								var end = dosage.getTiming().getRepeat().getBoundsPeriod().getEnd();
								if (start != null && end != null) {
									boundsPeriod = Duration.between(start.toInstant(), end.toInstant()).toDays() >= 28;
								}
							}
						}
					}
				}
			}
		}
		return expectedSupply || validityPeriod || boundsPeriod;
	}

	private void configureLibraryAndTerminologyProviders() {
		var libraries = knowledgeArtifactCache.getLibraryCache();
		this.libraryExecutionProvider.setLibraryLoader(new InMemoryLibraryLoader(libraries.values()));

		var valueSets = knowledgeArtifactCache.getValueSetCache();
		var terminologyProvider = new HapiTerminologyProvider(validationSupport, valueSets, requestDetails);
		this.libraryExecutionProvider.setTerminologyProvider(terminologyProvider);
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}

	private class CdsHooksRequestHandler {
		private final CdsHooksRequest request;
		private final EpicLogging logging;
		private final Endpoint remoteDataEndpoint;
		private String mrn;

		public CdsHooksRequestHandler(CdsHooksRequest request, EpicLogging logging) {
			this.request = request;
			this.logging = logging;
			if (request.fhirServer != null && !request.fhirServer.equals(appProperties.getServer_address())) {
				var ep = new Endpoint().setAddress(request.fhirServer);
				if (request.fhirAuthorization != null) {
					ep.addHeader(String.format("Authorization: %s %s",
						request.fhirAuthorization.tokenType,
						request.fhirAuthorization.accessToken)
					);
				}
				remoteDataEndpoint = ep;
			} else {
				remoteDataEndpoint = new Endpoint().setAddress(appProperties.getServer_address());
			}
		}

		public String getPatientId() {
			if (request instanceof CdsHooksRequest.OrderSelect) {
				return ((CdsHooksRequest.OrderSelect) request).context.patientId;
			} else if (request instanceof CdsHooksRequest.OrderSign) {
				return ((CdsHooksRequest.OrderSign) request).context.patientId;
			} else {
				return request.context.patientId;
			}
		}

		public JsonObject getDraftOrders() {
			if (request instanceof CdsHooksRequest.OrderSelect) {
				return ((CdsHooksRequest.OrderSelect) request).context.draftOrders;
			} else if (request instanceof CdsHooksRequest.OrderSign) {
				return ((CdsHooksRequest.OrderSign) request).context.draftOrders;
			}
			return null;
		}

		public Parameters getDraftOrdersParameters() {
			var draftOrders = getDraftOrders();
			logging.logDraftOrders(draftOrders);
			return draftOrders == null ? null : CdsHooksUtil.getParameters(draftOrders);
		}

		public Bundle getDraftOrdersBundle() {
			var draftOrders = getDraftOrders();
			return getFhirContext().newJsonParser().parseResource(Bundle.class, new Gson().toJson(draftOrders));
		}

		public List<String> getDraftOrderMedicationCodes() {
			var medCodes = new ArrayList<String>();
			var draftOrdersBundle = getDraftOrdersBundle();
			var medReqs = BundleUtil.toListOfResourcesOfType(getFhirContext(), draftOrdersBundle, MedicationRequest.class);
			var meds = BundleUtil.toListOfResourcesOfType(getFhirContext(), draftOrdersBundle, Medication.class);
			for (var medReq : medReqs) {
				if (medReq.hasMedicationReference()) {
					// Making an assumption that the draftOrders bundle will contain the Medication resource - should be safe for EPIC
					var match = meds.stream().filter(
						med -> medReq.getMedicationReference().getReference().endsWith(med.getIdPart())).findFirst();
					if (match.isPresent() && match.get().hasCode() && match.get().getCode().hasCoding()) {
						medCodes.addAll(match.get().getCode().getCoding().stream()
							.map(Coding::getCode).collect(Collectors.toList()));
					}
				} else if (medReq.hasMedicationCodeableConcept() && medReq.getMedicationCodeableConcept().hasCoding()) {
					medCodes.addAll(medReq.getMedicationCodeableConcept().getCoding().stream()
						.map(Coding::getCode).collect(Collectors.toList()));
				}
			}
			return medCodes;
		}

		public BooleanType useServerData() {
			return new BooleanType(false);
		}

		public Bundle getPrefetchBundle() {
			var data = CdsHooksUtil.getPrefetchResources(request);
			var draftOrders = getDraftOrders();
			// Use prefetch resources if provided otherwise use MCL
			if (data == null) {
				var configResolver = new EpicModuleConfigurationResolver(getFhirContext(), remoteDataEndpoint, request);
				data = configResolver.getPrefetchBundle();
				logging.logMclQueryPerformanceResult(configResolver.getQueryResultMap());
			}

			if (draftOrders != null) {
				// Add non-request resources (e.g. referenced Medications) to bundle
				CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrders, data);
			}

			logging.logBundleResources(data);

			// Get the patient MRN for logging
			var patient = BundleUtil.toListOfResourcesOfType(getFhirContext(), data, Patient.class).stream().findFirst().orElseThrow();
			if (patient.hasIdentifier()) {
				var mrn = patient.getIdentifier().stream().filter(identifier -> identifier.hasType() && identifier.getType().hasText() && identifier.getType().getText().equals("EPICMRN")).findFirst();
				mrn.ifPresent(identifier -> this.mrn = identifier.getValue());
			}

			return data;
		}
	}

	private class OrderSelectMetadata {
		private String response;
		private Long startTime;
		private List<String> medicationCodes;

		public String getResponse() {
			return response;
		}

		public void setResponse(String response) {
			this.response = response;
		}

		public Long getStartTime() {
			return startTime;
		}

		public void setStartTime(Long startTime) {
			this.startTime = startTime;
		}

		public List<String> getMedicationCodes() {
			return medicationCodes;
		}

		public void setMedicationCodes(List<String> medicationCodes) {
			this.medicationCodes = medicationCodes;
		}
	}
}
