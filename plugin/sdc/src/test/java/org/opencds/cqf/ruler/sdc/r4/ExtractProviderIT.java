package org.opencds.cqf.ruler.sdc.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.external.cr.StarterCrR4Config;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
	StarterCrR4Config.class }, properties = { "hapi.fhir.fhir_version=r4", "hapi.fhir.cr.enabled=true" })
class ExtractProviderIT extends RestIntegrationTest {

	@Test
	void testExtract() {
		String examplePatient = "example_patient.json";
		String exampleQuestionnaire = "questionnaire_1559.json";
		String exampleQR = "questionnaire_response_1558.json";

		loadResource(examplePatient);
		loadResource(exampleQuestionnaire);
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) loadResource(exampleQR);

		Parameters params = parameters(
				part("questionnaire-response", questionnaireResponse));

		Bundle actual = getClient()
				.operation()
				.onType(QuestionnaireResponse.class)
				.named("$extract")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(actual);

		// Expecting one observation per item
		assertEquals(5, actual.getEntry().size());

		// Ensure the Observations were saved to the local server
		// The HAPI implementation of $extract does not automatically save the bundle
		// I would question the need for ever doing this.  If you want the resources saved, POST the bundle yourself.
		// It should NOT be done as part of the $extract operation itself.
//		for (Bundle.BundleEntryComponent bec : actual.getEntry()) {
//			assertEquals("201 Created", bec.getResponse().getStatus());
//		}
	}

	@Test
	void testExtract_noQuestionnaireReference_throwsException() {
		QuestionnaireResponse test = (QuestionnaireResponse) getFhirContext().newJsonParser()
				.parseResource(stringFromResource("mypain-questionnaire-response-no-url.json"));

		Parameters params = new Parameters();
		params.addParameter().setName("questionnaireResponse").setResource(test);

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(QuestionnaireResponse.class).named("$extract")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
		});
	}
}
