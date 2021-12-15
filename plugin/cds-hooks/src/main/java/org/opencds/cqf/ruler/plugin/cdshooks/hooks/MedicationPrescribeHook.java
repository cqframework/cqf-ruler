package org.opencds.cqf.ruler.plugin.cdshooks.hooks;

import org.opencds.cqf.ruler.plugin.cdshooks.request.JsonHelper;
import org.opencds.cqf.ruler.plugin.cdshooks.request.Request;
import com.google.gson.JsonElement;

public class MedicationPrescribeHook extends Hook {

    public MedicationPrescribeHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return JsonHelper.getObjectRequired(getRequest().getContext().getContextJson(), "medications");
    }
}
