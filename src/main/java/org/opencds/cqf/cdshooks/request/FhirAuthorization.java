package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;

public class FhirAuthorization {

    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
    private String scope;
    private String subject;

    public String getAccessToken() {
        return accessToken;
    }
    public String getTokenType() {
        return tokenType;
    }
    public Integer getExpiresIn() {
        return expiresIn;
    }
    public String getScope() {
        return scope;
    }
    public String getSubject() {
        return subject;
    }

    public FhirAuthorization(JsonObject fhirAuthorization) {
        if (fhirAuthorization != null) {
            accessToken = JsonFieldResolution.getStringField(fhirAuthorization, "access_token", true);
            tokenType = JsonFieldResolution.getStringField(fhirAuthorization, "token_type", true);
            expiresIn = JsonFieldResolution.getIntegerField(fhirAuthorization, "expires_in", true);
            scope = JsonFieldResolution.getStringField(fhirAuthorization, "scope", true);
            subject = JsonFieldResolution.getStringField(fhirAuthorization, "subject", true);
        }
    }
}
