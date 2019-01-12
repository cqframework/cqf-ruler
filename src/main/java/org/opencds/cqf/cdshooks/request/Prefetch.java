package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Prefetch {

    private JsonObject prefetchJson;
    private JsonObject discoveryPrefetchJson;
    private Map<String, JsonElement> resourceMap;

    public Prefetch(JsonObject prefetchJson, JsonObject discoveryPrefetchJson) {
        this.prefetchJson = prefetchJson;
        this.discoveryPrefetchJson = discoveryPrefetchJson;
        resourceMap = new HashMap<>();

        if (prefetchJson != null) {
            for (Map.Entry<String, JsonElement> entry : prefetchJson.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    if (obj.has("response") && obj.get("response").isJsonObject()) {
                        String status = JsonHelper.getStringRequired(obj.get("response").getAsJsonObject(), "status");
                        if (status.startsWith("200") && obj.has("resource")) {
                            resourceMap.put(entry.getKey(), obj.get("resource"));
                        }
                    }
                    else {
                        resourceMap.put(entry.getKey(), obj);
                    }
                }
                else {
                    resourceMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public JsonObject getPrefetchJson() {
        return prefetchJson;
    }

    public JsonObject getDiscoveryPrefetchJson() {
        return discoveryPrefetchJson;
    }

    public Map<String, JsonElement> getResourceMap() {
        return resourceMap;
    }
}
