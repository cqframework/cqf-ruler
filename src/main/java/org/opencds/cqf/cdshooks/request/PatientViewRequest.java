package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;

public class PatientViewRequest extends CdsRequest {

    public PatientViewRequest(JsonObject requestJson) {
        super(requestJson);
    }

    @Override
    public void setContext(JsonObject context) {
        this.context = new PatientViewContext(context);
    }
}
