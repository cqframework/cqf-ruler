package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;

public class OrderReviewRequest extends CdsRequest {

    public OrderReviewRequest(JsonObject requestJson) {
        super(requestJson);
    }

    @Override
    public void setContext(JsonObject context) {
        this.context = new OrderReviewContext(context);
    }
}
