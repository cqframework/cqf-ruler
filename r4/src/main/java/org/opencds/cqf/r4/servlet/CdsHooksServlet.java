package org.opencds.cqf.r4.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.entity.ContentType;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cds.discovery.DiscoveryResolutionR4;
import org.opencds.cqf.cds.evaluation.EvaluationContext;
import org.opencds.cqf.cds.evaluation.R4EvaluationContext;
import org.opencds.cqf.cds.hooks.Hook;
import org.opencds.cqf.cds.hooks.HookFactory;
import org.opencds.cqf.cds.hooks.R4HookEvaluator;
import org.opencds.cqf.cds.providers.ProviderConfiguration;
import org.opencds.cqf.cds.request.JsonHelper;
import org.opencds.cqf.cds.request.Request;
import org.opencds.cqf.cds.response.CdsCard;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.common.exceptions.InvalidRequestException;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.common.retrieve.JpaFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.DataProviderException;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.r4.helpers.LibraryHelper;
import org.opencds.cqf.r4.providers.JpaTerminologyProvider;
import org.opencds.cqf.r4.providers.PlanDefinitionApplyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

@WebServlet(name = "cds-services")
public class CdsHooksServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private FhirVersionEnum version = FhirVersionEnum.R4;
    private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

    private org.opencds.cqf.r4.providers.PlanDefinitionApplyProvider planDefinitionProvider;

    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResolutionProvider;

    private JpaFhirRetrieveProvider fhirRetrieveProvider;

    private org.opencds.cqf.r4.providers.JpaTerminologyProvider jpaTerminologyProvider;

    private ProviderConfiguration providerConfiguration;

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
        .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        this.providerConfiguration = appCtx.getBean(ProviderConfiguration.class);
        this.planDefinitionProvider = appCtx.getBean(PlanDefinitionApplyProvider.class);
        this.libraryResolutionProvider = (LibraryResolutionProvider<org.hl7.fhir.r4.model.Library>)appCtx.getBean(LibraryResolutionProvider.class);
        this.fhirRetrieveProvider = appCtx.getBean(JpaFhirRetrieveProvider.class);
        this.jpaTerminologyProvider = appCtx.getBean(JpaTerminologyProvider.class);
    }

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
        if (!request.getRequestURL().toString().endsWith("cds-services")) {
            logger.error(request.getRequestURI());
            throw new ServletException("This servlet is not configured to handle GET requests.");
        }

        this.setAccessControlHeaders(response);
        response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info(request.getRequestURI());

        try {
            // validate that we are dealing with JSON
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                throw new ServletException(String.format("Invalid content type %s. Please use application/json.",
                        request.getContentType()));
            }

            
            String baseUrl = HapiProperties.getServerAddress();
            String service = request.getPathInfo().replace("/", "");

            JsonParser parser = new JsonParser();
            Request cdsHooksRequest = new Request(service, parser.parse(request.getReader()).getAsJsonObject(),
                    JsonHelper.getObjectRequired(getService(service), "prefetch"));

            logger.info(cdsHooksRequest.getRequestJson().toString());

            Hook hook = HookFactory.createHook(cdsHooksRequest);

            logger.info("cds-hooks hook: " + hook.getRequest().getHook());
            logger.info("cds-hooks hook instance: " + hook.getRequest().getHookInstance());
            logger.info("cds-hooks maxCodesPerQuery: " + this.getProviderConfiguration().getMaxCodesPerQuery());
            logger.info("cds-hooks expandValueSets: " + this.getProviderConfiguration().getExpandValueSets());
            logger.info("cds-hooks searchStyle: " + this.getProviderConfiguration().getSearchStyle());
            logger.info("cds-hooks prefetch maxUriLength: " + this.getProviderConfiguration().getMaxUriLength());
            logger.info("cds-hooks local server address: " + baseUrl);
            logger.info("cds-hooks fhir server address: " + hook.getRequest().getFhirServerUrl());

            PlanDefinition planDefinition = planDefinitionProvider.getDao()
                    .read(new IdType(hook.getRequest().getServiceName()));
            LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(libraryResolutionProvider);
            Library library = LibraryHelper.resolvePrimaryLibrary(planDefinition, libraryLoader,
                    libraryResolutionProvider);

            R4FhirModelResolver resolver = new R4FhirModelResolver();
            CompositeDataProvider provider = new CompositeDataProvider(resolver, fhirRetrieveProvider);

            Context context = new Context(library);

            DebugMap debugMap = new DebugMap();
            debugMap.setIsLoggingEnabled(true);
            context.setDebugMap(debugMap);

            context.registerDataProvider("http://hl7.org/fhir", provider); // TODO make sure tooling handles remote
                                                                           // provider case
            context.registerTerminologyProvider(jpaTerminologyProvider);
            context.registerLibraryLoader(libraryLoader);
            context.setContextValue("Patient", hook.getRequest().getContext().getPatientId().replace("Patient/", ""));
            context.setExpressionCaching(true);

            EvaluationContext<PlanDefinition> evaluationContext = new R4EvaluationContext(hook, version,
                    FhirContext.forR4().newRestfulGenericClient(baseUrl), jpaTerminologyProvider, context, library,
                    planDefinition, this.getProviderConfiguration());

            this.setAccessControlHeaders(response);

            response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

            R4HookEvaluator evaluator = new R4HookEvaluator();

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
        }
        catch (CqlException e) {
            this.setAccessControlHeaders(response);
            response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
            response.getWriter().println("ERROR: Exception in CQL Execution.");
            this.printMessageAndCause(e, response);
            if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
                this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
            }

            this.printStackTrack(e, response);
            logger.error(e.toString());
        }
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

    private JsonObject getService(String service) {
        JsonArray services = getServices().get("services").getAsJsonArray();
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

    private JsonObject getServices() {
        DiscoveryResolutionR4 discoveryResolutionR4 = new DiscoveryResolutionR4(
                FhirContext.forR4().newRestfulGenericClient(HapiProperties.getServerAddress()));
        discoveryResolutionR4.setMaxUriLength(this.getProviderConfiguration().getMaxUriLength());
        return discoveryResolutionR4.resolve()
                        .getAsJson();
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
        if (HapiProperties.getCorsEnabled()) {
            resp.setHeader("Access-Control-Allow-Origin", HapiProperties.getCorsAllowedOrigin());
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