package org.opencds.cqf.ruler.plugin.cr.dstu3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.opencds.cqf.ruler.plugin.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.devtools.dstu3.CodeSystemUpdateProvider;
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
				"hapi.fhir.fhir_version=dstu3",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false"
		})
public class ExpressionEvaluationIT implements ITestSupport {

	@Autowired
	private ExpressionEvaluation expressionEvaluation;

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
	public void testOpioidCdsPlanDefinitionDomain() throws Exception {
		DomainResource plandefinition = (DomainResource) planDefinitions.get("opioidcds-10");
		// Patient First
		uploadTests("test/plandefinition/Rec10/Patient", ourCtx, myDaoRegistry);
		Map<String, IBaseResource> resources = uploadTests("test/plandefinition/Rec10", ourCtx, myDaoRegistry);
		IBaseResource patient = resources.get("example-rec-10-no-screenings");
		Object isFormerSmoker = expressionEvaluation.evaluateInContext(plandefinition,
				"true", false,
				patient.getIdElement().getIdPart(), new SystemRequestDetails());
		assertTrue(isFormerSmoker instanceof Boolean);
		assertTrue(((Boolean) isFormerSmoker).booleanValue());
	}
}
