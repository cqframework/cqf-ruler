package org.opencds.cqf.ruler.cdshooks.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "hook")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CdsHooksRequest.PatientView.class, name = "patient-view"),
        @JsonSubTypes.Type(value = CdsHooksRequest.OrderSign.class, name = "order-sign"),
        @JsonSubTypes.Type(value = CdsHooksRequest.OrderSelect.class, name = "order-select")
})
public class CdsHooksRequest {
    private static final Gson gson = new Gson();

    @JsonProperty(required = true)
    public String hook;
    @JsonProperty(required = true)
    public String hookInstance;
    @JsonProperty
    public String fhirServer;
    @JsonProperty
    public FhirAuthorization fhirAuthorization;
    @JsonProperty(required = true)
    public Context context;
    @JsonProperty
    public Prefetch prefetch;
    @JsonIgnore
    public Object extension;

    public static class FhirAuthorization {
        @JsonProperty(value = "access_token", required = true)
        public String accessToken;
        @JsonProperty(value = "token_type", required = true, defaultValue = "Bearer")
        public String tokenType;
        @JsonProperty(value = "expires_in", required = true)
        public int expiresIn;
        @JsonProperty(required = true)
        public String scope;
        @JsonProperty(required = true)
        public String subject;
    }

    public static class Context {
        @JsonProperty(required = true)
        public String userId;
        @JsonProperty(required = true)
        public String patientId;
        @JsonProperty
        public String encounterId;
    }

    public static class Prefetch {
        @JsonProperty
        public Map<String, JsonElement> resources = new HashMap<>();

        @JsonAnySetter
        public void setResources(String key, Object resource) {
            if (resource == null) {
                resources.put(key, JsonNull.INSTANCE);
            }
            JsonElement element = gson.fromJson(gson.toJson(resource), JsonElement.class);
            if (element.isJsonArray()) {
                resources.put(key, element.getAsJsonArray());
            }
            else if (element.isJsonObject()) {
                JsonObject resourceObject = element.getAsJsonObject();
                if (resourceObject.has("response")) {
                    JsonObject response = resourceObject.get("response").getAsJsonObject();
                    if (response.has("status") && response.get("status").getAsString().equals("200 OK")
                            && resourceObject.has("resource")) {
                        resources.put(key, resourceObject.get("resource").getAsJsonObject());
                    }
                } else {
                    resources.put(key, resourceObject);
                }
            }
        }
    }

    @JsonTypeName("patient-view")
    public static class PatientView extends CdsHooksRequest {

    }

    @JsonTypeName("order-sign")
    public static class OrderSign extends CdsHooksRequest {
        @JsonProperty(required = true)
        public CdsHooksRequest.OrderSign.Context context;

        public static class Context extends CdsHooksRequest.Context {
            @JsonProperty(required = true)
            public JsonObject draftOrders;

            @JsonAnySetter
            public void setDraftOrders(Object bundle) {
                draftOrders = gson.fromJson(gson.toJson(bundle), JsonObject.class);
            }
        }
    }

    @JsonTypeName("order-select")
    public static class OrderSelect extends CdsHooksRequest {
        @JsonProperty(required = true)
        public CdsHooksRequest.OrderSelect.Context context;

        public static class Context extends CdsHooksRequest.OrderSign.Context {
            @JsonProperty(required = true)
            private List<String> selections;
        }
    }
}
