package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { DataOperationProviderIT.class,
		CrConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	void testR4LibraryDataRequirementsOperation() {
		loadTransaction("DataReqLibraryTransactionBundleR4.json");

		Parameters params = parameters(stringPart("target", "dummy"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Library", "LibraryEvaluationTest"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);

		Library returnLibrary2 = getClient().operation()
				.onInstance(new IdType("Library", "pertinencetag"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary2);
		assertNotNull(returnLibrary2.getDataRequirement().get(1).getExtensionByUrl(
				"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-pertinence"));
	}

	@Test
	void testR4LibraryDataRequirementsNonManifestMultiVersionOperation() {
		loadTransaction("DataReqLibraryTransactionBundleMultiVersionR4.json");

		Library library = getClient().read().resource(Library.class).withId("LibraryEvaluationTest").execute();
		assertNotNull(library);
		assertEquals("http://fhir.org/guides/cqf/common/Library/LibraryEvaluationTestDependency|1.0.000",
				library.getRelatedArtifact().get(0).getResource());

		Library library2 = getClient().read().resource(Library.class).withId("LibraryEvaluationTest2").execute();
		assertNotNull(library2);
		assertEquals("http://fhir.org/guides/cqf/common/Library/LibraryEvaluationTestDependency",
				library2.getRelatedArtifact().get(0).getResource());

		assertEquals(library.getUrl(), library2.getUrl());

		Parameters params = parameters(stringPart("target", "dummy"));

		Library returnLibrary1 = getClient().operation()
				.onInstance(new IdType("Library", "LibraryEvaluationTest"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary1);

		assertTrue(returnLibrary1.getRelatedArtifact().stream().anyMatch(
				x -> (x.getType().toString().equals("DEPENDSON") &&
						x.getResource().equals("Library/LibraryEvaluationTestDependency|1.0.000"))));

		Library returnLibrary2 = getClient().operation()
				.onInstance(new IdType("Library", "LibraryEvaluationTest2"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary2);

		assertTrue(returnLibrary2.getRelatedArtifact().stream().anyMatch(
				x -> (x.getType().toString().equals("DEPENDSON") &&
						x.getResource().equals("Library/LibraryEvaluationTestDependency|2.0.000"))));
	}

	@Test
	void testR4LibraryFhirQueryPattern() {
		loadTransaction("ExmLogicTransactionBundle.json");

		Parameters params = parameters(stringPart("target", "dummy"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Library", "EXMLogic"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			switch (dr.getType()) {
				case "Patient": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					assertEquals("Patient?_id={{context.patientId}}", query);
				}
					break;

				case "Encounter": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					if (dr.hasCodeFilter()) {
						assertEquals(
								"Encounter?subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292",
								query);
					} else {
						assertEquals("Encounter?subject=Patient/{{context.patientId}}", query);
					}
				}
					break;

				case "Coverage": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					assertEquals(
							"Coverage?beneficiary=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591",
							query);
				}
					break;
			}
		}
	}

	@Test
	void testR4MeasureDataRequirementsOperation() {
		loadTransaction("Exm104FhirR4MeasureBundle.json");

		Parameters params = parameters(
				stringPart("startPeriod", "2019-01-01"),
				stringPart("endPeriod", "2020-01-01"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);
	}

	@Test
	void testR4MeasureFhirQueryPattern() {
		loadTransaction("ExmLogicMeasureTransactionBundle.json");

		Parameters params = parameters(
				stringPart("startPeriod", "2019-01-01"),
				stringPart("endPeriod", "2020-01-01"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Measure", "measure-exm"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			switch (dr.getType()) {
				case "Patient": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					assertEquals("Patient?_id={{context.patientId}}", query);
				}
					break;

				case "Encounter": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					if (dr.hasCodeFilter()) {
						assertEquals(
								"Encounter?status=finished&subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292",
								query);
					} else {
						assertEquals("Encounter?subject=Patient/{{context.patientId}}", query);
					}
				}
					break;

				case "Coverage": {
					String query = dr.getExtensionByUrl(
							"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
							.getValueAsPrimitive().getValueAsString();
					assertEquals(
							"Coverage?beneficiary=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591",
							query);
				}
					break;
			}
		}
	}

	@Test
	void testR4LibraryEvaluationTest() {
		String bundleAsText = stringFromResource("bundlegen-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();
		loadTransaction("bundlegen-bundle.json");

		Parameters params = parameters(stringPart("target", "dummy"));

		Library returnLibrary = getClient().operation()
				.onInstance(new IdType("Library", "LibraryEvaluationTest"))
				.named("$data-requirements")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();

		assertNotNull(returnLibrary);

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			String query = dr.getExtensionByUrl(
					"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern")
					.getValueAsPrimitive().getValueAsString();
			switch (dr.getType()) {
				case "Patient": {
					assertEquals("Patient?_id={{context.patientId}}", query);
				}
					break;

				case "Condition": {
					if (dr.hasCodeFilter() && dr.getCodeFilter().size() > 0) {
						assertEquals(
								"Condition?category:in=http://mcg.com/fhir/ValueSet/condition-problem-list-category&subject=Patient/{{context.patientId}}",
								query);
					} else {
						assertEquals("Condition?subject=Patient/{{context.patientId}}", query);
					}
				}
					break;

				case "Encounter": {
					assertEquals("Encounter?subject=Patient/{{context.patientId}}", query);
				}
					break;

				case "Procedure": {
					assertEquals("Procedure?subject=Patient/{{context.patientId}}", query);
				}
					break;
			}
		}
	}
}
