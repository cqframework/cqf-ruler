package org.opencds.cqf.ruler.plugin.cdshooks.dstu3;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.plugin.cdshooks.ResourceChangeEvent;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { Application.class, CdsHooksConfig.class },
		properties = {"hapi.fhir.fhir_version=dstu3", "hapi.fhir.security.basic_auth.enabled=false"})
class CdsHooksServletIT extends RestIntegrationTest {
	@Autowired
	CdsServicesCache cdsServicesCache;
	private String ourCdsBase;

	@BeforeEach
	void beforeEach() {
		ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
	}

	@Test
	void testGetCdsServices() throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet request = new HttpGet(ourCdsBase);
		request.addHeader("Content-Type", "application/json");
		assertEquals(200, httpClient.execute(request).getStatusLine().getStatusCode());
	}

	@Test
	void testCdsServicesRequest() throws IOException {
		// Server Load
		loadTransaction("HelloWorldPatientView-bundle.json");
		loadResource("hello-world-patient-view-patient.json");

		ResourceChangeEvent rce = new ResourceChangeEvent();
		rce.setUpdatedResourceIds(Collections.singletonList(new IdType("hello-world-patient-view")));
		cdsServicesCache.handleChange(rce);

		Patient ourPatient = getClient().read().resource(Patient.class).withId("patient-hello-world-patient-view")
				.execute();
		assertNotNull(ourPatient);
		assertEquals("patient-hello-world-patient-view", ourPatient.getIdElement().getIdPart());
		PlanDefinition ourPlanDefinition = getClient().read().resource(PlanDefinition.class)
				.withId("hello-world-patient-view").execute();
		assertNotNull(ourPlanDefinition);
		Bundle getPlanDefinitions;
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
		String jsonHooksRequest = stringFromResource("request-HelloWorld.json");
		Gson gsonRequest = new Gson();
		JsonObject jsonRequestObject = gsonRequest.fromJson(jsonHooksRequest, JsonObject.class);
		jsonRequestObject.addProperty("fhirServer", getServerBase());

		// Setup Client
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost request = new HttpPost(ourCdsBase + "/hello-world-patient-view");
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
		assertEquals("The CDS Service is alive and communicating successfully!", patientName);

		// Ensure Summary
		assertNotNull(cards.get(0));
		assertNotNull(cards.get(0).getAsJsonObject().get("summary"));
		String summary = cards.get(0).getAsJsonObject().get("summary").getAsString();
		assertEquals("Hello World!", summary);

		// Ensure Activity Definition / Suggestions
		assertNotNull(cards.get(0).getAsJsonObject().get("suggestions"));
		JsonArray suggestions = cards.get(0).getAsJsonObject().get("suggestions").getAsJsonArray();
		assertNotNull(suggestions.get(0));
		assertNotNull(suggestions.get(0).getAsJsonObject().get("actions"));
		JsonArray actions = suggestions.get(0).getAsJsonObject().get("actions").getAsJsonArray();
		assertNotNull(actions.get(0));
		assertNotNull(actions.get(0).getAsJsonObject().get("description"));
		String suggestionsDescription = actions.get(0).getAsJsonObject().get("description").getAsString();
		assertEquals("The CDS Service is alive and communicating successfully!", suggestionsDescription);
	}

}
