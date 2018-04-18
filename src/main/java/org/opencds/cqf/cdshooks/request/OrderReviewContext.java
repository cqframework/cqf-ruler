package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.exceptions.InvalidContextException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

import java.util.List;

public class OrderReviewContext extends Context {

    private List<Object> orderList;
    private JsonElement orders;

    public OrderReviewContext(JsonObject context) {
        super(context);
    }

    @Override
    public List<Object> getResources(CdsHooksProviders.FhirVersion version) {
        if (orderList == null) {
            orderList = CdsHooksHelper.parseResources(orders, version);
        }
        return orderList;
    }

    @Override
    public void validate() {
        orders = getContext().get("orders");
        if (orders == null) {
            throw new MissingRequiredFieldException("The context for the order-review hook must include an orders field");
        }
        for (String key : getContext().keySet()) {
            if (!key.equals("patientId") && !key.equals("encounterId") && !key.equals("orders")) {
                throw new InvalidContextException("Invalid order-review context field: " + key + ", expecting patientId, encounterId, or orders");
            }
        }
    }
}
