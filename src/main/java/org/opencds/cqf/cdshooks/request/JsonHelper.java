package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

public class JsonHelper {

    public static String getStringRequired(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element != null && !element.isJsonNull()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                return getString(element);
            }
            else {
                throw new InvalidFieldTypeException("Expected JSON String type for field: " + field);
            }
        }
        throw new MissingRequiredFieldException("Missing required field: " + field);
    }

    public static String getStringOptional(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return getString(element);
        }
        throw new InvalidFieldTypeException("Expected JSON String type for field: " + field);
    }

    private static String getString(JsonElement element) {
        return element.getAsString();
    }

    public static int getIntRequired(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element != null && !element.isJsonNull()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return getInt(element);
            }
            else {
                throw new InvalidFieldTypeException("Expected JSON Integer type for field: " + field);
            }
        }
        throw new MissingRequiredFieldException("Missing required field: " + field);
    }

    private static int getInt(JsonElement element) {
        return element.getAsInt();
    }

    public static JsonObject getObjectRequired(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element != null && !element.isJsonNull()) {
            if (element.isJsonObject()) {
                return getObject(element);
            }
            else {
                throw new InvalidFieldTypeException("Expected JSON Object type for field: " + field);
            }
        }
        throw new MissingRequiredFieldException("Missing required field: " + field);
    }

    public static JsonObject getObjectOptional(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        else if (element.isJsonObject()) {
            return getObject(element);
        }
        throw new InvalidFieldTypeException("Expected JSON Object type for field: " + field);
    }

    private static JsonObject getObject(JsonElement element) {
        return element.getAsJsonObject();
    }

    public static boolean getBooleanOptional(JsonObject request, String field) {
        JsonElement element = request.get(field);
        if (element == null || element.isJsonNull()) {
            return false;
        }
        else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        throw new InvalidFieldTypeException("Expected JSON Boolean type for field: " + field);
    }
}
