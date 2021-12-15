package org.opencds.cqf.ruler.plugin.cdshooks.hooks;

import org.opencds.cqf.ruler.plugin.cdshooks.request.Request;
import com.google.gson.JsonElement;

public class PatientViewHook extends Hook {

    public PatientViewHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return null;
    }
}
