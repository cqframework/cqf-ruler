package org.opencds.cqf.ruler.cr.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ExpressionEvaluationIT.class,
		CrConfig.class, CqlConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4",
		})
class ExpressionEvaluationIT extends RestIntegrationTest {

	@Autowired
	private ExpressionEvaluation expressionEvaluation;

	private Map<String, IBaseResource> measures;
	private Map<String, IBaseResource> planDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		uploadTests("valueset");
		uploadTests("library");
		measures = uploadTests("measure");
		planDefinitions = uploadTests("plandefinition");
	}

	@Test
	void testExpressionEvaluationANCIND01MeasureDomain() throws Exception {
		DomainResource measure = (DomainResource) measures.get("ANCIND01");
		// Patient First
		uploadTests("test/measure/ANCIND01/charity-otala-1/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/measure/ANCIND01");
		IBaseResource patient = resources.get("charity-otala-1");
		Object ipResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.\"Initial Population\"",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(ipResult instanceof Boolean);
		assertTrue((Boolean) ipResult);
		Object denomResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.Denominator",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(denomResult instanceof Boolean);
		assertTrue((Boolean) denomResult);
		Object numerResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.Numerator",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(numerResult instanceof Boolean);
		assertTrue((Boolean) numerResult);
	}

	@Test
	void testExpressionEvaluationANCDT01PlanDefinitionDomain() throws Exception {
		DomainResource planDefinition = (DomainResource) planDefinitions.get("lcs-cds-patient-view");
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker");
		IBaseResource patient = resources.get("Former-Smoker");
		Object isFormerSmoker = expressionEvaluation.evaluateInContext(planDefinition,
				"Is former smoker who quit within past 15 years", patient.getIdElement().getIdPart(), true,
				new SystemRequestDetails());
		assertTrue(isFormerSmoker instanceof Boolean);
		assertTrue((Boolean) isFormerSmoker);

		Object isCurrentSmoker = expressionEvaluation.evaluateInContext(planDefinition, "Is current smoker",
				patient.getIdElement().getIdPart(), true, new SystemRequestDetails());
		assertTrue(isCurrentSmoker instanceof Boolean);
		assertFalse((Boolean) isCurrentSmoker);
	}
}
