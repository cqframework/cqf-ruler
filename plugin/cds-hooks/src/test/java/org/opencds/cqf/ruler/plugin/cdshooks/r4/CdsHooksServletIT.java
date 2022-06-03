package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
		CdsHooksConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.cr.security_configuration.enabled=false"
		})
public class CdsHooksServletIT extends RestIntegrationTest {
	String ourCdsBase;

	@BeforeEach
	void beforeEach() {
		ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
	}

	@Test
	public void testGetCdsServices() throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet request = new HttpGet(ourCdsBase);
		request.addHeader("Content-Type", "application/json");
		assertEquals(200, httpClient.execute(request).getStatusLine().getStatusCode());
	}

	@Test
	// TODO: Debug delay in Client.search().
	public void testCdsServicesRequest() throws IOException {
		// Server Load
		loadTransaction("Screening-bundle-r4.json");
		Patient ourPatient = getClient().read().resource(Patient.class).withId("HighRiskIDUPatient").execute();
		assertNotNull(ourPatient);
		assertEquals("HighRiskIDUPatient", ourPatient.getIdElement().getIdPart());
		PlanDefinition ourPlanDefinition = getClient().read().resource(PlanDefinition.class)
				.withId("plandefinition-Screening").execute();
		assertNotNull(ourPlanDefinition);
		Bundle getPlanDefinitions = null;
		int tries = 0;
		do {
			// Can take up to 10 seconds for HAPI to reindex searches
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tries++;
			getPlanDefinitions = getClient().search().forResource(PlanDefinition.class).returnBundle(Bundle.class)
					.execute();
		} while (getPlanDefinitions.getEntry().size() == 0 && tries < 15);
		assertTrue(getPlanDefinitions.hasEntry());

		// Update fhirServer Base
		String jsonHooksRequest = stringFromResource("request-HighRiskIDUPatient.json");
		Gson gsonRequest = new Gson();
		JsonObject jsonRequestObject = gsonRequest.fromJson(jsonHooksRequest, JsonObject.class);
		jsonRequestObject.addProperty("fhirServer", getServerBase());

		// Setup Client
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost request = new HttpPost(ourCdsBase + "/plandefinition-Screening");
		request.setEntity(new StringEntity(jsonRequestObject.toString()));
		request.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpClient.execute(request);
		String result = EntityUtils.toString(response.getEntity());

		Gson gsonResponse = new Gson();
		JsonObject jsonResponseObject = gsonResponse.fromJson(result, JsonObject.class);

		// Ensure Cards
		assertNotNull(jsonResponseObject.get("cards"));
		JsonArray cards = jsonResponseObject.get("cards").getAsJsonArray();

		// Ensure Patient Detail
		assertNotNull(cards.get(0).getAsJsonObject().get("detail"));
		String patientName = cards.get(0).getAsJsonObject().get("detail").getAsString();
		assertEquals("Ashley Madelyn", patientName);

		// Ensure Summary
		assertNotNull(cards.get(1));
		assertNotNull(cards.get(1).getAsJsonObject().get("summary"));
		String recommendation = cards.get(1).getAsJsonObject().get("summary").getAsString();
		assertEquals(
				"HIV Screening Recommended due to patient being at High Risk for HIV and over three months have passed since previous screening.",
				recommendation);

		// Ensure Activity Definition / Suggestions
		assertNotNull(cards.get(1).getAsJsonObject().get("suggestions"));
		JsonArray suggestions = cards.get(1).getAsJsonObject().get("suggestions").getAsJsonArray();
		assertNotNull(suggestions.get(0));
		assertNotNull(suggestions.get(0).getAsJsonObject().get("actions"));
		JsonArray actions = suggestions.get(0).getAsJsonObject().get("actions").getAsJsonArray();
		assertNotNull(actions.get(0));
		assertNotNull(actions.get(0).getAsJsonObject().get("resource"));
		JsonObject suggestionsActivityResource = actions.get(0).getAsJsonObject().get("resource").getAsJsonObject();
		assertNotNull(suggestionsActivityResource.get("resourceType"));
		assertEquals("ServiceRequest", suggestionsActivityResource.get("resourceType").getAsString());
		assertNotNull(suggestionsActivityResource.get("subject"));
		String expectedPatientID = ourPatient.getIdElement().getIdPart();
		String actualPatientID = suggestionsActivityResource.get("subject").getAsJsonObject().get("reference")
				.getAsString();
		assertEquals("Patient/" + expectedPatientID, actualPatientID);
	}

}
