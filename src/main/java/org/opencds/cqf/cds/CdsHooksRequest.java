package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.*;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.Bundle;
import org.opencds.cqf.exceptions.*;
import org.opencds.cqf.helpers.Dstu2ToStu3;

import java.io.IOException;
import java.io.Reader;

public class CdsHooksRequest {

    private JsonObject requestJson;

    private String hook;
    private String hookInstance;
    private String fhirServer;
    private JsonObject fhirAuthorization;
    private String user; // this is really a Reference (Resource/ID)
    private JsonObject context;
    private JsonObject prefetch;

    // Not an standard request element
    private String service;
    public void setService(String service) {
        this.service = service;
    }
    public String getService() {
        return this.service;
    }

    public CdsHooksRequest(Reader cdsHooksRequest) throws IOException {
        JsonParser parser = new JsonParser();
        this.requestJson =  parser.parse(cdsHooksRequest).getAsJsonObject();

        this.hook = this.requestJson.getAsJsonPrimitive("hook").getAsString();
        if (this.hook == null || this.hook.isEmpty()) {
            throw new MissingHookException();
        }

        this.hookInstance = this.requestJson.getAsJsonPrimitive("hookInstance").getAsString();
        if (this.hookInstance == null || this.hookInstance.isEmpty()) {
            throw new MissingHookInstanceException();
        }

        this.user = this.requestJson.getAsJsonPrimitive("user").getAsString();
        if (this.user == null || this.user.isEmpty()) {
            throw new MissingUserException();
        }

        this.context = this.requestJson.getAsJsonObject("context");
        if (this.context == null || this.context.size() < 2) {
            throw new MissingContextException();
        }

        // if prefetch isn't provided or populated, ensure the fhirServer endpoint is supplied
        this.prefetch = this.requestJson.getAsJsonObject("prefetch");
        if (this.prefetch == null || this.prefetch.size() == 0) {
            this.fhirServer = this.requestJson.getAsJsonPrimitive("fhirServer").getAsString();
            if (this.fhirServer == null || this.fhirServer.isEmpty()) {
                throw new MissingFhirServerException();
            }
        }
    }

    public String getHook() {
        return this.hook;
    }

    public String getHookInstance() {
        return this.hookInstance;
    }

    public String getFhirServer() {
        return this.fhirServer;
    }

    public JsonObject getFhirAuthorization() {
        if (this.fhirAuthorization == null) {
            this.fhirAuthorization = this.requestJson.getAsJsonObject("fhirAuthorization");
        }
        return this.fhirAuthorization;
    }
    public String getAccessToken() {
        if (getFhirAuthorization() == null) {
            return null;
        }
        return getFhirAuthorization().getAsJsonPrimitive("access_token").getAsString();
    }
    public String getTokenType() {
        if (getFhirAuthorization() == null) {
            return null;
        }
        return getFhirAuthorization().getAsJsonPrimitive("token_type").getAsString();
    }
    public Integer getExpiresIn() {
        if (getFhirAuthorization() == null) {
            return null;
        }
        return getFhirAuthorization().getAsJsonPrimitive("expires_in").getAsInt();
    }
    public String getScope() {
        if (getFhirAuthorization() == null) {
            return null;
        }
        return getFhirAuthorization().getAsJsonPrimitive("scope").getAsString();
    }
    public String getSubject() {
        if (getFhirAuthorization() == null) {
            return null;
        }
        return getFhirAuthorization().getAsJsonPrimitive("subject").getAsString();
    }

    public String getUser() {
        return this.user;
    }

    public JsonObject getContext() {
        return this.context;
    }
    public String getContextProperty(String property) {
        return this.context.getAsJsonPrimitive(property).getAsString();
    }
    public JsonObject getContextObject(String property) {
        return this.context.getAsJsonObject(property);
    }
    public Resource getContextResource(String property) {
        Gson gson = new Gson();
        String resource = gson.toJson(this.context.getAsJsonObject(property));
        try {
            return (Resource) FhirContext.forDstu3().newJsonParser().parseResource(resource);
        } catch (Exception e) {
            return Dstu2ToStu3.convertResource(
                    (org.hl7.fhir.instance.model.Resource) FhirContext.forDstu2Hl7Org().newJsonParser().parseResource(resource)
            );
        }
    }

    /*
        Prefetch format:
        "prefetch": {
            "{{ propertyName }}": {
                "response": {
                    "status": {{ status code }}
                },
                "resource": { FHIR Resource (may be a Bundle) }
            }
        }

    */

    public JsonObject getPrefetch() {
        if (this.prefetch == null) {
            JsonObject temp = this.requestJson.getAsJsonObject("prefetch");
            this.prefetch = temp == null ? new JsonObject() : temp;
        }
        return this.prefetch;
    }
    public void setPrefetch(JsonObject prefetch) {
        this.prefetch = prefetch;
    }
    // Convenience method
    // Populates resources array for sub-element of prefetch i.e. "supplyRequests" for order-review hook
    public void setPrefetch(Bundle prefetchBundle, String propertyName) {
        JsonObject subJson = new JsonObject();
        JsonParser parser = new JsonParser();
        JsonObject bundle = new JsonObject();
        if (prefetchBundle.hasEntry() && prefetchBundle.getEntry().size() > 0) {
            bundle =
                    parser.parse(
                            FhirContext.forDstu3().newJsonParser().encodeResourceToString(Dstu2ToStu3.convertResource(prefetchBundle))
                    ).getAsJsonObject();
        }
        subJson.add("response", getPrefetchResponse());
        subJson.add("resource", bundle);
        this.prefetch.add(propertyName, subJson);
    }
    // Convenience method
    public void setPrefetch(org.hl7.fhir.dstu3.model.Bundle prefetchBundle, String propertyName) {
        JsonObject subJson = new JsonObject();
        JsonParser parser = new JsonParser();
        JsonObject bundle = new JsonObject();
        if (prefetchBundle.hasEntry() && prefetchBundle.getEntry().size() > 0) {
            bundle = parser.parse(FhirContext.forDstu3().newJsonParser().encodeResourceToString(prefetchBundle)).getAsJsonObject();
        }
        subJson.add("response", getPrefetchResponse());
        subJson.add("resource", bundle);
        this.prefetch.add(propertyName, subJson);
    }
    private JsonObject getPrefetchResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("status", "200 OK");
        return response;
    }
}
