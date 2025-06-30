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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CDSHooksTransactionInterceptor;
import org.opencds.cqf.ruler.cdshooks.CdsHooksProperties;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Configurable
public class EpicCacheTimeoutCdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final Logger logger = LoggerFactory.getLogger(EpicCacheTimeoutCdsHooksServlet.class);

	private final DaoRegistry daoRegistry;
	private final AppProperties appProperties;
	private final CqlExecutionProvider cqlExecutionProvider;
	private final LibraryEvaluationProvider libraryExecutionProvider;
	private final ActivityDefinitionOperationsProvider applyEvaluator;
	private final ModelResolver modelResolver;
	private final CdsServicesCache cdsServicesCache;
	private final CDSHooksTransactionInterceptor knowledgeArtifactCache;
	private final CdsHooksProperties cdsHooksProperties;
	private final IValidationSupport validationSupport;
	private final ServletRequestDetails requestDetails;
	private final R4CqlExecution r4CqlExecution;

	// Cache from patientId → future of the *real* order-sign response JSON
	private final Cache<String, CompletableFuture<CacheMetadata>> orderSelectResponseCache =
		Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS)
			.maximumSize(1000)
			.build();

	@Autowired
	public EpicCacheTimeoutCdsHooksServlet(
		DaoRegistry daoRegistry, AppProperties appProperties,
		CqlExecutionProvider cqlExecutionProvider, LibraryEvaluationProvider libraryExecutionProvider,
		ActivityDefinitionOperationsProvider applyEvaluator,
		ModelResolver modelResolver, CdsServicesCache cdsServicesCache,
		CDSHooksTransactionInterceptor knowledgeArtifactCache, RestfulServer restfulServer,
		CdsHooksProperties cdsHooksProperties, IValidationSupport validationSupport) {
		this.daoRegistry = daoRegistry;
		this.appProperties = appProperties;
		this.cqlExecutionProvider = cqlExecutionProvider;
		this.libraryExecutionProvider = libraryExecutionProvider;
		this.applyEvaluator = applyEvaluator;
		this.modelResolver = modelResolver;
		this.cdsServicesCache = cdsServicesCache;
		this.knowledgeArtifactCache = knowledgeArtifactCache;
		this.validationSupport = validationSupport;
		this.cdsHooksProperties = cdsHooksProperties;
		this.requestDetails = new ServletRequestDetails();
		requestDetails.setFhirServerBase(appProperties.getServer_address());
		requestDetails.setServer(restfulServer);
		this.r4CqlExecution = new R4CqlExecution(appProperties.getServer_address());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
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
		asyncContext.start(() -> {
			try {
				// 1) Validate & parse request
				if (!"application/json".equals(request.getContentType())) {
					logging.logError("Unsupported content type: " + request.getContentType());
					response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
					return;
				}

				var raw = request.getReader().lines().collect(Collectors.joining());
				logging.logInfo(raw);
				var mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
				var hookReq = mapper.readValue(raw, CdsHooksRequest.class);
				var requestHelper = new CdsHooksRequestHelper(
					hookReq, request.getPathInfo().replace("/", ""), appProperties.getServer_address(), logging);

				// 2) Distinguish OrderSelect vs. OrderSign
				if (hookReq instanceof CdsHooksRequest.OrderSelect) {
					var future = new CompletableFuture<CacheMetadata>();
					var startTime = System.currentTimeMillis();
					orderSelectResponseCache.put(requestHelper.getPatientId(), future);

					// Return empty cards immediately
					writeJson(response, CdsHooksUtil.emptyCards());
					// Complete this async context — response is done
					asyncContext.complete();

					// Meanwhile, in the background compute the *real* order‑sign payload
					// Do not include the timeout here...
					CompletableFuture
						.supplyAsync(() ->
							computeOrderSignPayload(requestHelper, mapper, logging), ForkJoinPool.commonPool())
						.whenComplete((json, ex) -> {
							if (ex != null) {
								future.completeExceptionally(ex);
							} else {
								future.complete(new CacheMetadata(json, startTime));
							}
						});
				} else if (hookReq instanceof CdsHooksRequest.OrderSign) {
					var future = orderSelectResponseCache.getIfPresent(requestHelper.getPatientId());
					if (future != null) {
						// Wait for order-select to finish
						future.whenCompleteAsync((cachedMetaData, ex) -> {
							try {
								if (ex != null) {
									response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									response.getWriter().println("{\"error\":\"" + ex.getMessage() + "\"}");
								} else {
									if (ensureChronicOrSubacutePainOrder(requestHelper)) {
										writeJson(response, cachedMetaData.getResponse());
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
						var json = computeOrderSignPayloadWithTimeout(requestHelper, mapper, logging);
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
					logging.logError(e.getMessage(), e);
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

	/**
	 * Runs computeOrderSignPayload with a hard timeout.
	 * If the timeout elapses we:
	 *   • cancel the task
	 *   • write a structured log
	 *   • return an empty set of cards so the hook stays responsive.
	 */
	private String computeOrderSignPayloadWithTimeout(
		CdsHooksRequestHelper helper,
		ObjectMapper mapper,
		EpicLogging logging) {

		var pool = ForkJoinPool.commonPool(); // reuse existing pool
		var future = pool.submit(() -> computeOrderSignPayload(helper, mapper, logging));
		try {
			return future.get(cdsHooksProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			// stop the task
			future.cancel(true);

			// log as much context as possible
			logging.logError("Order-sign evaluation **timed-out** after "
				+ cdsHooksProperties.getRequestTimeoutMs() + " ms  "
				+ "(patient="  + helper.getPatientId()
				+ ", hookInstance=" + helper.getRequest().hookInstance
				+ ", serviceId=" + helper.getServiceId() + ")");

			// graceful fall-back – empty cards
			return CdsHooksUtil.emptyCards();
		} catch (ExecutionException | InterruptedException e) {
			logging.logError("Order-sign evaluation failed: ", e);
			return CdsHooksUtil.emptyCards();
		}
	}

	private String computeOrderSignPayload(
		CdsHooksRequestHelper helper, ObjectMapper mapper,
		EpicLogging logging) {
		// Prepare Library evaluation
		configureLibraryAndTerminologyProviders();

		// Prepare evaluation and card building
		PlanDefinition planDefinition;
		try {
			planDefinition = read(Ids.newId(PlanDefinition.class, helper.getServiceId()));
		} catch (ResourceNotFoundException e) {
			logging.logError(String.format("Could not resolve PlanDefinition/%s", helper.getServiceId()), e);
			throw e;
		}
		if (!planDefinition.hasLibrary()) {
			logging.logError(String.format(
				"PlanDefinition for service %s does not specify a primary library", helper.getServiceId()));
			throw new IllegalArgumentException(String.format(
				"PlanDefinition for service %s does not specify a primary library", helper.getServiceId()));
		}
		IdType primaryLibraryId = Ids.newId(Library.class, Canonicals.getIdPart(planDefinition.getLibrary().get(0)));
		var cqlExecutionHandler = new CqlExecutionHandler(
			r4CqlExecution, libraryExecutionProvider, cqlExecutionProvider,
			primaryLibraryId, helper.getDraftOrdersParameters(),
			helper.useServerData(), helper.getPrefetchBundle());
		var expressions = CdsHooksUtil.getExpressions(planDefinition);

		var evaluationResults = cqlExecutionHandler.evaluateLibrary(
			helper.getPatientId(), expressions, null);

		// TODO: Log decision provenance (rationale extension -> expression reference(s))
		// 	e.g. UDS Recommendation, No UDS Recommendation, Possible Unexpected Results

		// Build cards
		var cardBuilder = new CardBuilder(
			helper.getPatientId(), evaluationResults, planDefinition,
			applyEvaluator, requestDetails, modelResolver, cqlExecutionHandler);
		var cards = cardBuilder.buildCards();
		var result = new Cards();
		result.cards = cards.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (result.cards.isEmpty()) {
			logging.logNoGuidance(helper.getMrn(), helper.getRequest().hookInstance);
		}

		// Serialize cards into response
		String jsonResponse = null;
		try {
			jsonResponse = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
				.toJson(JsonParser.parseString(mapper.writeValueAsString(result)));
			logging.logInfo(jsonResponse);
			logging.logRequestDuration(helper.getRequest().hookInstance);
			return jsonResponse;
		} catch (JsonProcessingException | JsonSyntaxException jpe) {
			logging.logError("Error serializing CDS Hooks response: ", jpe);
		}
		logging.logRequestDuration(helper.getRequest().hookInstance);
		return jsonResponse;
	}

	private void configureLibraryAndTerminologyProviders() {
		var libraries = knowledgeArtifactCache.getLibraryCache();
		this.libraryExecutionProvider.setLibraryLoader(new InMemoryLibraryLoader(libraries.values()));

		var valueSets = knowledgeArtifactCache.getValueSetCache();
		var terminologyProvider = new HapiTerminologyProvider(validationSupport, valueSets, requestDetails);
		this.libraryExecutionProvider.setTerminologyProvider(terminologyProvider);
	}

	// Should really be calling the CQL, but this maximizes performance
	private Boolean ensureChronicOrSubacutePainOrder(CdsHooksRequestHelper helper) {
		var draftOrders = helper.getDraftOrdersBundle();
		var expectedSupply = false;
		var validityPeriod = false;
		var boundsPeriod = false;
		if (draftOrders != null) {
			var draftOrder = BundleUtil.toListOfResourcesOfType(
				getFhirContext(), draftOrders, MedicationRequest.class);
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
		return expectedSupply || validityPeriod || boundsPeriod;
	}

	private JsonObject getServices() {
		var services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	private void setResponseHeaders(HttpServletResponse response) {
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		// This resolves some CORS issues experienced with apps like the sandbox... Should resolve in the AppProperties
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}
}
