package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpioidRecommendation01IT extends OpioidRecommendationTestBase {

    @Test
    void testOpioidRecommendation01WithPrefetch() {
        loadTransaction("opioidcds-01-bundle.json");
        makeRequest("opioidcds-01", "opioidcds-01-request-prefetch.json");
    }

    @Test
    void testOpioidRecommendation01WithoutPrefetch() {
        loadTransaction("opioidcds-01-bundle.json");
        loadResource("opioidcds-01-patient.json");
        loadResource("opioidcds-01-medicationstatement.json");
        makeRequest("opioidcds-01", "opioidcds-01-request.json");
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
                "Recommend use of nonpharmacologic therapy and nonopioid pharmacologic therapy as alternative",
                card.get("summary").getAsString());
        assertTrue(card.has("indicator"));
        assertTrue(card.get("indicator").isJsonPrimitive());
        assertEquals("warning", card.get("indicator").getAsString());
        assertTrue(card.has("detail"));
        assertTrue(card.get("detail").isJsonPrimitive());
        assertEquals(
                "Medication requests(s): <Unable to determine medication name>",
                card.get("detail").getAsString());
        assertTrue(card.has("links"));
        assertTrue(card.get("links").isJsonArray());
        assertEquals(1, card.get("links").getAsJsonArray().size());
    }
}
