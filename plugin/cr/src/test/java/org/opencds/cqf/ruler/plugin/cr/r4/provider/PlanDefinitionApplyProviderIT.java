package org.opencds.cqf.ruler.plugin.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.opencds.cqf.ruler.plugin.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.devtools.r4.CodeSystemUpdateProvider;
import org.opencds.cqf.ruler.test.ITestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
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
public class PlanDefinitionApplyProviderIT implements ITestSupport {

	@Autowired
	private PlanDefinitionApplyProvider planDefinitionApplyProvider;

	@Autowired
	private FhirContext ourCtx;

	@Autowired
	private DaoRegistry myDaoRegistry;

	@LocalServerPort
	private int port;

	@Autowired
	private CodeSystemUpdateProvider codeSystemUpdateProvider;

	private Map<String, IBaseResource> planDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		uploadTests("valueset", ourCtx, myDaoRegistry);
		codeSystemUpdateProvider.updateCodeSystems();
		uploadTests("library", ourCtx, myDaoRegistry);
		planDefinitions = uploadTests("plandefinition", ourCtx, myDaoRegistry);
	}

	@Test
	public void testPlanDefinitionApplyFormerSmoker() throws Exception {
		DomainResource plandefinition = (DomainResource) planDefinitions.get("lcs-cds-patient-view");
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient", ourCtx, myDaoRegistry);
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker", ourCtx, myDaoRegistry);
		IBaseResource patient = resources.get("Former-Smoker");
		Object isFormerSmoker = planDefinitionApplyProvider.applyPlanDefinition(new SystemRequestDetails(),
				plandefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null, null,
				null);
		assertTrue(isFormerSmoker instanceof CarePlan);
	}
}
