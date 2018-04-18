package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;

import java.util.List;

public abstract class Context {

    private JsonObject context;
    private String patientId;
    private String encounterId;

    public JsonObject getContext() {
        return context;
    }
    public String getPatientId() {
        return patientId;
    }
    public String getEncounterId() {
        return encounterId;
    }

    public abstract List<Object> getResources(CdsHooksProviders.FhirVersion version);
    public abstract void validate();

    public Context(JsonObject context) {
        this.context = context;
        patientId = JsonFieldResolution.getStringField(context, "patientId", true);
        encounterId = JsonFieldResolution.getStringField(context, "encounterId", false);
        validate();
    }
}
