package org.opencds.cqf.ruler.plugin.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.opencds.cqf.ruler.plugin.devtools.r4.CodeSystemUpdateProvider;
import org.opencds.cqf.ruler.plugin.testutility.IServerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CrConfig.class, CqlConfig.class }, properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "hapi.fhir.fhir_version=r4",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false",
            "hapi.fhir.cr.enabled=true",
            "hapi.fhir.cql.enabled=true"
})
public class PlanDefinitionApplyProviderIT implements IServerSupport {

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

	private Map<String, IBaseResource> libraries;
	private Map<String, IBaseResource> vocabulary;
	private Map<String, IBaseResource> plandefinitions;

	@BeforeEach
	public void setup() throws Exception {
		 vocabulary = uploadTests("valueset");
		 codeSystemUpdateProvider.updateCodeSystems();
		 libraries = uploadTests("library");
		 plandefinitions = uploadTests("plandefinition");
	}

	@Test
	public void testPlanDefinitionApplyFormerSmoker() throws Exception {
		 DomainResource plandefinition = (DomainResource) plandefinitions.get("lcs-cds-patient-view");
		 // Patient First
		 uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		 Map<String, IBaseResource> resources = uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker");
		 IBaseResource patient = resources.get("Former-Smoker");
		 Object isFormerSmoker = planDefinitionApplyProvider.applyPlanDefinition(new SystemRequestDetails(), plandefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null, null, null);
		 assertTrue(isFormerSmoker instanceof CarePlan);
		 assertTrue(((CarePlan) isFormerSmoker).getDescription().equals("Potential eligible patient: Jake Lungsahoy: Born 1942-01-14 (Age: 79), Gender: male"));
		 System.out.println("x");
		 }

		 private Map<String, IBaseResource> uploadTests(String testDirectory) throws URISyntaxException, IOException {
					URL url = PlanDefinitionApplyProviderIT.class.getResource(testDirectory);
					File testDir = new File(url.toURI());
					return uploadTests(testDir.listFiles());
		 }
	
		 private Map<String, IBaseResource>  uploadTests(File[] files) throws IOException {
					Map<String, IBaseResource> resources = new HashMap<String, IBaseResource>();
					for(File file : files) {
								// depth first
							  if (file.isDirectory()) {
										 resources.putAll(uploadTests(file.listFiles()));
							  }
					}
					for (File file : files) {
						if (file.isFile()) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
							String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
							reader.close();
							IBaseResource resource = loadResource(FilenameUtils.getExtension(file.getAbsolutePath()), resourceString, ourCtx, myDaoRegistry);
							resources.put(resource.getIdElement().getIdPart(), resource);
						 }
					}
					return resources;
		 }
}
