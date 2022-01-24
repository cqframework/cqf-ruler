package org.opencds.cqf.ruler.cr.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.devtools.r4.CodeSystemUpdateProvider;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
		CrConfig.class, CqlConfig.class, DevToolsConfig.class }, properties = {
				"spring.main.allow-bean-definition-overriding=true",
				"spring.batch.job.enabled=false",
				"hapi.fhir.fhir_version=r4",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false"
		})
public class ExpressionEvaluationIT extends RestIntegrationTest {

	@Autowired
	private ExpressionEvaluation expressionEvaluation;

	@Autowired
	private CodeSystemUpdateProvider codeSystemUpdateProvider;

	private Map<String, IBaseResource> measures;
	private Map<String, IBaseResource> planDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		uploadTests("valueset");
		codeSystemUpdateProvider.updateCodeSystems();
		uploadTests("library");
		measures = uploadTests("measure");
		planDefinitions = uploadTests("plandefinition");
	}

	@Test
	public void testExpressionEvaluationANCIND01MeasureDomain() throws Exception {
		DomainResource measure = (DomainResource) measures.get("ANCIND01");
		// Patient First
		uploadTests("test/measure/ANCIND01/charity-otala-1/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/measure/ANCIND01");
		IBaseResource patient = resources.get("charity-otala-1");
		Object ipResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.\"Initial Population\"",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(ipResult instanceof Boolean);
		assertTrue(((Boolean) ipResult).booleanValue());
		Object denomResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.Denominator",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(denomResult instanceof Boolean);
		assertTrue(((Boolean) denomResult).booleanValue());
		Object numerResult = expressionEvaluation.evaluateInContext(measure, "ANCIND01.Numerator",
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(numerResult instanceof Boolean);
		assertTrue(((Boolean) numerResult).booleanValue());
	}

	@Test
	public void testExpressionEvaluationANCDT01PlanDefinitionDomain() throws Exception {
		DomainResource planDefinition = (DomainResource) planDefinitions.get("lcs-cds-patient-view");
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker");
		IBaseResource patient = resources.get("Former-Smoker");
		Object isFormerSmoker = expressionEvaluation.evaluateInContext(planDefinition,
				"Is former smoker who quit within past 15 years", patient.getIdElement().getIdPart(), true,
				new SystemRequestDetails());
		assertTrue(isFormerSmoker instanceof Boolean);
		assertTrue(((Boolean) isFormerSmoker).booleanValue());

		Object isCurrentSmoker = expressionEvaluation.evaluateInContext(planDefinition, "Is current smoker",
				patient.getIdElement().getIdPart(), true, new SystemRequestDetails());
		assertTrue(isCurrentSmoker instanceof Boolean);
		assertTrue((!(Boolean) isCurrentSmoker));
	}
}
