package org.opencds.cqf.ruler.cdshooks.r4;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.exception.DataProviderException;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.evaluation.EvaluationContext;
import org.opencds.cqf.ruler.cdshooks.evaluation.R4EvaluationContext;
import org.opencds.cqf.ruler.cdshooks.hooks.Hook;
import org.opencds.cqf.ruler.cdshooks.hooks.HookFactory;
import org.opencds.cqf.ruler.cdshooks.hooks.R4HookEvaluator;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.request.JsonHelper;
import org.opencds.cqf.ruler.cdshooks.request.Request;
import org.opencds.cqf.ruler.cdshooks.response.CdsCard;
import org.opencds.cqf.ruler.cql.CqlProperties;
import org.opencds.cqf.ruler.cql.JpaDataProviderFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.external.AppProperties;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class CdsHooksServlet extends HttpServlet implements DaoRegistryUser {
	private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

	private static final long serialVersionUID = 1L;

	@Autowired
	private CqlProperties cqlProperties;
	@Autowired
	private DaoRegistry daoRegistry;
	@Autowired
	private AppProperties myAppProperties;
	@Autowired
	private LibraryLoaderFactory libraryLoaderFactory;
	@Autowired
	private JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory;
	@Autowired
	private ProviderConfiguration providerConfiguration;
	@Autowired
	private JpaDataProviderFactory fhirRetrieveProviderFactory;
	@Autowired
	JpaTerminologyProviderFactory myJpaTerminologyProviderFactory;
	@Autowired
	private ModelResolver modelResolver;
	@Autowired
	CdsServicesCache cdsServicesCache;

	protected ProviderConfiguration getProviderConfiguration() {
		return this.providerConfiguration;
	}

	// CORS Pre-flight
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setAccessControlHeaders(resp);

		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("X-Content-Type-Options", "nosniff");

		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		logger.info(request.getRequestURI());

		if (!request.getRequestURL().toString().endsWith("/cds-services")
				&& !request.getRequestURL().toString().endsWith("/cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests.");
		}

		this.setAccessControlHeaders(response);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.info(request.getRequestURI());

		try {
			// validate that we are dealing with JSON
			if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
				throw new ServletException(String.format("Invalid content type %s. Please use application/json.",
						request.getContentType()));
			}

			String baseUrl = this.myAppProperties.getServer_address();
			String service = request.getPathInfo().replace("/", "");

			JsonParser parser = new JsonParser();
			Request cdsHooksRequest = new Request(service, parser.parse(request.getReader()).getAsJsonObject(),
					JsonHelper.getObjectRequired(getService(service), "prefetch"));

			logger.info(cdsHooksRequest.getRequestJson().toString());

			Hook hook = HookFactory.createHook(cdsHooksRequest);

			String hookName = hook.getRequest().getHook();
			logger.info("cds-hooks hook: {}", hookName);
			logger.info("cds-hooks hook instance: {}", hook.getRequest().getHookInstance());
			logger.info("cds-hooks maxCodesPerQuery: {}", this.getProviderConfiguration().getMaxCodesPerQuery());
			logger.info("cds-hooks expandValueSets: {}", this.getProviderConfiguration().getExpandValueSets());
			logger.info("cds-hooks searchStyle: {}", this.getProviderConfiguration().getSearchStyle());
			logger.info("cds-hooks prefetch maxUriLength: {}", this.getProviderConfiguration().getMaxUriLength());
			logger.info("cds-hooks local server address: {}", baseUrl);
			logger.info("cds-hooks fhir server address: {}", hook.getRequest().getFhirServerUrl());
			logger.info("cds-hooks cql_logging_enabled: {}", this.getProviderConfiguration().getCqlLoggingEnabled());

			PlanDefinition planDefinition = read(Ids.newId(PlanDefinition.class, hook.getRequest().getServiceName()));
			AtomicBoolean planDefinitionHookMatchesRequestHook = new AtomicBoolean(false);

			planDefinition.getAction().forEach(action -> {
				action.getTrigger().forEach(trigger -> {
					if (hookName.equals(trigger.getName())) {
						planDefinitionHookMatchesRequestHook.set(true);
						return;
					}
				});
				if (planDefinitionHookMatchesRequestHook.get()) {
					return;
				}
			});
			if (!planDefinitionHookMatchesRequestHook.get()) {
				throw new ServletException("ERROR: Request hook does not match the service called.");
			}

			// No tenant information available, so create local system request
			RequestDetails requestDetails = new SystemRequestDetails();

			LibraryLoader libraryLoader = libraryLoaderFactory
					.create(Lists.newArrayList(jpaLibraryContentProviderFactory.create(requestDetails)));

			CanonicalType canonical = planDefinition.getLibrary().get(0);
			Library library = search(Library.class, Searches.byCanonical(canonical)).single();

			org.cqframework.cql.elm.execution.Library elm = libraryLoader.load(
					new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));

			Context context = new Context(elm);
			context.setDebugMap(this.getDebugMap());

			// provider case
			// No tenant information available for cds-hooks
			TerminologyProvider serverTerminologyProvider = myJpaTerminologyProviderFactory.create(requestDetails);
			context.registerTerminologyProvider(serverTerminologyProvider);
			context.registerLibraryLoader(libraryLoader);
			context.setContextValue("Patient", hook.getRequest().getContext().getPatientId().replace("Patient/", ""));
			context.setExpressionCaching(true);

			EvaluationContext<PlanDefinition> evaluationContext = new R4EvaluationContext(hook,
					FhirContext.forCached(FhirVersionEnum.R4).newRestfulGenericClient(baseUrl),
					context, elm,
					planDefinition, this.getProviderConfiguration(), this.modelResolver);

			this.setAccessControlHeaders(response);
			response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			R4HookEvaluator evaluator = new R4HookEvaluator(this.modelResolver);
			String jsonResponse = toJsonResponse(evaluator.evaluate(evaluationContext));
			logger.info(jsonResponse);
			response.getWriter().println(jsonResponse);
		} catch (BaseServerResponseException e) {
			this.setAccessControlHeaders(response);
			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
			response.getWriter().println("ERROR: Exception connecting to remote server.");
			this.printMessageAndCause(e, response);
			this.handleServerResponseException(e, response);
			this.printStackTrack(e, response);
			logger.error(e.toString());
		} catch (DataProviderException e) {
			this.setAccessControlHeaders(response);
			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
			response.getWriter().println("ERROR: Exception in DataProvider.");
			this.printMessageAndCause(e, response);
			if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
				this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
			}

			this.printStackTrack(e, response);
			logger.error(e.toString());
		} catch (CqlException e) {
			this.setAccessControlHeaders(response);
			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
			response.getWriter().println("ERROR: Exception in CQL Execution.");
			this.printMessageAndCause(e, response);
			if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
				this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
			}

			this.printStackTrack(e, response);
			logger.error(e.toString());
		} catch (Exception e) {
			logger.error(e.toString());
			throw new ServletException("ERROR: Exception in cds-hooks processing.", e);
		}
	}

	private void handleServerResponseException(BaseServerResponseException e, HttpServletResponse response)
			throws IOException {
		switch (e.getStatusCode()) {
			case 401:
			case 403:
				response.getWriter().println("Precondition Failed. Remote FHIR server returned: " + e.getStatusCode());
				response.getWriter().println(
						"Ensure that the fhirAuthorization token is set or that the remote server allows unauthenticated access.");
				response.setStatus(412);
				break;
			case 404:
				response.getWriter().println("Precondition Failed. Remote FHIR server returned: " + e.getStatusCode());
				response.getWriter().println("Ensure the resource exists on the remote server.");
				response.setStatus(412);
				break;
			default:
				response.getWriter().println("Unhandled Error in Remote FHIR server: " + e.getStatusCode());
		}
	}

	private void printMessageAndCause(Exception e, HttpServletResponse response) throws IOException {
		if (e.getMessage() != null) {
			response.getWriter().println(e.getMessage());
		}

		if (e.getCause() != null && e.getCause().getMessage() != null) {
			response.getWriter().println(e.getCause().getMessage());
		}
	}

	private void printStackTrack(Exception e, HttpServletResponse response) throws IOException {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		response.getWriter().println(exceptionAsString);
	}

	private JsonObject getService(String service) {
		JsonArray services = getServicesArray();
		List<String> ids = new ArrayList<>();
		for (JsonElement element : services) {
			if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
				ids.add(element.getAsJsonObject().get("id").getAsString());
				if (element.isJsonObject() && element.getAsJsonObject().get("id").getAsString().equals(service)) {
					return element.getAsJsonObject();
				}
			}
		}
		throw new InvalidRequestException(
				"Cannot resolve service: " + service + "\nAvailable services: " + ids.toString());
	}

	private JsonArray getServicesArray() {
		return this.cdsServicesCache.getCdsServiceCache().get();
	}

	private JsonObject getServices() {
		JsonObject services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	private String toJsonResponse(List<CdsCard> cards) {
		JsonObject ret = new JsonObject();
		JsonArray cardArray = new JsonArray();

		for (CdsCard card : cards) {
			cardArray.add(card.toJson());
		}

		ret.add("cards", cardArray);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(ret);
	}

	private void setAccessControlHeaders(HttpServletResponse resp) {
		if (this.myAppProperties.getCors() != null) {
			if (this.myAppProperties.getCors().getAllow_Credentials()) {
				resp.setHeader("Access-Control-Allow-Origin",
						this.myAppProperties.getCors().getAllowed_origin().stream().findFirst().get());
				resp.setHeader("Access-Control-Allow-Methods",
						String.join(", ", Arrays.asList("GET", "HEAD", "POST", "OPTIONS")));
				resp.setHeader("Access-Control-Allow-Headers", String.join(", ", Arrays.asList("x-fhir-starter", "Origin",
						"Accept", "X-Requested-With", "Content-Type", "Authorization", "Cache-Control")));
				resp.setHeader("Access-Control-Expose-Headers",
						String.join(", ", Arrays.asList("Location", "Content-Location")));
				resp.setHeader("Access-Control-Max-Age", "86400");
			}
		}
	}

	public DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (cqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}
}
