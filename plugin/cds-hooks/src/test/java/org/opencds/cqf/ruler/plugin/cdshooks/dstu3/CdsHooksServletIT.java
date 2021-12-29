package org.opencds.cqf.ruler.plugin.cdshooks.dstu3;

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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.plugin.utility.ResolutionUtilities;
import org.opencds.cqf.ruler.test.ITestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
	CdsHooksConfig.class }, properties = {
	// Override is currently required when using MDM as the construction of the MDM
	// beans are ambiguous as they are constructed multiple places. This is evident
	// when running in a spring boot environment
	"spring.main.allow-bean-definition-overriding=true",
	"spring.batch.job.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:dbdstu3-mt",
	"hapi.fhir.fhir_version=dstu3",
	"hapi.fhir.tester_enabled=false",
	"hapi.fhir.allow_external_references=true",
})
public class CdsHooksServletIT implements ResolutionUtilities,
	org.opencds.cqf.ruler.plugin.utility.ClientUtilities, ITestSupport  {

	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	AppProperties myAppProperties;

	@Autowired
	private DaoRegistry ourRegistry;

	@LocalServerPort
	private int port;

	String ourServerBase;
	String ourCdsBase;

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forCached(FhirVersionEnum.DSTU3);
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);

		ourServerBase = "http://localhost:" + port + "/fhir";
		ourCdsBase = "http://localhost:" + port + "/cds-services";
		myAppProperties.setServer_address(ourServerBase);
		myAppProperties.setCors(new AppProperties.Cors());
		myAppProperties.setAllow_external_references(true);
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}


	@Test
	public void testGetCdsServices() throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet request = new HttpGet(ourCdsBase);
		request.addHeader("Content-Type", "application/json");
		assertEquals(200, httpClient.execute(request).getStatusLine().getStatusCode());
	}

	@Test
	// TODO: Add Opioid Tests once $apply-cql is implemented.
	public void testCdsServicesRequest() throws IOException {
		// Server Load
		loadTransaction("HelloWorldPatientView-bundle.json", ourCtx, ourRegistry);
		loadResource("hello-world-patient-view-patient.json", ourCtx, ourRegistry);
		Patient ourPatient = ourClient.read().resource(Patient.class).withId("patient-hello-world-patient-view").execute();
		assertNotNull(ourPatient);
		assertEquals("patient-hello-world-patient-view", ourPatient.getIdElement().getIdPart());
		PlanDefinition ourPlanDefinition = ourClient.read().resource(PlanDefinition.class).withId("hello-world-patient-view").execute();
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
			getPlanDefinitions = ourClient.search().forResource(PlanDefinition.class).returnBundle(Bundle.class).execute();
		} while(getPlanDefinitions.getEntry().size() == 0 && tries < 15);
		assertTrue(getPlanDefinitions.hasEntry());

		// Update fhirServer Base
		String jsonHooksRequest = stringFromResource("request-HelloWorld.json");
		Gson gsonRequest = new Gson();
		JsonObject jsonRequestObject = gsonRequest.fromJson(jsonHooksRequest, JsonObject.class);
		jsonRequestObject.addProperty("fhirServer", ourServerBase);

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
