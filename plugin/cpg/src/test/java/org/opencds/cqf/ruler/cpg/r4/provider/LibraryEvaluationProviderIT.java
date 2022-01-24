package org.opencds.cqf.ruler.cpg.r4.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
		CpgConfig.class }, properties = { "hapi.fhir.fhir_version=r4",
				"spring.main.allow-bean-definition-overriding=true",
				"debug=true",
				"spring.batch.job.enabled=false" })
public class LibraryEvaluationProviderIT extends RestIntegrationTest {
	@Test
	public void testLibraryEvaluationValidationThrows() throws IOException {

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
		params.addParameter().setName("patientId").setValue(null);
		params.addParameter().setName("context").setValue(new StringType("Patient"));

		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "ColorectalCancerScreeningsFHIR.json");
		Library lib = getClient().read().resource(Library.class).withId("ColorectalCancerScreeningsFHIR").execute();
		assertNotNull(lib);

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onInstance(new IdType("Library", "ColorectalCancerScreeningsFHIR"))
					.named("$evaluate")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
		});
	}

	@Test
	public void testLibraryEvaluationValidData() throws IOException {

		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "ColorectalCancerScreeningsFHIR.json");

		String bundleTextValueSets = stringFromResource(
				packagePrefix + "valuesets-ColorectalCancerScreeningsFHIR-bundle.json");
		FhirContext fhirContext = FhirContext.forR4();
		Bundle bundleValueSet = (Bundle) fhirContext.newJsonParser().parseResource(bundleTextValueSets);
		getClient().transaction().withBundle(bundleValueSet).execute();

		String bundleText = stringFromResource(packagePrefix + "additionalData.json");
		Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleText);
		Library lib = getClient().read().resource(Library.class).withId("ColorectalCancerScreeningsFHIR").execute();

		assertNotNull(bundle);
		assertNotNull(lib);

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2019-12-31"));
		params.addParameter().setName("patientId").setValue(new StringType("numer-EXM130"));
		params.addParameter().setName("context").setValue(new StringType("Patient"));
		params.addParameter().setName("additionalData").setResource(bundle);

		Bundle returnBundle = getClient().operation().onInstance(new IdType("Library", "ColorectalCancerScreeningsFHIR"))
				.named("$evaluate")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(returnBundle);
	}

}
