package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.common.retrieve.JpaFhirRetrieveProvider;
import ca.uhn.fhir.cql.r4.helper.LibraryHelper;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.cds.providers.ProviderConfiguration;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.DataProviderException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.plugin.cdshooks.CdsHooksProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.opencds.cqf.ruler.plugin.security.SecurityProperties;
import org.opencds.cqf.cds.discovery.DiscoveryResolutionR4;
import org.opencds.cqf.ruler.plugin.utility.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@WebServlet(name = "cds-services")
public class CdsHooksServlet extends HttpServlet implements ClientUtilities {

	@Autowired
	CdsHooksProperties cdsHooksProperties;
	private FhirContext ourCtx = FhirContext.forR4Cached();

	private static final long serialVersionUID = 1L;
	private FhirVersionEnum version = FhirVersionEnum.R4;
	private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

	// TODO: Use PlanDefinitionApply from Plugin
	//private org.opencds.cqf.r4.providers.PlanDefinitionApplyProvider planDefinitionProvider;
	private IFhirResourceDao<PlanDefinition> myPlanDefinitionDao;

	private LibraryResolutionProvider<Library> libraryResolutionProvider;

	private JpaFhirRetrieveProvider fhirRetrieveProvider;

	private TerminologyProvider serverTerminologyProvider;

	// TODO: Pull this into Plugin from Component
	//private ProviderConfiguration providerConfiguration;

	private ModelResolver modelResolver;

	private LibraryHelper libraryHelper;

	private SecurityProperties securityProperties = new SecurityProperties();

	@Autowired
	private AppProperties appProperties;

	@Override
	public void init() {
		// System level providers
		ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

		// TODO:
//		this.providerConfiguration = appCtx.getBean(ProviderConfiguration.class);
//		this.planDefinitionProvider = appCtx.getBean(PlanDefinitionApplyProvider.class);
//		this.libraryResolutionProvider = (LibraryResolutionProvider<org.hl7.fhir.r4.model.Library>)appCtx.getBean(LibraryResolutionProvider.class);
//		this.fhirRetrieveProvider = appCtx.getBean(JpaFhirRetrieveProvider.class);
//		this.serverTerminologyProvider = appCtx.getBean(TerminologyProvider.class);
//		this.modelResolver = appCtx.getBean("r4ModelResolver", ModelResolver.class);
//		this.libraryHelper = appCtx.getBean(LibraryHelper.class);
	}

//	protected ProviderConfiguration getProviderConfiguration() {
//		return this.providerConfiguration;
//	}

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
		if (!request.getRequestURL().toString().endsWith("cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests.");
		}

		// TODO:
		this.setAccessControlHeaders(response);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		logger.info(request.getRequestURI());

		try {
			// validate that we are dealing with JSON
			if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
				throw new ServletException(String.format("Invalid content type %s. Please use application/json.",
					request.getContentType()));
			}
		}

			// TODO:
//			String baseUrl = HapiProperties.getServerAddress();
//			String service = request.getPathInfo().replace("/", "");
//
//			JsonParser parser = new JsonParser();
//			Request cdsHooksRequest = new Request(service, parser.parse(request.getReader()).getAsJsonObject(),
//				JsonHelper.getObjectRequired(getService(service), "prefetch"));
//
//			logger.info(cdsHooksRequest.getRequestJson().toString());
//
//			Hook hook = HookFactory.createHook(cdsHooksRequest);
//
//			String hookName = hook.getRequest().getHook();
//			logger.info("cds-hooks hook: " + hookName);
//			logger.info("cds-hooks hook instance: " + hook.getRequest().getHookInstance());
//			logger.info("cds-hooks maxCodesPerQuery: " + this.getProviderConfiguration().getMaxCodesPerQuery());
//			logger.info("cds-hooks expandValueSets: " + this.getProviderConfiguration().getExpandValueSets());
//			logger.info("cds-hooks searchStyle: " + this.getProviderConfiguration().getSearchStyle());
//			logger.info("cds-hooks prefetch maxUriLength: " + this.getProviderConfiguration().getMaxUriLength());
//			logger.info("cds-hooks local server address: " + baseUrl);
//			logger.info("cds-hooks fhir server address: " + hook.getRequest().getFhirServerUrl());
//
//			PlanDefinition planDefinition = planDefinitionProvider.getDao()
//				.read(new IdType(hook.getRequest().getServiceName()));
//			AtomicBoolean planDefinitionHookMatchesRequestHook = new AtomicBoolean(false);
//
//			planDefinition.getAction().forEach(action -> {
//				action.getTrigger().forEach(trigger -> {
//					if(hookName.equals(trigger.getName())){
//						planDefinitionHookMatchesRequestHook.set(true);
//						return;
//					}
//				});
//				if(planDefinitionHookMatchesRequestHook.get()){
//					return;
//				}
//			});
//			if(!planDefinitionHookMatchesRequestHook.get()){
//				throw new ServletException("ERROR: Request hook does not match the service called.");
//			}
//			LibraryLoader libraryLoader = this.libraryHelper.createLibraryLoader(libraryResolutionProvider);
//			org.cqframework.cql.elm.execution.Library library = this.libraryHelper.resolvePrimaryLibrary(planDefinition, libraryLoader,
//				libraryResolutionProvider);
//
//			CompositeDataProvider provider = new CompositeDataProvider(this.modelResolver, fhirRetrieveProvider);
//
//			Context context = new Context(library);
//
//			context.setDebugMap(LoggingHelper.getDebugMap());
//
//			context.registerDataProvider("http://hl7.org/fhir", provider); // TODO make sure tooling handles remote
//			// provider case
//			context.registerTerminologyProvider(serverTerminologyProvider);
//			context.registerLibraryLoader(libraryLoader);
//			context.setContextValue("Patient", hook.getRequest().getContext().getPatientId().replace("Patient/", ""));
//			context.setExpressionCaching(true);
//
//			EvaluationContext<PlanDefinition> evaluationContext = new R4EvaluationContext(hook, version,
//				FhirContext.forCached(FhirVersionEnum.R4).newRestfulGenericClient(baseUrl), serverTerminologyProvider, context, library,
//				planDefinition, this.getProviderConfiguration(), this.modelResolver);
//
//			this.setAccessControlHeaders(response);
//
//			response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
//
//			R4HookEvaluator evaluator = new R4HookEvaluator(this.modelResolver);
//
//			String jsonResponse = toJsonResponse(evaluator.evaluate(evaluationContext));
//
//			logger.info(jsonResponse);
//
//			response.getWriter().println(jsonResponse);
//		} catch (BaseServerResponseException e) {
//			this.setAccessControlHeaders(response);
//			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
//			response.getWriter().println("ERROR: Exception connecting to remote server.");
//			this.printMessageAndCause(e, response);
//			this.handleServerResponseException(e, response);
//			this.printStackTrack(e, response);
//			logger.error(e.toString());
//		} catch (DataProviderException e) {
//			this.setAccessControlHeaders(response);
//			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
//			response.getWriter().println("ERROR: Exception in DataProvider.");
//			this.printMessageAndCause(e, response);
//			if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
//				this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
//			}
//
//			this.printStackTrack(e, response);
//			logger.error(e.toString());
//		}
//		catch (CqlException e) {
//			this.setAccessControlHeaders(response);
//			response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
//			response.getWriter().println("ERROR: Exception in CQL Execution.");
//			this.printMessageAndCause(e, response);
//			if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
//				this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
//			}
//
//			this.printStackTrack(e, response);
//			logger.error(e.toString());
//		}
		catch (Exception e) {
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

//	private JsonObject getService(String service) {
//		JsonArray services = getServices().get("services").getAsJsonArray();
//		List<String> ids = new ArrayList<>();
//		for (JsonElement element : services) {
//			if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
//				ids.add(element.getAsJsonObject().get("id").getAsString());
//				if (element.isJsonObject() && element.getAsJsonObject().get("id").getAsString().equals(service)) {
//					return element.getAsJsonObject();
//				}
//			}
//		}
//		throw new InvalidRequestException(
//			"Cannot resolve service: " + service + "\nAvailable services: " + ids.toString());
//	}

	private JsonObject getServices() {
		// TODO: Gather Server Base
		DiscoveryResolutionR4 discoveryResolutionR4 = new DiscoveryResolutionR4(
			createClient(ourCtx, "http://localhost:8080/fhir"));
			//FhirContext.forCached(FhirVersionEnum.R4).newRestfulGenericClient("http://localhost:8080/fhir/"));
		discoveryResolutionR4.setMaxUriLength(5555);

			//setMaxUriLength(this.getProviderConfiguration().getMaxUriLength());
		return discoveryResolutionR4.resolve()
			.getAsJson();
	}

//	private String toJsonResponse(List<CdsCard> cards) {
//		JsonObject ret = new JsonObject();
//		JsonArray cardArray = new JsonArray();
//
//		for (CdsCard card : cards) {
//			cardArray.add(card.toJson());
//		}
//
//		ret.add("cards", cardArray);
//
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		return gson.toJson(ret);
//	}

	private void setAccessControlHeaders(HttpServletResponse resp) {
		// TODO: Fix AppProperties
//		if (appProperties.getCors().getAllow_Credentials()) {
//			resp.setHeader("Access-Control-Allow-Origin", appProperties.getCors().getAllowed_origin().stream().findFirst().get());
//			resp.setHeader("Access-Control-Allow-Methods",
//				String.join(", ", Arrays.asList("GET", "HEAD", "POST", "OPTIONS")));
//			resp.setHeader("Access-Control-Allow-Headers", String.join(", ", Arrays.asList("x-fhir-starter", "Origin",
//				"Accept", "X-Requested-With", "Content-Type", "Authorization", "Cache-Control")));
//			resp.setHeader("Access-Control-Expose-Headers",
//				String.join(", ", Arrays.asList("Location", "Content-Location")));
//			resp.setHeader("Access-Control-Max-Age", "86400");
//		}

		if (true) {
			resp.setHeader("Access-Control-Allow-Origin", "*");
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




