package org.opencds.cqf.ruler.cr.r4.provider;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { DataOperationProviderIT.class,
	CrConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	public void testR4LibraryDataRequirementsOperation() throws IOException {
		String bundleAsText = stringFromResource( "DataReqLibraryTransactionBundleR4.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(newPart("target", "dummy"));

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
		assertNotNull(returnLibrary2.getDataRequirement().get(1).getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-pertinence"));
	}

	@Test
	public void testR4LibraryDataRequirementsNonManifestMultiVersionOperation() throws IOException {
		String bundleAsText = stringFromResource( "DataReqLibraryTransactionBundleMultiVersionR4.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Library library = getClient().read().resource(Library.class).withId("LibraryEvaluationTest").execute();
		assertNotNull(library);
		assertEquals("http://fhir.org/guides/cqf/common/Library/LibraryEvaluationTestDependency|1.0.000",
			library.getRelatedArtifact().get(0).getResource());

		Library library2 = getClient().read().resource(Library.class).withId("LibraryEvaluationTest2").execute();
		assertNotNull(library2);
		assertEquals("http://fhir.org/guides/cqf/common/Library/LibraryEvaluationTestDependency",
			library2.getRelatedArtifact().get(0).getResource());

		assertEquals(library.getUrl(), library2.getUrl());

		Parameters params = newParameters(
			newPart("target", "dummy"));

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
	public void testR4LibraryFhirQueryPattern() throws IOException {
		String bundleAsText = stringFromResource("ExmLogicTransactionBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(newPart("target", "dummy"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Library", "EXMLogic"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			switch (dr.getType()) {
				case "Patient": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					assertTrue("Patient?_id={{context.patientId}}".equals(query));
				}
				break;

				case "Encounter": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					if (dr.hasCodeFilter()) {
						assertTrue("Encounter?subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292".equals(query));
					}
					else {
						assertTrue("Encounter?subject=Patient/{{context.patientId}}".equals(query));
					}
				}
				break;

				case "Coverage": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					assertTrue("Coverage?policy-holder=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591".equals(query));
				}
				break;
			}
		}
	}

	@Test
	public void testR4MeasureDataRequirementsOperation() throws IOException {
		String bundleAsText = stringFromResource( "Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(
			newPart("startPeriod", "2019-01-01"),
			newPart("endPeriod", "2020-01-01"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
	}

	@Test
	public void testR4MeasureFhirQueryPattern() throws IOException {
		String bundleAsText = stringFromResource("ExmLogicMeasureTransactionBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(
			newPart("startPeriod", "2019-01-01"),
			newPart("endPeriod", "2020-01-01"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Measure", "measure-exm"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			switch (dr.getType()) {
				case "Patient": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					assertTrue("Patient?_id={{context.patientId}}".equals(query));
				}
				break;

				case "Encounter": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					if (dr.hasCodeFilter()) {
						assertTrue("Encounter?status=finished&subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292".equals(query));
					}
					else {
						assertTrue("Encounter?subject=Patient/{{context.patientId}}".equals(query));
					}
				}
				break;

				case "Coverage": {
					String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
					assertTrue("Coverage?policy-holder=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591".equals(query));
				}
				break;
			}
		}
	}

	@Test
	public void testR4LibraryEvaluationTest() throws IOException {
		String bundleAsText = stringFromResource("bundlegen-bundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(newPart("target", "dummy"));

		Library returnLibrary = getClient().operation().onInstance(new IdType("Library", "LibraryEvaluationTest"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
		System.out.println("Resource:"+this.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(returnLibrary));

		for (DataRequirement dr : returnLibrary.getDataRequirement()) {
			String query = dr.getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern").getValueAsPrimitive().getValueAsString();
			switch (dr.getType()) {
				case "Patient": {
					assertTrue("Patient?_id={{context.patientId}}".equals(query));
				}
				break;

				case "Condition": {
					if (dr.hasCodeFilter() && dr.getCodeFilter().size() > 0) {
						assertTrue("Condition?category:in=http://mcg.com/fhir/ValueSet/condition-problem-list-category&subject=Patient/{{context.patientId}}".equals(query));
					}
					else {
						assertTrue("Condition?subject=Patient/{{context.patientId}}".equals(query));
					}
				}
				break;

				case "Encounter": {
					assertTrue("Encounter?subject=Patient/{{context.patientId}}".equals(query));
				}
				break;

				case "Procedure": {
					assertTrue("Procedure?subject=Patient/{{context.patientId}}".equals(query));
				}
				break;
			}
		}
	}
}
