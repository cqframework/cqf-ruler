package org.opencds.cqf.cdshooks.hooks;

import com.google.gson.JsonElement;
import org.opencds.cqf.cdshooks.request.Request;

public class OrderReviewHook extends Hook {

    public OrderReviewHook(Request request) {
        super(request);
    }

    @Override
    public JsonElement getContextResources() {
        return getRequest().getContext().getResourceElement("orders");
    }
}
