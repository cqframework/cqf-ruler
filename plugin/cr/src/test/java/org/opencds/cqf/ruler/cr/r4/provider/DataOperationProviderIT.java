package org.opencds.cqf.ruler.cr.r4.provider;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { DataOperationProviderIT.class,
	CrConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class DataOperationProviderIT extends RestIntegrationTest {

	@Test
	public void testR4LibraryDataRequirementsOperation() throws IOException {
		String bundleAsText = stringFromResource( "DataReqLibraryTransactionBundle.json");
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

	@Test
	public void testR4LibraryFhirQueryPattern() throws IOException {
		String bundleAsText = stringFromResource("ExmLogicTransactionBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("target").setValue(new StringType("dummy"));

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

		Parameters params = new Parameters();
		params.addParameter().setName("startPeriod").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("endPeriod").setValue(new StringType("2020-01-01"));

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

		Parameters params = new Parameters();
		params.addParameter().setName("startPeriod").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("endPeriod").setValue(new StringType("2020-01-01"));

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

		Parameters params = new Parameters();
		params.addParameter().setName("target").setValue(new StringType("dummy"));

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
