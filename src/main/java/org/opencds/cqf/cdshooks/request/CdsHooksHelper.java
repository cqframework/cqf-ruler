package org.opencds.cqf.cdshooks.request;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.*;
import org.hl7.fhir.instance.model.Bundle;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.cdshooks.request.JsonFieldResolution;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher Schuler on 5/1/2017.
 */
public class CdsHooksHelper {

    public static String getPrettyJson(String uglyJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(uglyJson);
        return gson.toJson(element);
    }

    private static Gson gson = new Gson();
    // For context and prefetch resource resolution
    public static List<Object> parseResources(JsonArray resourceArray, CdsHooksProviders.FhirVersion version) {
        List<Object> resources = new ArrayList<>();
        for (JsonElement resource : resourceArray) {
            if (resource.isJsonObject()) {
                resources.add(parseResource(resource.getAsJsonObject(), version));
            }
            else {
                throw new InvalidFieldTypeException("Expected JSON Object type in resource Array");
            }
        }
        return resources;
    }

    public static List<Object> parseResources(JsonObject resource, CdsHooksProviders.FhirVersion version) {
        List<Object> resources = new ArrayList<>();
        String resourceType = JsonFieldResolution.getStringField(resource, "resourceType", true);
        if (resourceType.equals("Bundle")) {
            if (version == CdsHooksProviders.FhirVersion.DSTU2) {
                Bundle bundle = (Bundle) FhirContext.forDstu2Hl7Org().newJsonParser().parseResource(gson.toJson(resource));
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
            }
            else {
                org.hl7.fhir.dstu3.model.Bundle bundle =
                        (org.hl7.fhir.dstu3.model.Bundle) FhirContext.forDstu3().newJsonParser().parseResource(gson.toJson(resource));
                for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
            }
        }
        else {
            resources.add(parseResource(resource, version));
        }
        return resources;
    }

    public static List<Object> parseResources(JsonElement resource, CdsHooksProviders.FhirVersion version) {
        if (resource.isJsonObject()) {
            return parseResources(resource.getAsJsonObject(), version);
        }
        else if (resource.isJsonArray()) {
            return parseResources(resource.getAsJsonArray(), version);
        }
        else {
            throw new InvalidFieldTypeException("Invalid JSON type found - expecting Object or Array");
        }
    }

    public static Object parseResource(JsonObject resource, CdsHooksProviders.FhirVersion version) {
        if (version == CdsHooksProviders.FhirVersion.DSTU2) {
            return FhirContext.forDstu2Hl7Org().newJsonParser().parseResource(gson.toJson(resource));
        }
        else {
            return FhirContext.forDstu3().newJsonParser().parseResource(gson.toJson(resource));
        }
    }
}
