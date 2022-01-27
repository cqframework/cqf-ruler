package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.context.FhirContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { DataOperationProviderIT.class,
	CrConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
public class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	public void testDstu3DataRequirementsOperation() throws IOException {
		String bundleTextValueSets = stringFromResource( "LibraryTransactionBundle.json");
		FhirContext fhirContext = FhirContext.forDstu3();
		Bundle bundleValueSet = (Bundle)fhirContext.newJsonParser().parseResource(bundleTextValueSets);
		getClient().transaction().withBundle(bundleValueSet).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("target").setValue(new StringType("dummy"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Library", "LibraryEvaluationTest"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
	}

}
