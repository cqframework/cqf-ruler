package org.opencds.cqf.cdshooks.hooks;

import com.google.gson.JsonElement;
import org.opencds.cqf.cdshooks.request.Request;

public class MedicationPrescribeHook extends Hook {

    public MedicationPrescribeHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return getRequest().getContext().getResourceElement("medications");
    }
}
