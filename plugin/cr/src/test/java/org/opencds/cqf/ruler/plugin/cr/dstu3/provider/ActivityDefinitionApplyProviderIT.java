package org.opencds.cqf.ruler.plugin.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.opencds.cqf.ruler.plugin.devtools.DevToolsConfig;
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
public class ActivityDefinitionApplyProviderIT implements ITestSupport {

	@Autowired
	private ActivityDefinitionApplyProvider activityDefinitionApplyProvider;

	@Autowired
	private FhirContext ourCtx;

	@Autowired
	private DaoRegistry myDaoRegistry;

	@LocalServerPort
	private int port;

	private Map<String, IBaseResource> activityDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		activityDefinitions = uploadTests("activitydefinition", ourCtx, myDaoRegistry);
	}

	@Test
	public void testActivityDefinitionApply() throws Exception {
		DomainResource activityDefinition = (DomainResource) activityDefinitions.get("opioidcds-risk-assessment-request");
		// Patient First
		Map<String, IBaseResource> resources = uploadTests("test/activitydefinition/Patient", ourCtx, myDaoRegistry);
		IBaseResource patient = resources.get("ExamplePatient");
		Resource applyResult = activityDefinitionApplyProvider.apply(new SystemRequestDetails(),
				activityDefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null,
				null, null);
		assertTrue(applyResult instanceof ProcedureRequest);
		assertTrue(((ProcedureRequest) applyResult).getCode().getCoding().get(0).getCode().equals("454281000124100"));
	}
}
