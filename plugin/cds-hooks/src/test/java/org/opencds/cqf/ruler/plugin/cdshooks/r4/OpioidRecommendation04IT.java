package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpioidRecommendation04IT extends OpioidRecommendationTestBase {

    @Test
    void testOpioidRecommendation04WithPrefetch() {
        loadTransaction("opioidcds-04-bundle.json");
        makeRequest("opioidcds-04", "opioidcds-04-request-prefetch.json");
    }

    @Test
    void testOpioidRecommendation04WithoutPrefetch() {
        loadTransaction("opioidcds-04-bundle.json");
        loadResource("opioidcds-04-patient.json");
        loadResource("opioidcds-04-encounter.json");
        loadResource("opioidcds-04-medicationrequest.json");
        makeRequest("opioidcds-04", "opioidcds-04-request.json");
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
                "Recommend use of immediate-release opioids instead of extended release/long acting opioids when starting patient on opioids.",
                card.get("summary").getAsString());
        assertTrue(card.has("indicator"));
        assertTrue(card.get("indicator").isJsonPrimitive());
        assertEquals("warning", card.get("indicator").getAsString());
        assertTrue(card.has("detail"));
        assertTrue(card.get("detail").isJsonPrimitive());
        assertEquals(
                "The following medication requests(s) release rates should be re-evaluated: <Unable to determine medication name>",
                card.get("detail").getAsString());
        assertTrue(card.has("links"));
        assertTrue(card.get("links").isJsonArray());
        assertEquals(2, card.get("links").getAsJsonArray().size());
    }
}
