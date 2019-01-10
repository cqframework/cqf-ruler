package org.opencds.cqf.bulkdata;

import com.google.gson.*;

import java.util.*;

public class BulkDataResponse {

    private Date transactionTime;
    private String request;
    private boolean requiresAccessToken;
    private Map<String, String> resources;
    private String outputUrlBase;
    private List<String> error;

    public Date getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public boolean isRequiresAccessToken() {
        return requiresAccessToken;
    }

    public void setRequiresAccessToken(boolean requiresAccessToken) {
        this.requiresAccessToken = requiresAccessToken;
    }

    public String getOutputUrlBase() {
        return outputUrlBase;
    }

    public void setOutputUrlBase(String outputUrlBase) {
        this.outputUrlBase = outputUrlBase;
    }

    public List<String> getError() {
        return error;
    }

    public void addError(String error) {
        this.error.add(error);
    }

    public Map<String, String> getResources() {
        return resources;
    }

    public void addResource(String resourceName, String ndjson) {
        resources.put(resourceName, ndjson);
    }

    public BulkDataResponse() {
        resources = new HashMap<>();
        error = new ArrayList<>();
    }

    public String getJson() {
        JsonObject json = new JsonObject();
        json.add("transactionTime", new JsonPrimitive(transactionTime.toInstant().toString()));
        json.add("request", new JsonPrimitive(request));

        JsonArray output = new JsonArray();
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            JsonObject outputObj = new JsonObject();
            outputObj.add("type", new JsonPrimitive(entry.getKey()));
            outputObj.add("url", new JsonPrimitive(outputUrlBase + "/" + entry.getKey()));
            output.add(outputObj);
        }
        json.add("output", output);

        JsonArray errors = new JsonArray();
        int count = 0;
        for (String error : error) {
            JsonObject errObject = new JsonObject();
            errObject.add("type", new JsonPrimitive("OperationOutcome"));
            errObject.add("url", new JsonPrimitive(outputUrlBase + "/error_" + Integer.toString(++count)));
            output.add(errObject);
        }
        json.add("error", errors);

        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }
}
