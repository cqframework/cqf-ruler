package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.cr.common.HapiTerminologyProvider;
import ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
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
import org.opencds.cqf.ruler.cdshooks.r4.epic.util.MUSCR4CdsHooksRequestHelper;
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Configurable
public class MUSCTimeoutCdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final Logger logger = LoggerFactory.getLogger(MUSCTimeoutCdsHooksServlet.class);

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

	@Autowired
	public MUSCTimeoutCdsHooksServlet(
		DaoRegistry daoRegistry, AppProperties appProperties,
		CqlExecutionProvider cqlExecutionProvider, LibraryEvaluationProvider libraryEvaluationProvider,
		ActivityDefinitionOperationsProvider applyEvaluator,
		ModelResolver modelResolver, CdsServicesCache cdsServicesCache,
		CDSHooksTransactionInterceptor knowledgeArtifactCache, RestfulServer restfulServer,
		CdsHooksProperties cdsHooksProperties, IValidationSupport validationSupport) {
		this.daoRegistry = daoRegistry;
		this.appProperties = appProperties;
		this.cqlExecutionProvider = cqlExecutionProvider;
		this.libraryExecutionProvider = libraryEvaluationProvider;
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
		var contentType = request.getContentType();
		if (contentType == null || !contentType.startsWith("application/json")) {
			logging.logError("Unsupported content type: " + contentType);
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			return;
		}

		try {
			var raw = request.getReader().lines().collect(Collectors.joining());
			logging.logInfo(raw);
			var mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
			var hookReq = mapper.readValue(raw, CdsHooksRequest.class);

			var requestTimeoutMs = (int) cdsHooksProperties.getRequestTimeoutMs();
			// Use a slightly smaller prefetch timeout to leave time for CQL evaluation and card building
			var prefetchTimeoutMs = requestTimeoutMs > 500 ? requestTimeoutMs - 500 : requestTimeoutMs;

			var serviceId = request.getPathInfo().replace("/", "");

			logging.logInfo("MUSC CDS Hooks order-sign request received "
				+ "(hookInstance=" + hookReq.hookInstance
				+ ", serviceId=" + serviceId
				+ ", timeoutMs=" + requestTimeoutMs
				+ ", prefetchTimeoutMs=" + prefetchTimeoutMs + ")");

			var requestHelper = new MUSCR4CdsHooksRequestHelper(
				hookReq, serviceId, appProperties.getServer_address(), logging, prefetchTimeoutMs);
			var json = computeOrderSignPayloadWithTimeout(requestHelper, mapper, logging);
			writeJson(response, json);
		} catch (Exception e) {
			logging.logError(e.getMessage(), e);
		}
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
		} catch (IOException ioe) {
			logger.error("Error writing CDS Hooks JSON response", ioe);
		}
	}

	/**
	 * Runs computeOrderSignPayload with a hard timeout.
	 * If the timeout elapses we:
	 *   • cancel the task
	 *   • write a structured log
	 *   • return an empty set of cards so the hook stays responsive.
	 */
	private String computeOrderSignPayloadWithTimeout(
		MUSCR4CdsHooksRequestHelper helper,
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
				+ "(patient="  + helper.getMrn()
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
		MUSCR4CdsHooksRequestHelper helper, ObjectMapper mapper,
		EpicLogging logging) {
		var requestStart = System.currentTimeMillis();
		long planDefMs = -1;
		long prefetchMs = -1;
		long cqlMs = -1;
		long cardBuildMs = -1;
		long serializeMs = -1;

		// Prepare Library evaluation
		configureLibraryAndTerminologyProviders();

		// Prepare evaluation and card building
		PlanDefinition planDefinition;
		var planDefStart = System.currentTimeMillis();
		try {
			planDefinition = read(Ids.newId(PlanDefinition.class, helper.getServiceId()));
		} catch (ResourceNotFoundException e) {
			planDefMs = System.currentTimeMillis() - planDefStart;
			logging.logInfo("Perf milestone=planDefinitionResolved "
				+ "(hookInstance=" + helper.getRequest().hookInstance
				+ ", serviceId=" + helper.getServiceId()
				+ ", patient=" + helper.getMrn()
				+ ", durationMs=" + planDefMs + ")");
			throw e;
		}
		planDefMs = System.currentTimeMillis() - planDefStart;
		logging.logInfo("Perf milestone=planDefinitionResolved "
			+ "(hookInstance=" + helper.getRequest().hookInstance
			+ ", serviceId=" + helper.getServiceId()
			+ ", patient=" + helper.getMrn()
			+ ", durationMs=" + planDefMs + ")");

		if (!planDefinition.hasLibrary()) {
			logging.logError(String.format(
				"PlanDefinition for service %s does not specify a primary library", helper.getServiceId()));
			throw new IllegalArgumentException(String.format(
				"PlanDefinition for service %s does not specify a primary library", helper.getServiceId()));
		}
		IdType primaryLibraryId = Ids.newId(Library.class, Canonicals.getIdPart(planDefinition.getLibrary().get(0)));

		var prefetchStart = System.currentTimeMillis();
		var prefetchBundle = helper.getPrefetchBundle();
		prefetchMs = System.currentTimeMillis() - prefetchStart;
		logging.logInfo("Perf milestone=prefetchResolved "
			+ "(hookInstance=" + helper.getRequest().hookInstance
			+ ", serviceId=" + helper.getServiceId()
			+ ", patient=" + helper.getMrn()
			+ ", durationMs=" + prefetchMs + ")");

		var cqlExecutionHandler = new CqlExecutionHandler(
			r4CqlExecution, libraryExecutionProvider, cqlExecutionProvider,
			primaryLibraryId, helper.getDraftOrdersParameters(),
			helper.useServerData(), prefetchBundle);
		var expressions = CdsHooksUtil.getExpressions(planDefinition);

		var cqlStart = System.currentTimeMillis();
		var evaluationResults = cqlExecutionHandler.evaluateLibrary(
			helper.getPatientId(), expressions, null);
		cqlMs = System.currentTimeMillis() - cqlStart;
		var cqlDurationMs = cqlMs;
		logging.logInfo("CQL execution completed "
			+ "(hookInstance=" + helper.getRequest().hookInstance
			+ ", serviceId=" + helper.getServiceId()
			+ ", patient=" + helper.getMrn()
			+ ", durationMs=" + cqlDurationMs + ")");

		// TODO: Log decision provenance (rationale extension -> expression reference(s))
		// 	e.g. UDS Recommendation, No UDS Recommendation, Possible Unexpected Results

		// Build cards
		var cardBuildStart = System.currentTimeMillis();
		var cardBuilder = new CardBuilder(
			helper.getPatientId(), evaluationResults, planDefinition,
			applyEvaluator, requestDetails, modelResolver, cqlExecutionHandler);
		var cards = cardBuilder.buildCards();
		cardBuildMs = System.currentTimeMillis() - cardBuildStart;
		logging.logInfo("Perf milestone=buildCards "
			+ "(hookInstance=" + helper.getRequest().hookInstance
			+ ", serviceId=" + helper.getServiceId()
			+ ", patient=" + helper.getMrn()
			+ ", durationMs=" + cardBuildMs + ")");

		var result = new Cards();
		result.cards = cards.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (result.cards.isEmpty()) {
			logging.logNoGuidance(helper.getMrn(), helper.getRequest().hookInstance);
			var totalMs = System.currentTimeMillis() - requestStart;
			logging.logNoGuidance(String.format(
				"No guidance performance summary (mrn=%s, hookInstance=%s, serviceId=%s, patient=%s): planDefMs=%d, prefetchMs=%d, cqlMs=%d, cardBuildMs=%d, serializeMs=%d, totalMs=%d",
				helper.getMrn(), helper.getRequest().hookInstance, helper.getServiceId(), helper.getMrn(),
				planDefMs, prefetchMs, cqlMs, cardBuildMs, serializeMs, totalMs));
		}

		// Serialize cards into response
		var serializeStart = System.currentTimeMillis();
		String jsonResponse = null;
		try {
			jsonResponse = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
				.toJson(JsonParser.parseString(mapper.writeValueAsString(result)));
			logging.logInfo(jsonResponse);
			serializeMs = System.currentTimeMillis() - serializeStart;
			logging.logInfo("Perf milestone=serializeResponse "
				+ "(hookInstance=" + helper.getRequest().hookInstance
				+ ", serviceId=" + helper.getServiceId()
				+ ", patient=" + helper.getMrn()
				+ ", durationMs=" + serializeMs + ")");
		} catch (JsonProcessingException | JsonSyntaxException jpe) {
			serializeMs = System.currentTimeMillis() - serializeStart;
			logging.logError("Error serializing CDS Hooks response: ", jpe);
		}
		var totalMs = System.currentTimeMillis() - requestStart;
		logging.logInfo("Perf milestone=total "
			+ "(hookInstance=" + helper.getRequest().hookInstance
			+ ", serviceId=" + helper.getServiceId()
			+ ", patient=" + helper.getMrn()
			+ ", durationMs=" + totalMs + ")");
		return jsonResponse;
	}

	private void configureLibraryAndTerminologyProviders() {
		var libraries = knowledgeArtifactCache.getLibraryCache();
		this.libraryExecutionProvider.setLibraryLoader(new InMemoryLibraryLoader(libraries.values()));

		var valueSets = knowledgeArtifactCache.getValueSetCache();
		var terminologyProvider = new HapiTerminologyProvider(validationSupport, valueSets, requestDetails);
		this.libraryExecutionProvider.setTerminologyProvider(terminologyProvider);
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
