package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;

public class MedicationPrescribeRequest extends CdsRequest {

    public MedicationPrescribeRequest(JsonObject requestJson) {
        super(requestJson);
    }

    @Override
    public void setContext(JsonObject context) {
        this.context = new MedicationPrescribeContext(context);
    }
}
