package org.opencds.cqf.ruler.cdshooks.hooks;

import org.opencds.cqf.ruler.cdshooks.request.JsonHelper;
import org.opencds.cqf.ruler.cdshooks.request.Request;
import com.google.gson.JsonElement;

public class OrderReviewHook extends Hook {

    public OrderReviewHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return JsonHelper.getObjectRequired(getRequest().getContext().getContextJson(), "orders");
    }
}
