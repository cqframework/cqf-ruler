package org.opencds.cqf.ruler.cdshooks.r4;

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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CDSHooksTransactionInterceptor;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configurable
public class EpicCdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final Logger logger = LoggerFactory.getLogger(EpicCdsHooksServlet.class);

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

	@Autowired
	public EpicCdsHooksServlet(
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
		// Validate content type header
		var contentType = request.getContentType();
		if (contentType == null || !contentType.startsWith("application/json")) {
			logging.logError(String.format("Expected content type: application/json, found %s.", contentType));
			return;
		}

		// Deserialize request
		String cdsHooksRequestRaw;
		try {
			cdsHooksRequestRaw = request.getReader().lines().collect(Collectors.joining());
			logging.logInfo(cdsHooksRequestRaw);
		} catch (IOException ioe) {
			logging.logError("Error reading CDS Hooks Request: ", ioe);
			return;
		}
		var mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
		CdsHooksRequest cdsHooksRequest;
		try {
			cdsHooksRequest = mapper.readValue(cdsHooksRequestRaw, CdsHooksRequest.class);
		} catch (JsonProcessingException jpe) {
			logging.logError("Error deserializing CDS Hooks Request: ", jpe);
			return;
		}

		// Prepare Library evaluation
		configureLibraryAndTerminologyProviders();

		// Prepare evaluation and card building
		var cdsHooksRequestHandler = new CdsHooksRequestHandler(cdsHooksRequest);
		var serviceId = request.getPathInfo().replace("/", "");
		PlanDefinition planDefinition;
		try {
			planDefinition = read(Ids.newId(PlanDefinition.class, serviceId));
		} catch (ResourceNotFoundException e) {
			logging.logError(String.format("Could not resolve PlanDefinition/%s", serviceId), e);
			return;
		}
		if (!planDefinition.hasLibrary()) {
			logging.logError(String.format("PlanDefinition for service %s does not specify a primary library", serviceId));
			return;
		}
		IdType primaryLibraryId = Ids.newId(Library.class, Canonicals.getIdPart(planDefinition.getLibrary().get(0)));
		var patientId = cdsHooksRequestHandler.getPatientId();
		var cqlExecutionHandler = new CqlExecutionHandler(r4CqlExecution, libraryExecutionProvider, cqlExecutionProvider,
			primaryLibraryId, cdsHooksRequestHandler.getDraftOrdersParameters(),
			cdsHooksRequestHandler.useServerData(), cdsHooksRequestHandler.getPrefetchBundle(logging));
		var expressions = CdsHooksUtil.getExpressions(planDefinition);
		var evaluationResults = cqlExecutionHandler.evaluateLibrary(patientId, expressions,
			cdsHooksRequestHandler.remoteDataEndpoint);

		// Build cards
		CardBuilder cardBuilder = new CardBuilder(patientId, evaluationResults, planDefinition, applyEvaluator, requestDetails, modelResolver, cqlExecutionHandler);
		List<Card> cards = cardBuilder.buildCards();
		Cards result = new Cards();
		result.cards = cards.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (result.cards.isEmpty()) {
			logging.logNoGuidance(patientId, cdsHooksRequestHandler.request.hookInstance);
		}

		// Serialize cards into response
		try {
			String jsonResponse = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
				.toJson(JsonParser.parseString(mapper.writeValueAsString(result)));
			logging.logInfo(jsonResponse);
			setResponseHeaders(response);
			response.getWriter().println(jsonResponse);
		} catch (JsonProcessingException | JsonSyntaxException jpe) {
			logging.logError("Error serializing CDS Hooks response: ", jpe);
		} catch (IOException ioe) {
			logging.logError("Error writing CDS Hooks response: ", ioe);
		}

		logging.logRequestDuration(cdsHooksRequestHandler.request.hookInstance);
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

	private void configureLibraryAndTerminologyProviders() {
		var libraries = knowledgeArtifactCache.getLibraryCache();
		this.libraryExecutionProvider.setLibraryLoader(new InMemoryLibraryLoader(libraries.values()));

		var valuesets = knowledgeArtifactCache.getValueSetCache();
		var terminologyProvider = new HapiTerminologyProvider(validationSupport, valuesets, requestDetails);
		this.libraryExecutionProvider.setTerminologyProvider(terminologyProvider);
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}

	private class CdsHooksRequestHandler {
		private final CdsHooksRequest request;
		private final Endpoint remoteDataEndpoint;

		public CdsHooksRequestHandler(CdsHooksRequest request) {
			this.request = request;
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
				remoteDataEndpoint = null;
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
			return draftOrders == null ? null : CdsHooksUtil.getParameters(draftOrders);
		}

		public BooleanType useServerData() {
			return new BooleanType(remoteDataEndpoint == null);
		}

		public Bundle getPrefetchBundle(EpicLogging logging) {
			var data = CdsHooksUtil.getPrefetchResources(request);
			var draftOrders = getDraftOrders();
			// Use prefetch resources if provided otherwise use MCL
			if (data == null) {
				var configResolver = new ModuleConfigurationResolver(getFhirContext(),
					remoteDataEndpoint == null
						? new Endpoint().setAddress(appProperties.getServer_address()) : remoteDataEndpoint,
					request);
				data = configResolver.getPrefetchBundle();
				logging.logMclQueryPerformance(configResolver.getPerformanceMap());
				logging.logBundleResources(data);
			}

			if (draftOrders != null) {
				// Add non-request resources (e.g. referenced Medications) to bundle
				CdsHooksUtil.addNonRequestResourcesFromContextToDataBundle(draftOrders, data);
			}

			return data;
		}
	}
}
