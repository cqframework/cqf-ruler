package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpioidRecommendation08IT extends OpioidRecommendationTestBase {

    @Test
    void testOpioidRecommendation08OrderSignWithoutPrefetch() {
        loadTransaction("opioidcds-08-order-sign-bundle.json");
        loadResource("opioidcds-08-patient.json");
        loadResource("opioidcds-08-medication.json");
        makeRequest("opioidcds-08-order-sign", "opioidcds-08-request.json");
    }

    @Test
    void testOpioidRecommendation08OrderSignWithPrefetch() {
        loadTransaction("opioidcds-08-order-sign-bundle.json");
        loadResource("opioidcds-08-medication.json");
        makeRequest("opioidcds-08-order-sign", "opioidcds-08-request-prefetch.json");
    }

    void validate(String cardsString) {
        Gson jsonParser = new Gson();
        JsonObject cardsObject = jsonParser.fromJson(cardsString, JsonObject.class);
        assertTrue(cardsObject.has("cards"));

        JsonArray cardsArray = cardsObject.get("cards").getAsJsonArray();
        assertFalse(cardsArray.isEmpty());
        assertEquals(1, cardsArray.size());
        assertTrue(cardsArray.get(0).isJsonObject());

        JsonObject card = cardsArray.get(0).getAsJsonObject();
        assertTrue(card.has("summary"));
        assertTrue(card.get("summary").isJsonPrimitive());
        assertEquals(
                "Incorporate into the management plan strategies to mitigate risk; including considering offering naloxone when factors that increase risk for opioid overdose are present",
                card.get("summary").getAsString());
        assertTrue(card.has("indicator"));
        assertTrue(card.get("indicator").isJsonPrimitive());
        assertEquals("warning", card.get("indicator").getAsString());
        assertTrue(card.has("detail"));
        assertTrue(card.get("detail").isJsonPrimitive());
        assertEquals(
                "Consider offering naloxone given following risk factor(s) for opioid overdose: Average MME (180.0 '{MME}/d') >= 50 mg/d.",
                card.get("detail").getAsString());
        assertTrue(card.has("links"));
        assertTrue(card.get("links").isJsonArray());
        assertEquals(2, card.get("links").getAsJsonArray().size());
    }

}
