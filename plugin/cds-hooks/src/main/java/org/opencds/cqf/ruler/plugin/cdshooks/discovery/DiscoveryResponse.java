package org.opencds.cqf.ruler.plugin.cdshooks.discovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryResponse {
    private List<DiscoveryElement> discoveryElements;

    public DiscoveryResponse() {
        discoveryElements = new ArrayList<>();
    }

    public void addElement(DiscoveryElement element) {
        discoveryElements.add(element);
    }

    public JsonObject getAsJson() {
        JsonObject responseJson = new JsonObject();
        JsonArray services = new JsonArray();

        for (DiscoveryElement element : discoveryElements) {
            services.add(element.getAsJson());
        }

        responseJson.add("services", services);
        return responseJson;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(getAsJson());
    }
}
