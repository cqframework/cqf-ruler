package org.opencds.cqf.ruler.cdshooks.r4;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.cr.common.HapiTerminologyProvider;
import ca.uhn.fhir.cr.config.CrProperties;
import ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.BundleUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.exception.DataProviderException;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CDSHooksTransactionInterceptor;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;
import org.opencds.cqf.ruler.cdshooks.response.Card;
import org.opencds.cqf.ruler.cdshooks.response.Cards;
import org.opencds.cqf.ruler.cdshooks.response.ErrorHandling;
import org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Configurable
public class CdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final long serialVersionUID = 1L;

	// --- Loggers ---
	private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

	// Performance logger
	private static final Logger performanceLogger =
		LoggerFactory.getLogger("org.opencds.cds.performance");

	// Errors with PHI
	private static final Logger errorLogger =
		LoggerFactory.getLogger("org.opencds.cds.error");

	// Info with PHI
	private static final Logger infoLogger =
		LoggerFactory.getLogger("org.opencds.cds.info");

	// Redacted info
	private static final Logger infoRedactedLogger =
		LoggerFactory.getLogger("org.opencds.cds.info.redacted");

	// --- Autowired ---
	@Autowired private CrProperties.CqlProperties cqlProperties;
	@Autowired private DaoRegistry daoRegistry;
	@Autowired private AppProperties myAppProperties;
	@Autowired private CqlExecutionProvider cqlExecution;
	@Autowired private LibraryEvaluationProvider libraryExecution;
	@Autowired private ActivityDefinitionOperationsProvider applyEvaluator;
	@Autowired private ProviderConfiguration providerConfiguration;
	@Autowired private ModelResolver modelResolver;
	@Autowired private CdsServicesCache cdsServicesCache;
	@Autowired private CDSHooksTransactionInterceptor cache;
	@Autowired private RestfulServer restfulServer;
	@Autowired private IValidationSupport validationSupport;

	// --- Other Fields ---
	private final ServletRequestDetails requestDetails = new ServletRequestDetails();

	private R4CqlExecution cqlExecutor;
	private boolean isEpic = false;
	private CdsHooksRequest.OrderSign.Context orderSignContext;

	private Cache<String, String> orderSelectResponseCache = Caffeine.newBuilder()
		.expireAfterWrite(1, TimeUnit.HOURS)
		.build();


	// -------------------------------------------------------
	// CORS Pre-flight
	// -------------------------------------------------------
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		ErrorHandling.setAccessControlHeaders(resp, myAppProperties);
		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("X-Content-Type-Options", "nosniff");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	// -------------------------------------------------------
	// GET => Provide /cds-services
	// -------------------------------------------------------
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		logger.info(request.getRequestURI());
		if (!request.getRequestURL().toString().endsWith("/cds-services") &&
			!request.getRequestURL().toString().endsWith("/cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests other than /cds-services.");
		}

		ErrorHandling.setAccessControlHeaders(response, myAppProperties);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.setHeader("Access-Control-Allow-Origin", "*");

		// Return the services JSON
		String servicesJson = new GsonBuilder().setPrettyPrinting().create()
			.toJson(getServices());
		response.getWriter().println(servicesJson);
	}

	// -------------------------------------------------------
	// POST => Primary logic
	// -------------------------------------------------------
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		this.isEpic = false;
		this.orderSignContext = null;

		String requestInstance = null;

		try {
			validateRequestContentType(request);
			String baseUrl = myAppProperties.getServer_address();
			String service = deriveServiceName(request);

			ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
			String requestJson = readRequestJson(request);
			CdsHooksRequest cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
			requestInstance = cdsHooksRequest.hookInstance;

			logRequestInfo(cdsHooksRequest, requestJson);
			this.cqlExecutor = new R4CqlExecution(baseUrl);
			requestDetails.setFhirServerBase(baseUrl);
			requestDetails.setServer(restfulServer);

			PlanDefinition servicePlan = readPlanDefinition(service);
			IdType logicId = extractLogicId(servicePlan);
			String patientId = determinePatientId(cdsHooksRequest);
			Parameters parameters = buildContextParameters(cdsHooksRequest);
			List<String> expressions = CdsHooksUtil.getExpressions(servicePlan);
			BooleanType useServerData = null;
			Endpoint remoteDataEndpoint = buildRemoteDataEndpointIfNeeded(cdsHooksRequest, baseUrl);

			// If endpoint is null, fallback to the FHIR server from the request
			if (remoteDataEndpoint == null) {
				remoteDataEndpoint = new Endpoint().setAddress(cdsHooksRequest.fhirServer);
			} else {
				useServerData = new BooleanType(false);
			}

			// Determine if this is an Epic case
			boolean isEpicOrderSign = handleEpicOrderSignCase(cdsHooksRequest, service);
			boolean isEpicOrderSelect = handleEpicOrderSelectCase(cdsHooksRequest, service);

			// Cache check for Epic Order Sign
			if (isEpicOrderSign) {
				String cachedResponse = orderSelectResponseCache.getIfPresent(patientId);
				if (cachedResponse != null) {
					// Check if the order is for subacute/chronic pain
					var data = new Bundle();
					CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(getDraftOrders(cdsHooksRequest), data);
					var evaluationResult = cqlExecutor.getLibraryExecution(
						libraryExecution, logicId, patientId, Collections.singletonList("Patient Is Being Prescribed Opioid Analgesic with Ambulatory Misuse Potential"),
						parameters, useServerData, data, null
					);
					if (evaluationResult.hasParameter("return")) {
						var result = evaluationResult.getParameter("return").getValue();
						if (result instanceof BooleanType && !((BooleanType) result).booleanValue()) {
							// if not for subacute/chronic pain, return empty cards
							writeResponse(response, new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
								.toJson(JsonParser.parseString(mapper.writeValueAsString(new Cards()))));
							return;
						}
					} else {
						// null response or other issue - return empty cards
						writeResponse(response, new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
							.toJson(JsonParser.parseString(mapper.writeValueAsString(new Cards()))));
						return;
					}
					// If the cached value is empty, wait until it gets populated (up to a maximum wait time)
					if (cachedResponse.isEmpty()) {
						final int MAX_RETRIES = 30; // for example, wait up to 30 * 100ms = 3 seconds
						int retries = 0;
						while (cachedResponse.isEmpty() && retries < MAX_RETRIES) {
							try {
								Thread.sleep(100); // wait 100ms before checking again
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();
								break;
							}
							cachedResponse = orderSelectResponseCache.getIfPresent(patientId);
							retries++;
						}
					}
					if (!cachedResponse.isEmpty()) {
						writeResponse(response, cachedResponse);
						long endTime = System.currentTimeMillis();
						long durationMs = endTime - startTime;
						performanceLogger.info(
							"CDS Hook request (cache hit) for hook instance {} took {} ms",
							cdsHooksRequest.hookInstance, durationMs
						);
						return;
					}
				}
			} else if (isEpicOrderSelect) {
				// Create an empty cache entry for the patient to acknowledge a requests has been made
				String cachedResponse = orderSelectResponseCache.getIfPresent(patientId);
				if (cachedResponse == null) {
					orderSelectResponseCache.put(patientId, "");
					// return empty cards
					writeResponse(response, new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
						.toJson(JsonParser.parseString(mapper.writeValueAsString(new Cards()))));
					response.flushBuffer();
				} else {
					writeResponse(response, cachedResponse);
				}
			}

			Bundle data;
			Parameters evaluationResult;
			configureLibraryAndTerminologyProviders();

			if (isEpicOrderSign || isEpicOrderSelect) {
				this.isEpic = true;
				data = resolvePrefetchBundleForEpic(cdsHooksRequest, remoteDataEndpoint);
				evaluationResult = cqlExecutor.getLibraryExecution(
					libraryExecution, logicId, patientId, expressions,
					parameters, useServerData, data, null
				);
			} else {
				data = CdsHooksUtil.getPrefetchResources(cdsHooksRequest);
				evaluationResult = cqlExecutor.getLibraryExecution(
					libraryExecution, logicId, patientId, expressions,
					parameters, useServerData, data, remoteDataEndpoint
				);
			}

			// Build Card list (the actual "hook" response)
			List<Card> cards = buildCards(servicePlan, evaluationResult, patientId);
			Cards result = new Cards();
			result.cards = cards.stream().filter(Objects::nonNull).collect(Collectors.toList());
			String jsonResponse = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
				.toJson(JsonParser.parseString(mapper.writeValueAsString(result)));

			// Cache response for Epic Order Select case
			if (isEpicOrderSelect) {
				orderSelectResponseCache.put(patientId, jsonResponse);
			} else {
				writeResponse(response, jsonResponse);
			}

			long endTime = System.currentTimeMillis();
			long durationMs = endTime - startTime;
			performanceLogger.info(
				"CDS Hook request for hook instance {} took {} ms",
				cdsHooksRequest.hookInstance, durationMs
			);

		} catch (BaseServerResponseException e) {
			handleException(response, "ERROR: Exception connecting to remote server.", e, requestInstance);
		} catch (DataProviderException e) {
			handleException(response, "ERROR: Exception in DataProvider.", e, requestInstance);
		} catch (CqlException e) {
			handleException(response, "ERROR: Exception in CQL Execution.", e, requestInstance);
		} catch (Exception e) {
			logger.error("Error encountered: ", e);
			errorLogger.error("Error encountered for request with hook instance {}, error: {}", requestInstance, e.toString());
			throw new ServletException("ERROR: Exception in cds-hooks processing.", e);
		}
	}

	// -------------------------------------------------------
	// Helper Methods
	// -------------------------------------------------------

	private void configureLibraryAndTerminologyProviders() {
		var libraries = cache.getLibraryCache();
		this.libraryExecution.setLibraryLoader(new InMemoryLibraryLoader(libraries.values()));

		var valuesets = cache.getValueSetCache();
		HapiTerminologyProvider terminologyProvider = new HapiTerminologyProvider(validationSupport, valuesets, requestDetails);
		this.libraryExecution.setTerminologyProvider(terminologyProvider);
	}

	private String deriveServiceName(HttpServletRequest request) {
		// e.g., if pathInfo is "/serviceName", we remove the leading slash
		return request.getPathInfo().replace("/", "");
	}

	private String readRequestJson(HttpServletRequest request) throws IOException {
		return request.getReader().lines().collect(Collectors.joining());
	}

	private PlanDefinition readPlanDefinition(String service) {
		PlanDefinition plan = read(Ids.newId(PlanDefinition.class, service));
		if (!plan.hasLibrary()) {
			throw new ErrorHandling.CdsHooksError(
				"Logic library reference missing from PlanDefinition: " + plan.getId()
			);
		}
		return plan;
	}

	private IdType extractLogicId(PlanDefinition servicePlan) {
		return Ids.newId(
			Library.class,
			Canonicals.getIdPart(servicePlan.getLibrary().get(0))
		);
	}

	private String determinePatientId(CdsHooksRequest cdsHooksRequest) {
		if (cdsHooksRequest instanceof CdsHooksRequest.OrderSelect) {
			return ((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.patientId;
		} else if (cdsHooksRequest instanceof CdsHooksRequest.OrderSign) {
			return ((CdsHooksRequest.OrderSign) cdsHooksRequest).context.patientId;
		} else {
			return cdsHooksRequest.context.patientId;
		}
	}

	private Parameters buildContextParameters(CdsHooksRequest cdsHooksRequest) {
		if (cdsHooksRequest instanceof CdsHooksRequest.OrderSelect) {
			return CdsHooksUtil.getParameters(
				((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.draftOrders
			);
		} else if (cdsHooksRequest instanceof CdsHooksRequest.OrderSign) {
			this.orderSignContext = ((CdsHooksRequest.OrderSign) cdsHooksRequest).context;
			return CdsHooksUtil.getParameters(orderSignContext.draftOrders);
		} else {
			return null;
		}
	}

	private Endpoint buildRemoteDataEndpointIfNeeded(CdsHooksRequest cdsHooksRequest, String baseUrl) {
		if (cdsHooksRequest.fhirServer != null && !cdsHooksRequest.fhirServer.equals(baseUrl)) {
			Endpoint ep = new Endpoint().setAddress(cdsHooksRequest.fhirServer);
			if (cdsHooksRequest.fhirAuthorization != null) {
				ep.addHeader(String.format("Authorization: %s %s",
					cdsHooksRequest.fhirAuthorization.tokenType,
					cdsHooksRequest.fhirAuthorization.accessToken)
				);
			}
			return ep;
		}
		return null;
	}

	private boolean handleEpicOrderSignCase(CdsHooksRequest cdsHooksRequest, String service) {
		return (cdsHooksRequest instanceof CdsHooksRequest.OrderSign)
			&& "opioidcds-10-order-sign".equals(service);
	}

	private boolean handleEpicOrderSelectCase(CdsHooksRequest cdsHooksRequest, String service) {
		return (cdsHooksRequest instanceof CdsHooksRequest.OrderSelect)
			&& "opioidcds-10-order-select".equals(service);
	}

	private Bundle resolvePrefetchBundleForEpic(CdsHooksRequest cdsHooksRequest, Endpoint remoteDataEndpoint) {
		Bundle data;
		// Use prefetch resources if provided
		if (cdsHooksRequest.prefetch != null
			&& cdsHooksRequest.prefetch.resources != null
			&& !cdsHooksRequest.prefetch.resources.isEmpty()) {

			data = CdsHooksUtil.getPrefetchResources(cdsHooksRequest);

			// Get draft orders from the request context if available
			var draftOrders = getDraftOrders(cdsHooksRequest);
			if (draftOrders != null) {
				CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrders, data);
			}
			logBundleResources(data);
		} else {
			// If no prefetch, fetch data using configured queries
			ModuleConfigurationResolver configResolver = new ModuleConfigurationResolver(getFhirContext(), remoteDataEndpoint, cdsHooksRequest);
			data = configResolver.getPrefetchBundle();

			// Log performance of the queries
			configResolver.getPerformanceMap().forEach((k, v) ->
				performanceLogger.info("Time for query: {}, {} ms", k, v)
			);

			var draftOrders = getDraftOrders(cdsHooksRequest);
			if (draftOrders != null) {
				CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrders, data);
			}
			logBundleResources(data);
		}
		return data;
	}

	/**
	 * Helper method to extract draft orders from the context if the request is of type
	 * OrderSelect or OrderSign.
	 */
	private JsonObject getDraftOrders(CdsHooksRequest request) {
		if (request instanceof CdsHooksRequest.OrderSelect) {
			return ((CdsHooksRequest.OrderSelect) request).context.draftOrders;
		} else if (request instanceof CdsHooksRequest.OrderSign) {
			return ((CdsHooksRequest.OrderSign) request).context.draftOrders;
		}
		return null;
	}

	private void logBundleResources(Bundle data) {
		logger.info("================== Resource Log Start ==================");
		infoLogger.info("================== Resource Log Start ==================");

		for (IBaseResource r : BundleUtil.toListOfResources(getFhirContext(), data)) {
			String resourceJson = getFhirContext().newJsonParser().encodeResourceToString(r);
			logger.info(resourceJson);
			infoLogger.info(resourceJson);
		}

		logger.info("================== Resource Log End ==================");
		infoLogger.info("================== Resource Log End ==================");
	}

	private List<Card> buildCards(PlanDefinition servicePlan, Parameters evaluationResult, String patientId) {
		List<Card.Link> planLinks = resolvePlanLinks(servicePlan);
		List<Card> cards = new ArrayList<>();

		if (servicePlan.hasAction()) {
			Card topLevelCard = resolveServicePlan(
				servicePlan.getAction(),
				evaluationResult,
				patientId,
				cards,
				planLinks,
				true,
				null
			);
			if (topLevelCard != null) {
				cards.add(topLevelCard);
			}
		}
		return cards;
	}

	private void writeResponse(HttpServletResponse response, String jsonResponse) throws IOException {
		logger.info(jsonResponse);
		infoLogger.info(jsonResponse);

		response.setContentType("text/json;charset=UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.getWriter().println(jsonResponse);
	}

	private void validateRequestContentType(HttpServletRequest request) throws ServletException {
		String contentType = request.getContentType();
		if (contentType == null || !contentType.startsWith("application/json")) {
			throw new ServletException(String.format(
				"Invalid content type %s. Please use application/json.", contentType
			));
		}
	}

	private void handleException(
		HttpServletResponse response, String errorMsg,
		Exception e, String requestInstance
	) throws IOException {

		ErrorHandling.handleError(response, errorMsg, e, myAppProperties);
		errorLogger.error("Error for request with hook instance {}, error: {}", requestInstance, e.toString());
		logger.error(e.toString());
	}

	// -------------------------------------------------------
	// Logging Helpers
	// -------------------------------------------------------
	private void logRequestInfo(CdsHooksRequest request, String jsonRequest) {
		logger.info(jsonRequest);
		infoLogger.info(jsonRequest);

		logger.info("cds-hooks hook instance: {}", request.hookInstance);
		infoLogger.info("cds-hooks hook instance: {}", request.hookInstance);
		infoRedactedLogger.info("cds-hooks hook instance: {}", request.hookInstance);

		logWithRedaction("cds-hooks maxCodesPerQuery: {}", getProviderConfiguration().getMaxCodesPerQuery());
		logWithRedaction("cds-hooks expandValueSets: {}", getProviderConfiguration().getExpandValueSets());
		logWithRedaction("cds-hooks queryBatchThreshold: {}", getProviderConfiguration().getQueryBatchThreshold());
		logWithRedaction("cds-hooks searchStyle: {}", getProviderConfiguration().getSearchStyle());
		logWithRedaction("cds-hooks prefetch maxUriLength: {}", getProviderConfiguration().getMaxUriLength());

		logWithRedaction("cds-hooks local server address: {}", myAppProperties.getServer_address());
		logWithRedaction("cds-hooks fhir server address: {}", request.fhirServer);
		logWithRedaction("cds-hooks cql_logging_enabled: {}", getProviderConfiguration().getCqlLoggingEnabled());
	}

	private void logWithRedaction(String format, Object arg) {
		logger.info(format, arg);
		infoLogger.info(format, arg);
		infoRedactedLogger.info(format, arg);
	}

	// -------------------------------------------------------
	// PlanDefinition / Card Building
	// -------------------------------------------------------
	private List<Card.Link> resolvePlanLinks(PlanDefinition servicePlan) {
		List<Card.Link> links = new ArrayList<>();
		if (servicePlan.hasRelatedArtifact()) {
			for (RelatedArtifact ra : servicePlan.getRelatedArtifact()) {
				Card.Link link = new Card.Link();
				if (ra.hasDisplay()) {
					link.setLabel(ra.getDisplay());
				}
				if (ra.hasUrl()) {
					link.setUrl(ra.getUrl());
				}
				// Use "absolute" as default type if none provided
				link.setType(ra.hasExtension()
					? ra.getExtensionFirstRep().getValue().primitiveValue()
					: "absolute"
				);
				links.add(link);
			}
		}
		return links;
	}

	private Card resolveServicePlan(
		List<PlanDefinition.PlanDefinitionActionComponent> actions,
		Parameters evaluationResults,
		String patientId,
		List<Card> cards,
		List<Card.Link> links,
		boolean newCard,
		Card oldCard
	) {
		Card card = newCard ? new Card() : oldCard;

		for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
			if (resolveCondition(action, evaluationResults, patientId).get()) {
				processActionProperties(action, card, evaluationResults, patientId);

				if (action.hasAction()) {
					resolveServicePlan(action.getAction(), evaluationResults, patientId, cards, links, false, card);
				}
				// If Epic, set extension
				if (this.isEpic) {
					Card.Extension extension = new Card.Extension();
					extension.setMimeType("text/html");
					card.setExtension(extension);
				}
			}
		}

		// Only attach links if we have a summary
		if (card.getSummary() != null && links != null) {
			card.setLinks(links);
		} else if (card.getSummary() == null) {
			return null;
		}
		return card;
	}

	private void processActionProperties(
		PlanDefinition.PlanDefinitionActionComponent action,
		Card card,
		Parameters evaluationResults,
		String patientId
	) {
		// Title, description, priority => summary, detail, indicator
		if (action.hasTitle()) {
			card.setSummary(action.getTitle());
		}
		if (action.hasDescription()) {
			card.setDetail(action.getDescription());
		}
		if (action.hasPriority()) {
			String indicator = deriveIndicator(action.getPriority());
			card.setIndicator(indicator);
		}
		if (action.hasDocumentation()) {
			card.setSource(resolveSource(action));
		}
		if (action.hasSelectionBehavior()) {
			card.setSelectionBehavior(action.getSelectionBehavior().toCode());
		}
		if (action.hasParticipant() && action.getParticipant().stream()
			.anyMatch(p -> "device".equals(p.getType().toCode()))) {
			// System Action
			resolveSystemActions(action, evaluationResults, card);
		}
		if (action.hasDynamicValue()) {
			resolveDynamicActions(action, evaluationResults, patientId, card);
		}
		if (action.hasReason()) {
			resolveOverrideReasons(action, card);
		}
		if (action.hasDefinition()) {
			Card.Suggestion suggestion = resolveSuggestions(action, patientId);
			card.addSuggestion(suggestion);
		}
	}

	private String deriveIndicator(PlanDefinition.RequestPriority priority) {
		switch (priority.toCode()) {
			case "routine": return "info";
			case "urgent":  return "warning";
			case "stat":    return "critical";
			default:
				throw new IllegalArgumentException("Invalid priority code: " + priority.toCode());
		}
	}

	public AtomicBoolean resolveCondition(
		PlanDefinition.PlanDefinitionActionComponent action,
		Parameters evaluationResults,
		String patientId
	) {
		AtomicBoolean conditionMet = new AtomicBoolean(false);
		if (!action.hasCondition()) {
			// If no conditions, assume true
			return new AtomicBoolean(true);
		}

		for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
			if (condition.hasExpression() && condition.getExpression().hasLanguage() && condition.getExpression().hasExpression()) {
				Type conditionResult = evaluateConditionExpression(condition, evaluationResults, patientId);
				if (conditionResult != null) {
					conditionMet.set(conditionResult.isPrimitive() &&
						Boolean.parseBoolean(conditionResult.primitiveValue()));
				}
			}
		}
		return conditionMet;
	}

	private Type evaluateConditionExpression(
		PlanDefinition.PlanDefinitionActionConditionComponent condition,
		Parameters evaluationResults,
		String patientId
	) {
		String lang = condition.getExpression().getLanguage();
		String expressionText = condition.getExpression().getExpression();
		if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
			return evaluationResults.getParameter(expressionText).getValue();
		} else if ("text/cql".equals(lang)) {
			return cqlExecutor.getExpressionExecution(cqlExecution, patientId, expressionText)
				.getParameterValue("return");
		}
		// Default => false
		return new BooleanType(false);
	}

	public Card.Source resolveSource(PlanDefinition.PlanDefinitionActionComponent action) {
		Card.Source source = new Card.Source();
		RelatedArtifact doc = action.getDocumentationFirstRep();
		if (doc.hasDisplay()) {
			source.setLabel(doc.getDisplay());
		}
		if (doc.hasUrl()) {
			source.setUri(doc.getUrl());
		}
		return source;
	}

	public Card.Suggestion resolveSuggestions(PlanDefinition.PlanDefinitionActionComponent action, String patientId) {
		Card.Suggestion suggestion = new Card.Suggestion();
		Card.Suggestion.Action suggAction = new Card.Suggestion.Action();
		suggAction.fhirContext = getFhirContext();

		if (action.hasPrefix()) {
			suggestion.setLabel(action.getPrefix());
		}
		if (action.hasPrecheckBehavior()) {
			boolean isRecommended = action.getPrecheckBehavior().equals(PlanDefinition.ActionPrecheckBehavior.YES);
			suggestion.setIsRecommended(isRecommended);
		}

		boolean hasAction = false;
		if (action.hasDescription()) {
			suggAction.setDescription(action.getDescription());
			hasAction = true;
		}
		if (action.hasType() && action.getType().hasCoding()) {
			String actionCode = action.getType().getCodingFirstRep().getCode();
			// Skip "fire-event" in your logic
			if (actionCode != null && !"fire-event".equals(actionCode)) {
				suggAction.setType(actionCode);
				hasAction = true;
			}
		}

		if (action.hasDefinitionCanonicalType() &&
			action.getDefinitionCanonicalType().getValue().contains("ActivityDefinition")) {
			suggAction.setType("create");
			IdType definitionId = new IdType(
				Canonicals.getResourceType(action.getDefinitionCanonicalType().getValue()),
				Canonicals.getIdPart(action.getDefinitionCanonicalType().getValue())
			);
			String updatedPatientId = patientId.startsWith("Patient/") ? patientId : "Patient/" + patientId;
			IBaseResource resource = applyEvaluator.apply(
				definitionId, updatedPatientId, null, updatedPatientId,
				null, null, null, null, null, null, null, null, null, null, null, null,
				requestDetails
			);
			suggAction.setResource(resource);
			hasAction = true;

			// Example hacky behavior for ServiceRequest
			if (resource instanceof ServiceRequest) {
				suggAction.setDescription("Service Request for Urine Drug Screening");
				((ServiceRequest) resource).setIntent(ServiceRequest.ServiceRequestIntent.PROPOSAL);
				var category = Collections.singletonList(
					new CodeableConcept().addCoding(
						new Coding()
							.setSystem("http://terminology.hl7.org/CodeSystem/medicationrequest-category")
							.setCode("outpatient")
							.setDisplay("Outpatient")
					)
				);
				((ServiceRequest) resource).setCategory(category);
			}
		}

		if (hasAction) {
			suggestion.setActions(Collections.singletonList(suggAction));
		}
		return suggestion;
	}

	public void resolveDynamicActions(
		PlanDefinition.PlanDefinitionActionComponent action,
		Parameters evaluationResults,
		String patientId,
		Card card
	) {
		for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dv : action.getDynamicValue()) {
			if (dv.hasPath() && dv.hasExpression() && dv.getExpression().hasLanguage() && dv.getExpression().hasExpression()) {
				IBase dynamicValueResult = evaluateDynamicValue(dv, evaluationResults, patientId);
				if (dynamicValueResult == null) {
					continue;
				}
				applyDynamicValueToCard(dv, card, dynamicValueResult);
			}
		}
	}

	private IBase evaluateDynamicValue(
		PlanDefinition.PlanDefinitionActionDynamicValueComponent dv,
		Parameters evaluationResults,
		String patientId
	) {
		String lang = dv.getExpression().getLanguage();
		String expressionText = dv.getExpression().getExpression();
		if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
			return evaluationResults.getParameter(expressionText).getValue();
		} else {
			return cqlExecutor.getExpressionExecution(cqlExecution, patientId, expressionText)
				.getParameter("return")
				.getValue();
		}
	}

	private void applyDynamicValueToCard(
		PlanDefinition.PlanDefinitionActionDynamicValueComponent dv,
		Card card,
		IBase dynamicValueResult
	) {
		String path = dv.getPath();
		String resultString = dynamicValueResult.toString();

		if (path.endsWith("title")) {
			card.setSummary(resultString);
		} else if (path.endsWith("description")) {
			card.setDetail(resultString);
			if (card.getSuggestions() != null && !card.getSuggestions().isEmpty()) {
				List<Card.Suggestion.Action> actions = card.getSuggestions().get(0).getActions();
				if (actions != null && !actions.isEmpty()) {
					actions.get(0).setDescription(resultString);
				}
			}
		}
		// else set modelResolver
		else if (card.getSuggestions() != null && !card.getSuggestions().isEmpty()) {
			var firstSuggestion = card.getSuggestions().get(0);
			List<Card.Suggestion.Action> actions = firstSuggestion.getActions();
			if (actions != null && !actions.isEmpty()) {
				IBase resource = actions.get(0).getResource();
				if (resource != null) {
					modelResolver.setValue(resource, path, dynamicValueResult);
				}
			}
		}
	}

	public void resolveOverrideReasons(
		PlanDefinition.PlanDefinitionActionComponent action,
		Card card
	) {
		if (action.hasReason()) {
			for (CodeableConcept reason : action.getReason()) {
				for (Coding coding : reason.getCoding()) {
					var overrideCoding = new Card.Coding();
					overrideCoding.setSystem(coding.getSystem());
					overrideCoding.setCode(coding.getCode());
					overrideCoding.setDisplay(coding.getDisplay());
					card.addOverrideReason(overrideCoding);
				}
			}
		}
	}

	/**
	 * Super hacky system action example. If an action indicates "update" for a device, we
	 * add an extension to the first draftOrder and set that as a system action.
	 */
	public void resolveSystemActions(
		PlanDefinition.PlanDefinitionActionComponent action,
		Parameters evaluationResults,
		Card card
	) {
		if (action.hasType() &&
			action.getType().getCoding().stream().anyMatch(c -> "update".equals(c.getCode()))) {

			List<Card.SystemAction> systemActions = new ArrayList<>();
			// Grab the first draft order from the context
			var draftOrders = CdsHooksUtil.getDraftOrders(orderSignContext.draftOrders);
			if (draftOrders.isEmpty()) {
				return;
			}
			MedicationRequest draftOrder = draftOrders.get(0);

			for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dv : action.getDynamicValue()) {
				IBase dvResult = evaluationResults.getParameter(dv.getExpression().getExpression()).getValue();
				if (dvResult instanceof Extension) {
					draftOrder.addExtension((Extension) dvResult);
					var sysAction = new Card.SystemAction();
					sysAction.setType("update");
					sysAction.setResource(draftOrder);
					systemActions.add(sysAction);
				}
			}
			card.setSystemActions(systemActions);
		}
	}

	private JsonObject getServices() {
		JsonObject services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	public DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (cqlProperties.getCqlRuntimeOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}

	protected ProviderConfiguration getProviderConfiguration() {
		return this.providerConfiguration;
	}
}
