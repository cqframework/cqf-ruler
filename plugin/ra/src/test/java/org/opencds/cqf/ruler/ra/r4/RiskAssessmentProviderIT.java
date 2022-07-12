package org.opencds.cqf.ruler.ra.r4;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ReportProviderIT.class,
	RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class RiskAssessmentProviderIT extends RestIntegrationTest {
	@Autowired
	private RAProperties myRaProperties;
	@BeforeEach
	void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
		loadTransaction("ConditionCategoryPOC-bundle.json");
	}

	@Test
	void riskAssessmentHistoricOpen() {
		loadTransaction("tests-hist-open-HCC189-bundle.json");

		Parameters params = newParameters(
			newPart("periodStart", "2022-01-01"),
			newPart("periodEnd", "2022-12-31"),
			newPart("subject", "Patient/hist-open-HCC189")
		);

		MeasureReport response = getClient().operation()
			.onInstance(new IdType("Measure", "ConditionCategoryPOC"))
			.named("$risk-assessment")
			.withParameters(params)
			.returnResourceType(MeasureReport.class)
			.execute();

		assertFalse(response.getGroup().isEmpty());
		assertEquals(1, response.getGroup().size());
		assertFalse(response.getGroupFirstRep().getExtension().isEmpty());
		assertEquals(2, response.getGroupFirstRep().getExtension().size());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().hasValue());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getSystem());
		assertEquals("historic", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getExtension().get(1).getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getSystem());
		assertEquals("open-gap", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().hasCode() && response.getGroupFirstRep().getCode().hasCoding());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasSystem());
		assertEquals("http://terminology.hl7.org/CodeSystem/cmshcc", response.getGroupFirstRep().getCode().getCodingFirstRep().getSystem());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasCode());
		assertEquals("189", response.getGroupFirstRep().getCode().getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasDisplay());
		assertEquals("Amputation Status, Lower Limb/Amputation Complications", response.getGroupFirstRep().getCode().getCodingFirstRep().getDisplay());
	}

	@Test
	void riskAssessmentHistoricClosed() {
		loadTransaction("tests-hist-closed-HCC189-bundle.json");

		Parameters params = newParameters(
			newPart("periodStart", "2022-01-01"),
			newPart("periodEnd", "2022-12-31"),
			newPart("subject", "Patient/hist-closed-HCC189")
		);

		MeasureReport response = getClient().operation()
			.onInstance(new IdType("Measure", "ConditionCategoryPOC"))
			.named("$risk-assessment")
			.withParameters(params)
			.returnResourceType(MeasureReport.class)
			.execute();

		assertFalse(response.getGroup().isEmpty());
		assertEquals(1, response.getGroup().size());
		assertFalse(response.getGroupFirstRep().getExtension().isEmpty());
		assertEquals(2, response.getGroupFirstRep().getExtension().size());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().hasValue());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getSystem());
		assertEquals("historic", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getExtension().get(1).getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getSystem());
		assertEquals("closed-gap", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().hasCode() && response.getGroupFirstRep().getCode().hasCoding());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasSystem());
		assertEquals("http://terminology.hl7.org/CodeSystem/cmshcc", response.getGroupFirstRep().getCode().getCodingFirstRep().getSystem());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasCode());
		assertEquals("189", response.getGroupFirstRep().getCode().getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasDisplay());
		assertEquals("Amputation Status, Lower Limb/Amputation Complications", response.getGroupFirstRep().getCode().getCodingFirstRep().getDisplay());
	}

	@Test
	void riskAssessmentHistoricNetNew() {
		loadTransaction("tests-netnew-HCC189-bundle.json");

		Parameters params = newParameters(
			newPart("periodStart", "2022-01-01"),
			newPart("periodEnd", "2022-12-31"),
			newPart("subject", "Patient/netnew-HCC189")
		);

		MeasureReport response = getClient().operation()
			.onInstance(new IdType("Measure", "ConditionCategoryPOC"))
			.named("$risk-assessment")
			.withParameters(params)
			.returnResourceType(MeasureReport.class)
			.execute();

		assertFalse(response.getGroup().isEmpty());
		assertEquals(1, response.getGroup().size());
		assertFalse(response.getGroupFirstRep().getExtension().isEmpty());
		assertEquals(2, response.getGroupFirstRep().getExtension().size());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().hasValue());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getSystem());
		assertEquals("net-new", ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getExtension().get(1).getValue() instanceof CodeableConcept && ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getSystem());
		assertEquals("closed-gap", ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().hasCode() && response.getGroupFirstRep().getCode().hasCoding());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasSystem());
		assertEquals("http://terminology.hl7.org/CodeSystem/cmshcc", response.getGroupFirstRep().getCode().getCodingFirstRep().getSystem());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasCode());
		assertEquals("189", response.getGroupFirstRep().getCode().getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasDisplay());
		assertEquals("Amputation Status, Lower Limb/Amputation Complications", response.getGroupFirstRep().getCode().getCodingFirstRep().getDisplay());
	}
}
