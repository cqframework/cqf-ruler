package org.opencds.cqf.ruler.sdc.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.sdc.SDCConfig;
import org.opencds.cqf.ruler.sdc.SDCProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		SDCConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class ExtractProviderIT extends RestIntegrationTest {
	@Autowired
	private SDCProperties mySdcProperties;

	@BeforeEach
	void beforeEach() {
		String ourServerBase = "http://localhost:" + getPort() + "/fhir/";
		mySdcProperties.getExtract().setEndpoint(ourServerBase);
	}

	@Test
	void testExtract() {
		String examplePatient = "example_patient.json";
		String exampleQuestionnaire = "questionnaire_1559.json";
		String exampleQR = "questionnaire_response_1558.json";

		loadResource(examplePatient);
		loadResource(exampleQuestionnaire);
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) loadResource(exampleQR);

		Parameters params = parameters(
				part("questionnaireResponse", questionnaireResponse));

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
		for (Bundle.BundleEntryComponent bec : actual.getEntry()) {
			assertEquals("201 Created", bec.getResponse().getStatus());
		}
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
