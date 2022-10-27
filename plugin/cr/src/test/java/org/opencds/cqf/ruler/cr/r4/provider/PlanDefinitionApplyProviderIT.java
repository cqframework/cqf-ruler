package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		PlanDefinitionApplyProviderIT.class,
		CrConfig.class, CqlConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4",
		})
class PlanDefinitionApplyProviderIT extends RestIntegrationTest {

	private Map<String, IBaseResource> planDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		uploadTests("valueset");
		uploadTests("library");
		planDefinitions = uploadTests("plandefinition");
	}

	@Test
	void testPlanDefinitionApplyFormerSmoker() throws Exception {
		DomainResource plandefinition = (DomainResource) planDefinitions.get("lcs-cds-patient-view");
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker");
		IBaseResource patient = resources.get("Former-Smoker");

		var params = parameters(
				stringPart("patient", patient.getIdElement().getIdPart()),
				stringPart("encounter", ""),
				stringPart("practitioner", ""),
				stringPart("organization", ""),
				stringPart("userType", ""),
				stringPart("userLanguage", ""),
				stringPart("userTaskContext", ""),
				stringPart("setting", ""),
				stringPart("settingContext", ""));

		var isFormerSmoker = getClient().operation()
				.onInstance(plandefinition.getIdElement())
				.named("$apply")
				.withParameters(params)
				.returnResourceType(CarePlan.class)
				.execute();
		assertNotNull(isFormerSmoker);
		assertEquals("CarePlan", isFormerSmoker.fhirType());
	}

	@Test
	void testPlanDefinitionApplyR5FormerSmoker() throws Exception {
		DomainResource plandefinition = (DomainResource) planDefinitions.get("lcs-cds-patient-view");
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker");
		IBaseResource patient = resources.get("Former-Smoker");

		var params = parameters(
				stringPart("patient", patient.getIdElement().getIdPart()),
				stringPart("encounter", ""),
				stringPart("practitioner", ""),
				stringPart("organization", ""),
				stringPart("userType", ""),
				stringPart("userLanguage", ""),
				stringPart("userTaskContext", ""),
				stringPart("setting", ""),
				stringPart("settingContext", ""));

		var isFormerSmoker = getClient().operation()
				.onInstance(plandefinition.getIdElement())
				.named("$apply")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		assertNotNull(isFormerSmoker);
		assertEquals("Bundle", isFormerSmoker.fhirType());
	}
}
