package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { DataOperationProviderIT.class,
	CrConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
public class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	public void testDstu3DataRequirementsOperation() throws IOException {
		String bundleAsText = stringFromResource( "DataRequirementsTransactionBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("target").setValue(new StringType("dummy"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Library", "LibraryEvaluationTest"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
	}

	@Disabled("Erroring because could not convert r5 Library to dstu3")
	@Test
	public void testDstu3MeasureDataRequirementsOperation() throws IOException {
		String bundleAsText = stringFromResource( "Exm105Dstu3MeasureBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("startPeriod").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("endPeriod").setValue(new StringType("2020-01-01"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Measure", "measure-EXM105-FHIR3-8.0.000"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
	}

}
