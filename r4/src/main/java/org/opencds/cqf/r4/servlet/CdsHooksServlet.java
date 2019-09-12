package org.opencds.cqf.r4.servlet;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.alphora.evaluation.EvaluationContext;
import com.alphora.evaluation.R4EvaluationContext;
import com.alphora.hooks.Hook;
import com.alphora.hooks.HookFactory;
import com.alphora.hooks.R4HookEvaluator;
import com.alphora.providers.Discovery;
import com.alphora.providers.DiscoveryItem;
import com.alphora.request.JsonHelper;
import com.alphora.request.Request;
import com.alphora.response.CdsCard;
import com.google.gson.*;
import org.apache.http.entity.ContentType;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.config.HapiProperties;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.exceptions.InvalidRequestException;
import org.opencds.cqf.r4.helpers.LibraryHelper;
import org.opencds.cqf.r4.providers.FHIRPlanDefinitionResourceProvider;
import org.opencds.cqf.r4.providers.JpaDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "cds-services")
public class CdsHooksServlet extends HttpServlet
{
    static JpaDataProvider provider;
    private FhirVersionEnum version = FhirVersionEnum.R4;
    private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

    // CORS Pre-flight
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        setAccessControlHeaders(resp);

        resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        resp.setHeader("X-Content-Type-Options", "nosniff");
        
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        logger.info(request.getRequestURI());
        if (!request.getRequestURL().toString().endsWith("cds-services"))
        {
            logger.error(request.getRequestURI());
            throw new ServletException("This servlet is not configured to handle GET requests.");
        }

        this.setAccessControlHeaders(response);
        response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        logger.info(request.getRequestURI());

        try {
            // validate that we are dealing with JSON
            if (!request.getContentType().startsWith("application/json"))
            {
                throw new ServletException(
                        String.format(
                                "Invalid content type %s. Please use application/json.",
                                request.getContentType()
                        )
                );
            }

            String baseUrl =
                    request.getRequestURL().toString()
                            .replace(request.getPathInfo(), "").replace(request.getServletPath(), "") + "/baseR4";
            String service = request.getPathInfo().replace("/", "");

            JsonParser parser = new JsonParser();
            Request cdsHooksRequest =
                    new Request(
                            service,
                            parser.parse(request.getReader()).getAsJsonObject(),
                            JsonHelper.getObjectRequired(getService(service), "prefetch")
                    );

            logger.info(cdsHooksRequest.getRequestJson().toString());

            Hook hook = HookFactory.createHook(cdsHooksRequest);

            PlanDefinition planDefinition = (PlanDefinition) provider.resolveResourceProvider("PlanDefinition").getDao().read(new IdType(hook.getRequest().getServiceName()));
            LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader((org.opencds.cqf.r4.providers.LibraryResourceProvider) provider.resolveResourceProvider("Library"));
            Library library = LibraryHelper.resolvePrimaryLibrary(planDefinition, libraryLoader, (org.opencds.cqf.r4.providers.LibraryResourceProvider) provider.resolveResourceProvider("Library"));

            Context context = new Context(library);
            context.registerDataProvider("http://hl7.org/fhir", provider.setEndpoint(baseUrl)); // TODO make sure tooling handles remote provider case
            context.registerLibraryLoader(libraryLoader);
            context.setContextValue("Patient", hook.getRequest().getContext().getPatientId());
            context.setExpressionCaching(true);

            EvaluationContext evaluationContext = new R4EvaluationContext(hook, version, provider.setEndpoint(baseUrl), context, library, planDefinition);

            this.setAccessControlHeaders(response);

            response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

            R4HookEvaluator evaluator = new R4HookEvaluator();

            String jsonResponse = toJsonResponse(evaluator.evaluate(evaluationContext));

            logger.info(jsonResponse);

            response.getWriter().println(jsonResponse);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.setAccessControlHeaders(response);

            response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            response.getWriter().println(toJsonResponse(Collections.singletonList(CdsCard.errorCard(e))));
        }
    }

    private JsonObject getService(String service)
    {
        JsonArray services = getServices().get("services").getAsJsonArray();
        List<String> ids = new ArrayList<>();
        for (JsonElement element : services)
        {
            if (element.isJsonObject() && element.getAsJsonObject().has("id"))
            {
                ids.add(element.getAsJsonObject().get("id").getAsString());
                if (element.isJsonObject() && element.getAsJsonObject().get("id").getAsString().equals(service))
                {
                    return element.getAsJsonObject();
                }
            }
        }
        throw new InvalidRequestException("Cannot resolve service: " + service + "\nAvailable services: " + ids.toString());
    }

    private JsonObject getServices()
    {
        JsonObject responseJson = new JsonObject();
        JsonArray services = new JsonArray();

        FHIRPlanDefinitionResourceProvider provider = (FHIRPlanDefinitionResourceProvider) getProvider("PlanDefinition");
        for (Discovery<PlanDefinition> discovery : provider.getDiscoveries(version))
        {
            PlanDefinition planDefinition = discovery.getPlanDefinition();
            JsonObject service = new JsonObject();
            if (planDefinition != null)
            {
                if (planDefinition.hasAction())
                {
                    // TODO - this needs some work - too naive
                    if (planDefinition.getActionFirstRep().hasTrigger())
                    {
                        if (planDefinition.getActionFirstRep().getTriggerFirstRep().hasName())
                        {
                            service.addProperty("hook", planDefinition.getActionFirstRep().getTriggerFirstRep().getName());
                        }
                    }
                }
                if (planDefinition.hasName())
                {
                    service.addProperty("name", planDefinition.getName());
                }
                if (planDefinition.hasTitle())
                {
                    service.addProperty("title", planDefinition.getTitle());
                }
                if (planDefinition.hasDescription())
                {
                    service.addProperty("description", planDefinition.getDescription());
                }
                service.addProperty("id", planDefinition.getIdElement().getIdPart());

                if (!discovery.getItems().isEmpty())
                {
                    JsonObject prefetchContent = new JsonObject();
                    for (DiscoveryItem item : discovery.getItems())
                    {
                        if (item.getItemNo() == null) {
                            prefetchContent.addProperty("error", item.getUrl());
                        }
                        else {
                            prefetchContent.addProperty(item.getItemNo(), item.getUrl());
                        }
                    }
                    service.add("prefetch", prefetchContent);
                }
            }
            else
            {
                service.addProperty("Error", discovery.getItems().get(0).getUrl());
            }
            services.add(service);
        }

        responseJson.add("services", services);
        return responseJson;
    }

    private String toJsonResponse(List<CdsCard> cards)
    {
        JsonObject ret = new JsonObject();
        JsonArray cardArray = new JsonArray();

        for (CdsCard card : cards)
        {
            cardArray.add(card.toJson());
        }

        ret.add("cards", cardArray);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return  gson.toJson(ret);
    }


    private void setAccessControlHeaders(HttpServletResponse resp) {
        if (HapiProperties.getCorsEnabled())
        {
            resp.setHeader("Access-Control-Allow-Origin", HapiProperties.getCorsAllowedOrigin());
            resp.setHeader("Access-Control-Allow-Methods", String.join(", ", Arrays.asList("GET", "HEAD", "POST", "OPTIONS")));
            resp.setHeader("Access-Control-Allow-Headers", 
                String.join(", ", Arrays.asList(
                    "x-fhir-starter",
                    "Origin",
                    "Accept",
                    "X-Requested-With",
                    "Content-Type",
                    "Authorization",
                    "Cache-Control")));
            resp.setHeader("Access-Control-Expose-Headers", String.join(", ", Arrays.asList("Location", "Content-Location")));
            resp.setHeader("Access-Control-Max-Age", "86400");
        }
    }

    private IResourceProvider getProvider(String name)
    {
        for (IResourceProvider res : provider.getCollectionProviders())
        {
            if (res.getResourceType().getSimpleName().equals(name))
            {
                return res;
            }
        }

        throw new IllegalArgumentException("This should never happen!");
    }
}
