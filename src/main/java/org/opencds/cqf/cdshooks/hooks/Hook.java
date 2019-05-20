package org.opencds.cqf.cdshooks.hooks;

import com.google.gson.JsonElement;
import org.opencds.cqf.cdshooks.request.Request;

public abstract class Hook {

    private Request request;

    public Hook(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }

    public abstract JsonElement getContextResources();
}
