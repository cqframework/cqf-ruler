package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.LibraryLoaderCache;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DirtiesContext
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
	CdsHooksConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class OpioidCDSRec10IT extends RestIntegrationTest {

	@Autowired
	LibraryLoaderCache libraryLoaderCache;

	@Autowired
	CdsServicesCache cdsServicesCache;
	private String ourCdsBase;

	private Bundle rec10ArtifactBundle;
	private final FhirContext fhirContext = FhirContext.forR4Cached();

	@BeforeAll
	void beforeAll() throws InterruptedException {
		ourCdsBase = "http://localhost:" + getPort() + "/cds-services";

		// Load artifacts
		loadTransaction("opioidcds-10-order-sign-bundle.json");
		rec10ArtifactBundle = (Bundle) readResource("opioidcds-10-order-sign-bundle.json");

		// Initialize Library and PlanDefinition caches
		libraryLoaderCache.handleInit(Arrays.asList(
			new IdType("Library/FHIRHelpers"),
			new IdType("Library/OpioidCDSCommonConfig"),
			new IdType("Library/OpioidCDSRoutines"),
			new IdType("Library/OMTKData2020"),
			new IdType("Library/OMTKLogicMK2020"),
			new IdType("Library/OpioidCDSCommon"),
			new IdType("Library/OpioidCDSREC10Common"),
			new IdType("Library/OpioidCDSREC10OrderSign")
		));
		cdsServicesCache.handleInit(Collections.singletonList(
			new IdType("PlanDefinition/opioidcds-10-order-sign")
		));
	}

	@Test
	void recommendUDS() {
		var patient = generatePatient();
		update(patient);

		var draftOrder = generateDraftOrder();
		var bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(draftOrder);
		var request = createRequest(patient, fhirContext.newJsonParser().encodeResourceToString(bundle));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Consider the Benefits and Risks of Conducting a Urine Toxicology Screen", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertEquals(
				"Consider the benefits and risks of toxicology testing to assess for prescribed medications as well as other prescribed and nonprescribed controlled substances.\r\n\r\nFor guidance regarding utilizing toxicology tests when prescribing opioids, see [Recommendation 10 of the 2022 CDC Clinical Practice Guideline](https://www.cdc.gov/mmwr/volumes/71/rr/rr7103a1.htm#Recommendation10).",
				card.get("detail").getAsString());
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void unexpectedPositiveCocaine() {
		var patient = generatePatient();
		update(patient);
		var lab = generateLab(patient, "cocaine-urine-drug-screening-tests", 360, 2, true);
		var labCodeDetail = lab.getCode().getCodingFirstRep().getDisplay();
		update(lab);

		var draftOrder = generateDraftOrder();
		var bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(draftOrder);
		var request = createRequest(patient, fhirContext.newJsonParser().encodeResourceToString(bundle));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected substance found: " + labCodeDetail));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void unexpectedPositiveOxycodone() {
		var patient = generatePatient();
		update(patient);
		var lab = generateLab(patient, "oxycodone-urine-drug-screening-tests", 360, 10, true);
		var labCodeDetail = lab.getCode().getCodingFirstRep().getDisplay();
		update(lab);

		var draftOrder = generateDraftOrder();
		var bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(draftOrder);
		var request = createRequest(patient, fhirContext.newJsonParser().encodeResourceToString(bundle));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected substance found: " + labCodeDetail));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void unexpectedNegativeOxycodone() {
		var patient = generatePatient();
		update(patient);
		var lab = generateLab(patient, "oxycodone-urine-drug-screening-tests", 20, 10, false);
		update(lab);
		var order = generateOrder(patient, "oxycodone-medications", 30, 21);
		var orderCodeDetail = order.getMedicationCodeableConcept().getCodingFirstRep().getDisplay();
		update(order);

		var draftOrder = generateDraftOrder();
		var bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(draftOrder);
		var request = createRequest(patient, fhirContext.newJsonParser().encodeResourceToString(bundle));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected negative result found: prescribed " + orderCodeDetail));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void epicUnexpectedNegative() {
		var patient = generatePatient();
		patient.setId("ejwbAxeb2soWEsIm6r4GJnw3");
		update(patient);
		loadTransaction("opioidcds-10-order-sign-patient-data-bundle.json");

		var draftOrder = generateDraftOrder();
		var bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(draftOrder);
		var request = createRequest(patient, fhirContext.newJsonParser().encodeResourceToString(bundle));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected negative result found: prescribed oxyCODONE (OxyCONTIN) 12 hr tablet"));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void epicUnexpectedNegativeWithPrefetch() {
		var patient = generatePatient();
		patient.setId("eOTWcfBK0B.0nnWNAZ-rrxA3");
		var prefetchBundle = fhirContext.newJsonParser().parseResource(stringFromResource("opioidcds-10-order-sign-patient-data-bundle-2.json"));

		var draftOrder = generateEpicDraftOrder();
		var request = createRequestWithPrefetch(patient, fhirContext.newJsonParser().encodeResourceToString(draftOrder), (Bundle) prefetchBundle);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected negative result found: prescribed fentaNYL (DURAGESIC) patch"));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void epicUnexpectedNegativeWithPrefetch2() {
		var prefetchBundle = fhirContext.newJsonParser().parseResource(stringFromResource("opioidcds-10-order-sign-patient-data-bundle-3.json"));

		var draftOrder = generateEpicDraftOrder2();
		var request = createRequestWithPrefetchAllData(fhirContext.newJsonParser().encodeResourceToString(draftOrder), (Bundle) prefetchBundle);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			var serviceRequest = new HttpPost(ourCdsBase + "/opioidcds-10-order-sign");
			serviceRequest.setEntity(new StringEntity(request));
			serviceRequest.addHeader("Content-Type", "application/json");

			var response = httpClient.execute(serviceRequest);
			var serviceResponse = EntityUtils.toString(response.getEntity());

			var gson = new Gson();
			var cdsHooksResponse = gson.fromJson(serviceResponse, JsonObject.class);
			assertTrue(cdsHooksResponse.has("cards"));
			var cards = cdsHooksResponse.getAsJsonArray("cards");
			assertFalse(cards.isEmpty());
			var card = cards.get(0).getAsJsonObject();
			assertTrue(card.has("summary"));
			assertEquals("Patient May Have Unexpected Toxicology Test Results", card.get("summary").getAsString());
			assertTrue(card.has("detail"));
			assertTrue(card.get("detail").getAsString().contains("Possible unexpected negative result found: prescribed fentaNYL (DURAGESIC) patch"));
			assertTrue(card.has("indicator"));
			assertEquals("warning", card.get("indicator").getAsString());
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	private String createRequestWithPrefetchAllData(String draftOrders, Bundle prefetchBundle) {
		var gson = new Gson();
		var request = new JsonObject();
		request.addProperty("hookInstance", "2128f180-452c-11ef-83bc-450207e71723");
		request.addProperty("fhirServer", "https://fhirnp.ynhh.org/FHIR_POC/api/FHIR/R4");
		request.addProperty("hook", "order-sign");

		var context = new JsonObject();
		context.addProperty("patientId", "eOTWcfBK0B.0nnWNAZ-rrxA3");
		context.addProperty("encounterId", "eJLoNtVFzxi-c-g3dv7ZzZQ3");
		context.addProperty("userId", "PractitionerRole/er1oqZcJl6k2MFrIJ59y-TA3");
		context.add("draftOrders", gson.fromJson(draftOrders, JsonObject.class));
		request.add("context", context);

		var prefetch = new JsonObject();
		Map<String, List<IBaseResource>> prefetchResources = new HashMap<>();
		BundleUtil.toListOfResources(fhirContext, prefetchBundle).forEach(
			res -> {
				if (prefetchResources.containsKey(res.fhirType())) {
					prefetchResources.get(res.fhirType()).add(res);
				} else {
					List<IBaseResource> newList = new ArrayList<>();
					newList.add(res);
					prefetchResources.put(res.fhirType(), newList);
				}
			}
		);

		AtomicInteger count = new AtomicInteger(1);
		prefetchResources.forEach((key, value) -> {
            var item = "item" + count.getAndIncrement();
            if (value.size() > 1) {
                var bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
                value.forEach(res -> bundle.addEntry().setResource((Resource) res));
                prefetch.add(item, gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(bundle), JsonObject.class));
            } else {
                prefetch.add(item, gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(value.get(0)), JsonObject.class));
            }
        });
		request.add("prefetch", prefetch);
		var extension = new JsonObject();
		extension.add("com.epic.cdshooks.request.bpa-trigger-action", new JsonPrimitive("23"));
		extension.add("com.epic.cdshooks.request.cds-hooks-specification-version", new JsonPrimitive("1.1"));
		extension.add("com.epic.cdshooks.request.fhir-version", new JsonPrimitive("R4"));
		extension.add("com.epic.cdshooks.request.criteria-id", new JsonPrimitive("8883611"));
		extension.add("com.epic.cdshooks.request.epic-version", new JsonPrimitive("10.9"));
		extension.add("com.epic.cdshooks.request.cds-hooks-implementation-version", new JsonPrimitive("1.8"));
		extension.add("com.epic.cdshooks.request.cds-hooks-su-version", new JsonPrimitive("0"));
		request.add("extension", extension);
		return gson.toJson(request);
	}

	private String createRequestWithPrefetch(Patient patient, String draftOrders, Bundle prefetchResource) {
		var gson = new Gson();
		var request = new JsonObject();
		request.addProperty("hookInstance", getRandomId());
		request.addProperty("fhirServer", "https://fhirnp.ynhh.org/FHIR_POC/api/FHIR/R4");
		request.addProperty("hook", "order-sign");
		var context = new JsonObject();
		context.addProperty("patientId", patient.getIdPart());
		context.addProperty("encounterId", "euzL35B6clUhMfIM2FSD.vg3");
		context.addProperty("userId", "PractitionerRole/er1oqZcJl6k2MFrIJ59y-TA3");
		context.add("draftOrders", gson.fromJson(draftOrders, JsonObject.class));
		request.add("context", context);
		var prefetch = new JsonObject();
		prefetch.add("item1", gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(patient), JsonObject.class));
		var mrBundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
		for (var mr : BundleUtil.toListOfResourcesOfType(fhirContext, prefetchResource, MedicationRequest.class)) {
			mrBundle.addEntry().setResource(mr);
		}
		prefetch.add("item2", gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(mrBundle), JsonObject.class));
		var labBundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
		for (var lab : BundleUtil.toListOfResourcesOfType(fhirContext, prefetchResource, Observation.class)) {
			labBundle.addEntry().setResource(lab);
		}
		prefetch.add("item3", gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(labBundle), JsonObject.class));
		var medBundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
		for (var med : BundleUtil.toListOfResourcesOfType(fhirContext, prefetchResource, Medication.class)) {
			medBundle.addEntry().setResource(med);
		}
		prefetch.add("item4", gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(medBundle), JsonObject.class));
		for (var oo : BundleUtil.toListOfResourcesOfType(fhirContext, prefetchResource, OperationOutcome.class)) {
			prefetch.add("item5", gson.fromJson(fhirContext.newJsonParser().encodeResourceToString(oo), JsonObject.class));
		}
		request.add("prefetch", prefetch);
		var extension = new JsonObject();
		extension.add("com.epic.cdshooks.request.bpa-trigger-action", new JsonPrimitive("23"));
		extension.add("com.epic.cdshooks.request.cds-hooks-specification-version", new JsonPrimitive("1.1"));
		extension.add("com.epic.cdshooks.request.fhir-version", new JsonPrimitive("R4"));
		extension.add("com.epic.cdshooks.request.criteria-id", new JsonPrimitive("8883611"));
		extension.add("com.epic.cdshooks.request.epic-version", new JsonPrimitive("10.9"));
		extension.add("com.epic.cdshooks.request.cds-hooks-implementation-version", new JsonPrimitive("1.8"));
		extension.add("com.epic.cdshooks.request.cds-hooks-su-version", new JsonPrimitive("0"));
		request.add("extension", extension);
		return gson.toJson(request);
	}

	private String createRequest(Patient patient, String draftOrders) {
		var gson = new Gson();
		var request = new JsonObject();
		request.addProperty("hookInstance", getRandomId());
		request.addProperty("fhirServer", getServerBase());
		request.addProperty("hook", "order-sign");
		var context = new JsonObject();
		context.addProperty("patientId", patient.getIdPart());
		context.add("draftOrders", gson.fromJson(draftOrders, JsonObject.class));
		request.add("context", context);
		return gson.toJson(request);
	}

	private MedicationRequest generateOrder(Patient patient, String valueSetId, int lookbackStart, int lookbackEnd) {
		var order = new MedicationRequest();
		order.setId(getRandomId());

		var calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, Math.negateExact(getRandom(lookbackEnd, lookbackStart)));

		order.setAuthoredOn(calendar.getTime());
		order.setMedication(getCodeableConceptFromValueSet(valueSetId));
		order.setSubject(new Reference(patient.getIdElement()));
		order.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
		order.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
		var communityCoding = new Coding().setCode("community").setSystem("http://terminology.hl7.org/CodeSystem/medicationrequest-category");
		order.addCategory().setCoding(Collections.singletonList(communityCoding));
		var duration = new Duration();
		duration.setValue(30).setUnit("days").setSystem("http://unitsofmeasure.org").setCode("d");
		order.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent().setExpectedSupplyDuration(duration));
		return order;
	}

	private Bundle generateEpicDraftOrder2() {
		var bundle = new Bundle().setType(Bundle.BundleType.COLLECTION);
		var order = fhirContext.newJsonParser().parseResource("{\"resourceType\":\"MedicationRequest\",\"id\":\"eBOePB3LqyVnSE1-qMQzbiJcwRelfhvQh89ifm3Ws.kEvACUuYKqEQimkJLHwXSWIXyOEeksWa-UkP54dfm99BL31L9xxF0Uol6DC6Q8-Qlr3mjaIpoB7Emp3VlBVtSY-V92eqDJ-b-3r9HmGMz-zsyBpXhc9NqLhOZOb7AYuLN-8L2Lmv6wJJbcvpGuWap5q8SVSg.347SslDJprfegxvw3\",\"status\":\"draft\",\"intent\":\"order\",\"category\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/medicationrequest-category\",\"code\":\"community\",\"display\":\"Community\"}],\"text\":\"Community\"}],\"medicationReference\":{\"reference\":\"Medication/eNRH.0i4FewMpi87mxShENw3\",\"display\":\"OXYCODONE ER 80 MG TABLET,CRUSH RESISTANT,EXTENDED RELEASE 12 HR\"},\"subject\":{\"reference\":\"Patient/eOTWcfBK0B.0nnWNAZ-rrxA3\",\"display\":\"Zzzunexp, NegAmbNine\"},\"encounter\":{\"reference\":\"Encounter/eJLoNtVFzxi-c-g3dv7ZzZQ3\",\"identifier\":{\"use\":\"usual\",\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.3.698084.8\",\"value\":\"80437322\"},\"display\":\"Office Visit\"},\"requester\":{\"reference\":\"Practitioner/edVgNmZO8PsofzicgHOR39A3\",\"type\":\"Practitioner\",\"display\":\"Physician Family Medicine, MD\"},\"recorder\":{\"reference\":\"Practitioner/edVgNmZO8PsofzicgHOR39A3\",\"type\":\"Practitioner\",\"display\":\"Physician Family Medicine, MD\"},\"reasonCode\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"161891005\",\"display\":\"Backache (finding)\"},{\"system\":\"http://hl7.org/fhir/sid/icd-9-cm\",\"code\":\"724.5\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"},{\"system\":\"http://hl7.org/fhir/sid/icd-10-cm\",\"code\":\"M54.9\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"text\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"reasonReference\":[{\"reference\":\"Condition/e73cVLsYj2KrmwISjiinLRvYM-OdhbjXMY42B-06t.IVsnRJZOuMET0L4cM5nNKwfieG4MvvKe54LmlYqVa-zpg3\",\"type\":\"Condition\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"dosageInstruction\":[{\"text\":\"Take 1 tablet (80 mg total) by mouth every 12 (twelve) hours.\",\"patientInstruction\":\"Take 1 tablet (80 mg total) by mouth every 12 (twelve) hours.\",\"timing\":{\"repeat\":{\"boundsPeriod\":{\"start\":\"2024-07-18T04:00:00Z\"},\"count\":60,\"frequency\":1,\"period\":12,\"periodUnit\":\"h\"},\"code\":{\"text\":\"every 12 (twelve) hours\"}},\"route\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"26643006\",\"display\":\"Oral route (qualifier value)\"},{\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.4.798268.7025\",\"code\":\"15\",\"display\":\"Oral\"}],\"text\":\"Oral\"},\"method\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"419652001\",\"display\":\"Take\"}],\"text\":\"Take\"},\"doseAndRate\":[{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"calculated\",\"display\":\"calculated\"}],\"text\":\"calculated\"},\"doseQuantity\":{\"value\":80,\"unit\":\"mg\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"mg\"}},{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"admin-amount\",\"display\":\"admin-amount\"}],\"text\":\"admin-amount\"},\"doseQuantity\":{\"value\":1,\"unit\":\"tablet\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"{tbl}\"}},{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"ordered\",\"display\":\"ordered\"}],\"text\":\"ordered\"},\"doseQuantity\":{\"value\":80,\"unit\":\"mg\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"mg\"}}]}],\"dispenseRequest\":{\"validityPeriod\":{\"start\":\"2024-07-18T04:00:00Z\",\"end\":\"2024-08-17T04:00:00Z\"},\"numberOfRepeatsAllowed\":0,\"quantity\":{\"value\":60,\"unit\":\"tablet\"},\"expectedSupplyDuration\":{\"value\":30,\"unit\":\"Day\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"d\"}}}");
		var med = fhirContext.newJsonParser().parseResource("{\"resourceType\":\"Medication\",\"id\":\"eNRH.0i4FewMpi87mxShENw3\",\"identifier\":[{\"use\":\"usual\",\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.2.698288\",\"value\":\"173935\"}],\"code\":{\"coding\":[{\"system\":\"http://www.whocc.no/atc\",\"code\":\"N02AA05\"},{\"system\":\"urn:oid:2.16.840.1.113883.6.208\",\"code\":\"72868\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"7804\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"82063\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"159821\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1860148\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218129\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218986\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218987\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"219160\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"219740\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"352858\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"541043\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"541047\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"602169\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"607620\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"790841\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1113310\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1664443\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1944530\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1049601\"}],\"text\":\"oxyCODONE (OxyCONTIN) 12 hr tablet\"},\"form\":{\"coding\":[{\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.4.698288.310\",\"code\":\"TR12\",\"display\":\"tablet,oral only,ext.rel.12 hr\"}],\"text\":\"tablet,oral only,ext.rel.12 hr\"},\"ingredient\":[{\"itemCodeableConcept\":{\"coding\":[{\"system\":\"http://www.whocc.no/atc\",\"code\":\"N02AA05\"},{\"system\":\"urn:oid:2.16.840.1.113883.6.208\",\"code\":\"72868\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"7804\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"82063\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"159821\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1860148\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218129\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218986\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"218987\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"219160\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"219740\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"352858\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"541043\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"541047\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"602169\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"607620\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"790841\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1113310\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1664443\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1944530\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1049601\"}],\"text\":\"oxyCODONE (OxyCONTIN) 12 hr tablet\"},\"strength\":{\"numerator\":{\"value\":80,\"unit\":\"mg\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"mg\"},\"denominator\":{\"value\":80,\"unit\":\"mg\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"mg\"}}}]}");
		bundle.addEntry().setResource((Resource) order);
		bundle.addEntry().setResource((Resource) med);
		return bundle;
	}

	private Bundle generateEpicDraftOrder() {
		var bundle = new Bundle().setType(Bundle.BundleType.COLLECTION);
		var order = fhirContext.newJsonParser().parseResource("{\"resourceType\":\"MedicationRequest\",\"id\":\"eBOePB3LqyVnSE1-qMQzbiOhpo9ds.adwCfD7kqF-EQ1lRaQbkRrBHYvST3drcgFFK3Qk2S-DydBf0vkoGyc7B1fv9B-w5.YJQIFXDAQy98ZQwT5utsMufOc5TAVEfHkvlDjj8khnC8kNlhctQLE3IOHuy95aM9sgDNYi1smWP4ZjaIwGWuqhd5bjiWTfavLOsh4oHVo8xq2Lp0ya0rIXNA3\",\"status\":\"draft\",\"intent\":\"order\",\"category\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/medicationrequest-category\",\"code\":\"community\",\"display\":\"Community\"}],\"text\":\"Community\"}],\"medicationReference\":{\"reference\":\"Medication/eyl6Lec3v4ru2gLNoY6SEkA3\",\"display\":\"FENTANYL 12 MCG/HR TRANSDERMAL PATCH\"},\"subject\":{\"reference\":\"Patient/eOTWcfBK0B.0nnWNAZ-rrxA3\",\"display\":\"Zzzunexp, NegAmbFive\"},\"encounter\":{\"reference\":\"Encounter/euzL35B6clUhMfIM2FSD.vg3\",\"identifier\":{\"use\":\"usual\",\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.3.698084.8\",\"value\":\"80436358\"},\"display\":\"Office Visit\"},\"requester\":{\"reference\":\"Practitioner/edVgNmZO8PsofzicgHOR39A3\",\"type\":\"Practitioner\",\"display\":\"Physician Family Medicine, MD\"},\"recorder\":{\"reference\":\"Practitioner/edVgNmZO8PsofzicgHOR39A3\",\"type\":\"Practitioner\",\"display\":\"Physician Family Medicine, MD\"},\"reasonCode\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"161891005\",\"display\":\"Backache (finding)\"},{\"system\":\"http://hl7.org/fhir/sid/icd-9-cm\",\"code\":\"724.5\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"},{\"system\":\"http://hl7.org/fhir/sid/icd-10-cm\",\"code\":\"M54.9\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"text\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"reasonReference\":[{\"reference\":\"Condition/eMUoKGnb.9bg9hecXqnVdjEqB5AVoITr35K-eHqsc1nSnmkZh5mrQaeoF38skrUkgE.1hYR.t8dPFpzWUjGPZ5w3\",\"type\":\"Condition\",\"display\":\"Back pain, unspecified back location, unspecified back pain laterality, unspecified chronicity\"}],\"dosageInstruction\":[{\"text\":\"Place 1 patch onto the skin every third day.\",\"patientInstruction\":\"Place 1 patch onto the skin every third day.\",\"timing\":{\"repeat\":{\"boundsPeriod\":{\"start\":\"2024-07-09T04:00:00Z\"},\"count\":10,\"frequency\":1,\"period\":72,\"periodUnit\":\"h\"},\"code\":{\"text\":\"every 72 hours\"}},\"route\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"45890007\",\"display\":\"Transdermal route (qualifier value)\"},{\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.4.798268.7025\",\"code\":\"20\",\"display\":\"Transdermal\"}],\"text\":\"Transdermal\"},\"doseAndRate\":[{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"calculated\",\"display\":\"calculated\"}],\"text\":\"calculated\"},\"doseQuantity\":{\"value\":1,\"unit\":\"patch\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"{patch}\"}},{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"admin-amount\",\"display\":\"admin-amount\"}],\"text\":\"admin-amount\"},\"doseQuantity\":{\"value\":1,\"unit\":\"patch\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"{patch}\"}},{\"type\":{\"coding\":[{\"system\":\"http://epic.com/CodeSystem/dose-rate-type\",\"code\":\"ordered\",\"display\":\"ordered\"}],\"text\":\"ordered\"},\"doseQuantity\":{\"value\":1,\"unit\":\"patch\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"{patch}\"}}]}],\"dispenseRequest\":{\"validityPeriod\":{\"start\":\"2024-07-09T04:00:00Z\",\"end\":\"2024-08-08T04:00:00Z\"},\"numberOfRepeatsAllowed\":0,\"quantity\":{\"value\":10,\"unit\":\"patch\"},\"expectedSupplyDuration\":{\"value\":30,\"unit\":\"Day\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"d\"}}}");
		var med = fhirContext.newJsonParser().parseResource("{\"resourceType\":\"Medication\",\"id\":\"eyl6Lec3v4ru2gLNoY6SEkA3\",\"identifier\":[{\"use\":\"usual\",\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.2.698288\",\"value\":\"41382\"}],\"code\":{\"coding\":[{\"system\":\"http://www.whocc.no/atc\",\"code\":\"N02AB03\"},{\"system\":\"urn:oid:2.16.840.1.113883.6.208\",\"code\":\"59102\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"4337\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"142436\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1652097\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"577057\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"4336\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"151678\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1237051\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1487612\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"583490\"}],\"text\":\"fentaNYL (DURAGESIC) patch\"},\"form\":{\"coding\":[{\"system\":\"urn:oid:1.2.840.114350.1.13.301.3.7.4.698288.310\",\"code\":\"PT72\",\"display\":\"Patch 72 hr\"}],\"text\":\"Patch 72 hr\"},\"ingredient\":[{\"itemCodeableConcept\":{\"coding\":[{\"system\":\"http://www.whocc.no/atc\",\"code\":\"N02AB03\"},{\"system\":\"urn:oid:2.16.840.1.113883.6.208\",\"code\":\"59102\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"4337\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"142436\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1652097\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"577057\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"4336\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"151678\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1237051\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"1487612\"},{\"system\":\"http://www.nlm.nih.gov/research/umls/rxnorm\",\"code\":\"583490\"}],\"text\":\"fentaNYL (DURAGESIC) patch\"},\"strength\":{\"numerator\":{\"value\":12,\"unit\":\"mcg/hr\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"ug/h\"},\"denominator\":{\"value\":12,\"unit\":\"mcg/hr\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"ug/h\"}}}]}");
		bundle.addEntry().setResource((Resource) order);
		bundle.addEntry().setResource((Resource) med);
		return bundle;
	}

	private MedicationRequest generateDraftOrder() {
		var draftOrder = new MedicationRequest();
		draftOrder.setId(getRandomId());
		draftOrder.setAuthoredOn(new Date());
		draftOrder.setMedication(getCodeableConceptFromValueSet("opioid-analgesics-with-ambulatory-misuse-potential"));
		draftOrder.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
		draftOrder.setStatus(MedicationRequest.MedicationRequestStatus.DRAFT);
		var communityCoding = new Coding().setCode("community").setSystem("http://terminology.hl7.org/CodeSystem/medicationrequest-category");
		draftOrder.addCategory().setCoding(Collections.singletonList(communityCoding));
		var duration = new Duration();
		duration.setValue(30).setUnit("days").setSystem("http://unitsofmeasure.org").setCode("d");
		draftOrder.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent().setExpectedSupplyDuration(duration));
		return draftOrder;
	}

	private Patient generatePatient() {
		var patient = new Patient();
		patient.setId(getRandomId());

		var calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.YEAR, Math.negateExact(getRandom(18, 100)));
		patient.setBirthDate(calendar.getTime());

		var genders = new Enumerations.AdministrativeGender[]{
			Enumerations.AdministrativeGender.FEMALE, Enumerations.AdministrativeGender.MALE};

		patient.setGender(genders[getRandom(0, 1)]);

		return patient;
	}

	private Observation generateLab(Patient patient, String valueSetId, int lookbackStart, int lookbackEnd, boolean result) {
		var lab = new Observation();
		lab.setId(getRandomId());
		lab.addCategory().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/observation-category").setCode("laboratory");
		lab.setStatus(Observation.ObservationStatus.FINAL);
		lab.setCode(getCodeableConceptFromValueSet(valueSetId));
		lab.setSubject(new Reference(patient.getIdElement()));

		var calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, Math.negateExact(getRandom(lookbackEnd, lookbackStart)));
		lab.setEffective(new DateTimeType(calendar));

		lab.setValue(new StringType(result ? "POS" : "NEG"));
		return lab;
	}

	private String getRandomId() {
		return UUID.randomUUID().toString();
	}

	private int getRandom(int min, int max) {
		var range = (max - min) + 1;
      return (int) ((range * Math.random()) + min);
	}

	private CodeableConcept getCodeableConceptFromValueSet(String valueSetId) {
		var valueSetList = BundleUtil.toListOfResourcesOfType(fhirContext, rec10ArtifactBundle, ValueSet.class);
		var match = valueSetList.stream().filter(vs -> vs.getIdPart().equals(valueSetId)).findFirst().orElseThrow();
		var concept = match.getExpansion().getContains().get(getRandom(0, match.getExpansion().getContains().size() - 1));
		var codeableConcept = new CodeableConcept();
		codeableConcept.addCoding().setSystem(concept.getSystem()).setCode(concept.getCode()).setDisplay(concept.getDisplay());
		return codeableConcept;
	}
}
