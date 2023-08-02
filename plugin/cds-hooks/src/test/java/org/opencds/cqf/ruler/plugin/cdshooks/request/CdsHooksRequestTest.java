package org.opencds.cqf.ruler.plugin.cdshooks.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.annotation.DirtiesContext;

class CdsHooksRequestTest {
	@DirtiesContext
	@Test
	void testFhirAuthExpiresIn() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
		// expires_in as String
		String requestJson = "{\n" +
				"  \"hookInstance\": \"6bc883b2-b795-4dcb-b661-34884a31d472\",\n" +
				"  \"fhirServer\": \"https://cloud.alphora.com/sandbox/r4/cds/fhir\",\n" +
				"  \"hook\": \"patient-view\",\n" +
				"  \"fhirAuthorization\": {\n" +
				"    \"access_token\": \"12345\",\n" +
				"    \"token_type\": \"Bearer\",\n" +
				"    \"expires_in\": \"3600\",\n" +
				"    \"scope\": \"\",\n" +
				"    \"subject\": \"\"\n" +
				"  },\n" +
				"  \"context\": {\n" +
				"    \"userId\": \"Practitioner/example\",\n" +
				"    \"patientId\": \"Patient/example\"\n" +
				"  }\n" +
				"}";
		CdsHooksRequest cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
		assertNotNull(cdsHooksRequest.fhirAuthorization);
		assertEquals(3600, cdsHooksRequest.fhirAuthorization.expiresIn);

		// expires_in as int
		requestJson = "{\n" +
				"  \"hookInstance\": \"6bc883b2-b795-4dcb-b661-34884a31d472\",\n" +
				"  \"fhirServer\": \"https://cloud.alphora.com/sandbox/r4/cds/fhir\",\n" +
				"  \"hook\": \"patient-view\",\n" +
				"  \"fhirAuthorization\": {\n" +
				"    \"access_token\": \"12345\",\n" +
				"    \"token_type\": \"Bearer\",\n" +
				"    \"expires_in\": 3600,\n" +
				"    \"scope\": \"\",\n" +
				"    \"subject\": \"\"\n" +
				"  },\n" +
				"  \"context\": {\n" +
				"    \"userId\": \"Practitioner/example\",\n" +
				"    \"patientId\": \"Patient/example\"\n" +
				"  }\n" +
				"}";

		cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
		assertNotNull(cdsHooksRequest.fhirAuthorization);
		assertEquals(3600, cdsHooksRequest.fhirAuthorization.expiresIn);

		// expires_in as invalid type (double)
		requestJson = "{\n" +
				"  \"hookInstance\": \"6bc883b2-b795-4dcb-b661-34884a31d472\",\n" +
				"  \"fhirServer\": \"https://cloud.alphora.com/sandbox/r4/cds/fhir\",\n" +
				"  \"hook\": \"patient-view\",\n" +
				"  \"fhirAuthorization\": {\n" +
				"    \"access_token\": \"12345\",\n" +
				"    \"token_type\": \"Bearer\",\n" +
				"    \"expires_in\": 3600.45,\n" +
				"    \"scope\": \"\",\n" +
				"    \"subject\": \"\"\n" +
				"  },\n" +
				"  \"context\": {\n" +
				"    \"userId\": \"Practitioner/example\",\n" +
				"    \"patientId\": \"Patient/example\"\n" +
				"  }\n" +
				"}";

		try {
			cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
			fail();
		} catch (JsonProcessingException JPE) {
			// pass
		}
	}
}
