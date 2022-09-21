package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes =
	{ Application.class, CdsHooksConfig.class },
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
class CdsHooksServletIT extends RestIntegrationTest {
	@Autowired
	CdsServicesCache cdsServicesCache;
	private String ourCdsBase;

	@BeforeEach
	void beforeEach() {
		ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
	}

	@Test
	void testGetCdsServices() {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(ourCdsBase);
			request.addHeader("Content-Type", "application/json");
			assertEquals(200, httpClient.execute(request).getStatusLine().getStatusCode());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void testCdsServicesCache() {
		loadTransaction("Screening-bundle-r4.json");
		loadTransaction("HelloWorldPatientView-bundle.json");
		PlanDefinition p1 = (PlanDefinition) loadResource("Screening-plandefinition.json");
		PlanDefinition p2 = (PlanDefinition) loadResource("HelloWorld-plandefinition.json");

		ResourceChangeEvent rce = new ResourceChangeEvent();
		rce.setCreatedResourceIds(Collections.singletonList(p1.getIdElement()));

		cdsServicesCache.clearCache();

		cdsServicesCache.handleChange(rce);
		assertEquals(1, cdsServicesCache.getCdsServiceCache().get().size());

		rce.setCreatedResourceIds(Collections.singletonList(p2.getIdElement()));
		cdsServicesCache.handleChange(rce);
		assertEquals(2, cdsServicesCache.getCdsServiceCache().get().size());

		rce.setCreatedResourceIds(null);
		rce.setDeletedResourceIds(Collections.singletonList(p1.getIdElement()));
		cdsServicesCache.handleChange(rce);
		assertEquals(1, cdsServicesCache.getCdsServiceCache().get().size());

		assertEquals(
			"HelloWorldPatientView",
			cdsServicesCache.getCdsServiceCache().get().get(0).getAsJsonObject().get("name").getAsString());
		PlanDefinition updatedP2 = new PlanDefinition();
		p2.copyValues(updatedP2);
		updatedP2.setName("HelloWorldPatientView-updated");
		update(updatedP2);
		rce.setDeletedResourceIds(null);
		rce.setUpdatedResourceIds(Collections.singletonList(updatedP2.getIdElement()));
		cdsServicesCache.handleChange(rce);
		assertEquals(
			"HelloWorldPatientView-updated",
			cdsServicesCache.getCdsServiceCache().get().get(0).getAsJsonObject().get("name").getAsString());
	}

	@Test
	void testCdsServicesRequest() {
		// Server Load
		loadTransaction("Screening-bundle-r4.json");

		ResourceChangeEvent rce = new ResourceChangeEvent();
		rce.setUpdatedResourceIds(Collections.singletonList(new IdType("plandefinition-Screening")));
		cdsServicesCache.handleChange(rce);

		Patient ourPatient = getClient().read().resource(Patient.class).withId("HighRiskIDUPatient").execute();

		// Update fhirServer Base
		String jsonHooksRequest = stringFromResource("request-HighRiskIDUPatient.json");
		Gson gsonRequest = new Gson();
		JsonObject jsonRequestObject = gsonRequest.fromJson(jsonHooksRequest, JsonObject.class);
		jsonRequestObject.addProperty("fhirServer", getServerBase());

		// Setup Client
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
			assertNotNull(cards.get(1).getAsJsonObject().get("detail"));
			String patientName = cards.get(1).getAsJsonObject().get("detail").getAsString();
			assertEquals("Ashley Madelyn", patientName);

			// Ensure Summary
			assertNotNull(cards.get(0));
			assertNotNull(cards.get(0).getAsJsonObject().get("summary"));
			String recommendation = cards.get(0).getAsJsonObject().get("summary").getAsString();
			assertEquals(
				"HIV Screening Recommended due to patient being at High Risk for HIV and over three months have passed since previous screening.",
				recommendation);

			// Ensure Activity Definition / Suggestions
			assertNotNull(cards.get(0).getAsJsonObject().get("suggestions"));
			JsonArray suggestions = cards.get(0).getAsJsonObject().get("suggestions").getAsJsonArray();
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
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}
}
