package org.opencds.cqf.ruler.plugin.cdshooks.request;

import org.opencds.cqf.ruler.plugin.cdshooks.exceptions.InvalidFieldTypeException;
import com.google.gson.JsonObject;

public class FhirAuthorization {

    private String accessToken;
    private String tokenType;
    private Object expiresIn;
    private String scope;
    private String subject;

    public FhirAuthorization(JsonObject object) {
        accessToken = JsonHelper.getStringRequired(object, "access_token");
        tokenType = JsonHelper.getStringRequired(object, "token_type");
        try {
            expiresIn = JsonHelper.getStringOptional(object, "expires_in");
        }
        catch (InvalidFieldTypeException ife) {
            expiresIn = JsonHelper.getIntRequired(object, "expires_in");
        }
        scope = JsonHelper.getStringRequired(object, "scope");
        subject = JsonHelper.getStringRequired(object, "subject");
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Object getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getSubject() {
        return subject;
    }
}
