package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

public class JsonFieldResolution {

    public static String getStringField(JsonObject object, String field, boolean required) {
        JsonElement stringField = object.get(field);
        if (validateNull(stringField, field, required) == null) {
            return null;
        }
        if (stringField.isJsonPrimitive() && stringField.getAsJsonPrimitive().isString()) {
            return stringField.getAsString();
        }
        throw new InvalidFieldTypeException("Expected JSON String type for field " + field);
    }

    public static Boolean getBooleanField(JsonObject object, String field, boolean required) {
        JsonElement stringField = object.get(field);
        if (validateNull(stringField, field, required) == null) {
            return null;
        }
        if (stringField.isJsonPrimitive() && stringField.getAsJsonPrimitive().isBoolean()) {
            return stringField.getAsBoolean();
        }
        throw new InvalidFieldTypeException("Expected JSON String type for field " + field);
    }

    public static Integer getIntegerField(JsonObject object, String field, boolean required) {
        JsonElement intField = object.get(field);
        if (validateNull(intField, field, required) == null) {
            return null;
        }
        if (intField.isJsonPrimitive() && intField.getAsJsonPrimitive().isNumber()) {
            return intField.getAsInt();
        }
        throw new InvalidFieldTypeException("Expected JSON Integer type for field " + field);
    }

    public static JsonObject getObjectField(JsonObject object, String field, boolean required) {
        JsonElement objectField = object.get(field);
        if (validateNull(objectField, field, required) == null) {
            return null;
        }
        if (objectField.isJsonObject()) {
            return objectField.getAsJsonObject();
        }
        throw new InvalidFieldTypeException("Expected JSON Object type for field " + field);
    }

    public static JsonArray getArrayField(JsonObject object, String field, boolean required) {
        JsonElement arrayField = object.get(field);
        if (validateNull(arrayField, field, required) == null) {
            return null;
        }
        if (arrayField.isJsonObject()) {
            return arrayField.getAsJsonArray();
        }
        throw new InvalidFieldTypeException("Expected JSON Object type for field " + field);
    }

    private static Boolean validateNull(JsonElement field, String fieldName, boolean required) {
        if (field == null) {
            if (required) {
                throw new MissingRequiredFieldException("The required field: " + fieldName + " is missing from the request");
            }
            return null;
        }

        return true;
    }
}
