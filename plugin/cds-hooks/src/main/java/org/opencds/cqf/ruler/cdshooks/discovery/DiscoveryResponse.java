package org.opencds.cqf.ruler.cdshooks.discovery;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
