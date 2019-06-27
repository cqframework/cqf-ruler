package org.opencds.cqf.cdshooks.evaluation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cdshooks.hooks.Hook;
import org.opencds.cqf.cdshooks.request.JsonHelper;

import java.io.IOException;
import java.util.*;

public class EvaluationHelper {

    // This method is very forgiving. Accepts: JSON Object, JSON Array, or JSON String
    public static List<Object> resolveContextResources(JsonElement contextJson, FhirContext fhirContext) {
        List<Object> contextResources = new ArrayList<>();
        Gson gson = new GsonBuilder().create();
        // Bundle or single resource
        if (contextJson.isJsonObject()) {
            addResources(fhirContext.newJsonParser().parseResource(gson.toJson(contextJson)), fhirContext, contextResources);
        }
        // List of resources
        else if (contextJson.isJsonArray()) {
            for (JsonElement resource : contextJson.getAsJsonArray()) {
                addResources(fhirContext.newJsonParser().parseResource(gson.toJson(resource)), fhirContext, contextResources);
            }
        }
        // Bundle or single resource as String
        else if (contextJson.isJsonPrimitive() && contextJson.getAsJsonPrimitive().isString()) {
            addResources(fhirContext.newJsonParser().parseResource(contextJson.getAsString()), fhirContext, contextResources);
        }
        return contextResources;
    }

    public static void addResources(IBaseResource resource, FhirContext fhirContext, List<Object> contextResources) {
        if (isBundle(resource)) {
            contextResources.addAll(resolveBundle(resource, fhirContext));
        }
        else {
            contextResources.add(resource);
        }
    }

    public static boolean isBundle(IBaseResource bundleCandidate) {
        return bundleCandidate instanceof Bundle || bundleCandidate instanceof ca.uhn.fhir.model.dstu2.resource.Bundle;
    }

    public static List<Object> resolveBundle(IBaseResource bundle, FhirContext fhirContext) {
        List<Object> resources = new ArrayList<>();
        if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
            if (((Bundle) bundle).hasEntry()) {
                for (Bundle.BundleEntryComponent entry : ((Bundle) bundle).getEntry()) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
            }
        }
        else if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU2) {
            if (!((ca.uhn.fhir.model.dstu2.resource.Bundle) bundle).getEntry().isEmpty()) {
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry : ((ca.uhn.fhir.model.dstu2.resource.Bundle) bundle).getEntry()) {
                    if (entry.getResource() != null) {
                        resources.add(entry.getResource());
                    }
                }
            }
        }
        return resources;
    }

    public static List<Object> resolvePrefetchResources(Hook hook, FhirContext fhirContext, IGenericClient client) throws IOException {
        List<Object> prefetchResources = new ArrayList<>();
        List<String> prefetchElementsToFetch = new ArrayList<>();
        Gson gson = new GsonBuilder().create();

        if (!hook.getRequest().getPrefetch().getResourceMap().isEmpty()) {
            for (Map.Entry<String, JsonElement> entry : hook.getRequest().getPrefetch().getDiscoveryPrefetchJson().entrySet()) {
                if (hook.getRequest().getPrefetch().getResourceMap().containsKey(entry.getKey())) {
                    JsonElement resourceJson = hook.getRequest().getPrefetch().getResourceMap().get(entry.getKey());
                    if (resourceJson.isJsonNull()) {
                        continue;
                    }
                    IBaseResource resource = fhirContext.newJsonParser().parseResource(gson.toJson(resourceJson));
                    addResources(resource, fhirContext, prefetchResources);
                } else {
                    prefetchElementsToFetch.add(entry.getKey());
                }
            }
        }
        else {
            prefetchElementsToFetch.addAll(hook.getRequest().getPrefetch().getDiscoveryPrefetchJson().keySet());
        }

        for (String elementToFetch : prefetchElementsToFetch) {
            String prefetchUrl = JsonHelper.getStringRequired(hook.getRequest().getPrefetch().getDiscoveryPrefetchJson(), elementToFetch);
            IBaseBundle bundle = client.search().forResource(getResourceName(prefetchUrl)).whereMap(getParameterMap(prefetchUrl, hook)).execute();
            prefetchResources.addAll(resolveBundle(bundle, fhirContext));
        }

        return prefetchResources;
    }

    public static String getResourceName(String prefetchUrl) {
        return prefetchUrl.split("\\?")[0];
    }

    public static Map<String, List<String>> getParameterMap(String prefetchUrl, Hook hook) {
        Map<String, List<String>> parameterMap = new HashMap<>();
        String cleanUrl =
                prefetchUrl.replaceAll("\\{\\{context.patientId}}", hook.getRequest().getContext().getPatientId())
                        .replaceAll("\\{\\{context.encounterId}}", hook.getRequest().getContext().getEncounterId())
                        .replaceAll("\\{\\{context.user}}", hook.getRequest().getUser())
                        .replaceAll("\\{\\{user}}", hook.getRequest().getUser());
        String[] temp = cleanUrl.split("\\?");
        if (temp.length > 1) {
            temp = temp[1].split("&");
            for (String t : temp) {
                String[] tArr = t.split("=");
                if (tArr.length == 2) {
                    parameterMap.put(tArr[0], Collections.singletonList(tArr[1]));
                }
            }
        }
        return parameterMap;
    }
}
