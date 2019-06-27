package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;

public class Request {

    private String serviceName;
    private JsonObject requestJson;
    private JsonObject prefetchDiscoveryJson;

    private String hook;
    private String hookInstance;
    private String fhirServerUrl;
    private FhirAuthorization fhirAuthorization;
    private String user;
    private Context context;
    private Prefetch prefetch;

    private Boolean applyCql;

    public Request(String serviceName, JsonObject requestJson, JsonObject prefetchDiscoveryJson) {
        this.serviceName = serviceName;
        this.requestJson = requestJson;
        this.prefetchDiscoveryJson = prefetchDiscoveryJson;
    }

    public String getServiceName() {
        return serviceName;
    }

    public JsonObject getRequestJson() {
        return requestJson;
    }

    public String getHook() {
        if (hook == null) {
            hook = JsonHelper.getStringRequired(requestJson, "hook");
        }
        return hook;
    }

    public String getHookInstance() {
        if (hookInstance == null) {
            hookInstance = JsonHelper.getStringRequired(requestJson, "hookInstance");
        }
        return hookInstance;
    }

    public String getFhirServerUrl() {
        if (fhirServerUrl == null) {
            // if fhirAuthorization is present, fhirServer is required
            fhirServerUrl = getFhirAuthorization() == null
                    ? JsonHelper.getStringOptional(requestJson, "fhirServer")
                    : JsonHelper.getStringRequired(requestJson, "fhirServer");
        }
        return fhirServerUrl;
    }

    public FhirAuthorization getFhirAuthorization() {
        if (fhirAuthorization == null) {
            JsonObject object = JsonHelper.getObjectOptional(requestJson, "fhirAuthorization");
            if (object != null) {
                fhirAuthorization = new FhirAuthorization(object);
            }
        }
        return fhirAuthorization;
    }

    public String getUser() {
        if (user == null) {
            user = JsonHelper.getStringOptional(requestJson, "user");
            // account for case when user is in the context
            if (user == null) {
                user = JsonHelper.getStringRequired(getContext().getContextJson(), "user");
                if (user == null) {
                    user = JsonHelper.getStringRequired(getContext().getContextJson(), "userId");
                }
            }
        }
        return user;
    }

    public Context getContext() {
        if (context == null) {
            context = new Context(JsonHelper.getObjectRequired(requestJson, "context"));
        }
        return context;
    }

    public Prefetch getPrefetch() {
        if (prefetch == null) {
            prefetch = new Prefetch(JsonHelper.getObjectOptional(requestJson, "prefetch"), prefetchDiscoveryJson);
        }
        return prefetch;
    }

    public boolean isApplyCql() {
        if (applyCql == null) {
            applyCql = JsonHelper.getBooleanOptional(requestJson, "applyCql");
        }
        return applyCql;
    }
}
