package com.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		ConverterConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class ConverterProviderIT extends RestIntegrationTest {
	@Test
	void testConverterConfig() {
		loadResource("ersd-v1-plandefinition-skeleton.json");
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		Parameters v2BundleParams = new Parameters();
		ParametersParameterComponent part = v2BundleParams.addParameter()
			.setName("resource")
			.setResource(v2Bundle);
		Bundle v1Bundle = getClient()
				.operation()
				.onServer()
				.named("$convert-v1")
				.withParameters(v2BundleParams)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(v1Bundle);
	}
}
