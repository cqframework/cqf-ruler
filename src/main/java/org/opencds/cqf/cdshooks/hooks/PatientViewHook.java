package org.opencds.cqf.cdshooks.hooks;

import com.google.gson.JsonElement;
import org.opencds.cqf.cdshooks.request.Request;

public class PatientViewHook extends Hook {

    public PatientViewHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return null;
    }
}
