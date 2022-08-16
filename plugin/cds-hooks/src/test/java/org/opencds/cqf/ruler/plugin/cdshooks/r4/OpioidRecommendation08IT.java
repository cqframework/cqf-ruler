package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.plugin.cdshooks.ResourceChangeEvent;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { OpioidRecommendation08IT.class, CdsHooksConfig.class },
        properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
class OpioidRecommendation08IT extends RestIntegrationTest {

    @Autowired
    CdsServicesCache cdsServicesCache;
    private String ourCdsBase;

    @BeforeEach
    void beforeEach() {
        ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
    }

    @Test
    void testOpioidRecommendation08OrderSignWithoutPrefetch() {
        loadTransaction("opioidcds-08-order-sign-bundle.json");
        loadResource("opioidcds-08-patient.json");
        loadResource("opioidcds-08-medication.json");

        ResourceChangeEvent rce = new ResourceChangeEvent();
        rce.setCreatedResourceIds(
                Collections.singletonList(new IdType("PlanDefinition/opioidcds-08-order-sign")));
        cdsServicesCache.handleChange(rce);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String cdsHooksRequestString = stringFromResource("opioidcds-08-request.json");
            Gson jsonParser = new Gson();
            JsonObject cdsHooksRequestObject = jsonParser.fromJson(cdsHooksRequestString, JsonObject.class);
            cdsHooksRequestObject.addProperty("fhirServer", getServerBase());

            HttpPost request = new HttpPost(ourCdsBase + "/opioidcds-08-order-sign");
            request.setEntity(new StringEntity(cdsHooksRequestObject.toString()));
            request.addHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(request);
            validate(EntityUtils.toString(response.getEntity()));
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }

    @Test
    void testOpioidRecommendation08OrderSignWithPrefetch() {
        loadTransaction("opioidcds-08-order-sign-bundle.json");
        loadResource("opioidcds-08-medication.json");
        ResourceChangeEvent rce = new ResourceChangeEvent();
        rce.setCreatedResourceIds(
                Collections.singletonList(new IdType("PlanDefinition/opioidcds-08-order-sign")));
        cdsServicesCache.handleChange(rce);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String cdsHooksRequestString = stringFromResource("opioidcds-08-request-prefetch.json");
            Gson jsonParser = new Gson();
            JsonObject cdsHooksRequestObject = jsonParser.fromJson(cdsHooksRequestString, JsonObject.class);
            cdsHooksRequestObject.addProperty("fhirServer", getServerBase());

            HttpPost request = new HttpPost(ourCdsBase + "/opioidcds-08-order-sign");
            request.setEntity(new StringEntity(cdsHooksRequestObject.toString()));
            request.addHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(request);
            validate(EntityUtils.toString(response.getEntity()));
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
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
        // TODO: The returned value is longer than the allowed 140 characters - update recommendation or be more lenient
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
