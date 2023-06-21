package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.stringPart;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
	CrConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	void testDstu3DataRequirementsOperation() {
		loadTransaction("DataReqLibraryTransactionBundleDstu3.json");

		Parameters params = parameters(stringPart("target", "dummy"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Library", "LibraryEvaluationTest"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);
	}

	@Test
	void testDstu3MeasureDataRequirementsOperation() {
		loadTransaction("Exm105Dstu3MeasureBundle.json");

		Parameters params = parameters(
				stringPart("startPeriod", "2019-01-01"),
				stringPart("endPeriod", "2020-01-01"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM105-FHIR3-8.0.000"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);
	}

}
