package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;
import org.opencds.cqf.exceptions.InvalidHookException;
import org.opencds.cqf.exceptions.InvalidRequestException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

import java.io.Reader;

public class CdsRequestFactory {

    // TODO - check for cdc-opioid-guidance base call - runs every recommendation
    public static CdsRequest createRequest(Reader cdsHooksRequest) {
        JsonParser parser = new JsonParser();
        JsonElement request;
        try {
            request = parser.parse(cdsHooksRequest);
        } catch(JsonParseException e) {
            throw new InvalidRequestException("Error parsing the JSON request: " + e.getMessage());
        }
        if (!request.isJsonObject()) {
            throw new InvalidRequestException("Expecting JSON Object type for the request");
        }
        JsonObject requestJson = request.getAsJsonObject();

        JsonElement hook = requestJson.get("hook");
        if (hook == null) {
            throw new MissingRequiredFieldException("The required hook field is missing from the request");
        }
        if (hook.isJsonPrimitive() && hook.getAsJsonPrimitive().isString()) {
            switch(hook.getAsJsonPrimitive().getAsString()) {
                case "patient-view":
                    return new PatientViewRequest(requestJson);
                case "medication-prescribe":
                    return new MedicationPrescribeRequest(requestJson);
                case "order-review":
                    return new OrderReviewRequest(requestJson);
                default:
                    throw new InvalidHookException(
                            "Invalid hook: " + hook.getAsJsonPrimitive().getAsString() + ", expecting patient-view | medication-prescribe | order-review"
                    );
            }
        }
        throw new InvalidFieldTypeException("Expecting JSON String for hook field");
    }

}
