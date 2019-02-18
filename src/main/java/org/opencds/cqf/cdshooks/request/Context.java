package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Context {

    private JsonObject contextJson;
    private String patientId;
    private String encounterId;

    public Context(JsonObject object) {
        contextJson = object;
        patientId = JsonHelper.getStringRequired(contextJson, "patientId");
        encounterId = JsonHelper.getStringOptional(contextJson, "encounterId");
    }

    public JsonObject getContextJson() {
        return contextJson;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public JsonElement getResourceElement(String name) {
        return contextJson.get(name);
    }
}
