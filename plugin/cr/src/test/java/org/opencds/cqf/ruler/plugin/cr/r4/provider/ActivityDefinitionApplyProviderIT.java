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

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.opencds.cqf.ruler.plugin.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.test.IServerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.apache.commons.io.FilenameUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CrConfig.class, CqlConfig.class, DevToolsConfig.class  }, properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "hapi.fhir.fhir_version=r4",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false"
})
public class ActivityDefinitionApplyProviderIT  implements IServerSupport{

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
		 activityDefinitions = uploadTests("activitydefinition");
	}

	@Test
	public void testActivityDefinitionApply() throws Exception {
		 DomainResource activityDefinition = (DomainResource) activityDefinitions.get("opioidcds-risk-assessment-request");
		 // Patient First
		 Map<String, IBaseResource> resources = uploadTests("test/activitydefinition/Patient");
		 IBaseResource patient = resources.get("ExamplePatient");
		 Resource applyResult = activityDefinitionApplyProvider.apply(new SystemRequestDetails(), activityDefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null, null, null);
			assertTrue(applyResult instanceof ServiceRequest);
			assertTrue(((ServiceRequest) applyResult).getCode().getCoding().get(0).getCode().equals("454281000124100"));
		 }
		 
		 protected Map<String, IBaseResource> uploadTests(String testDirectory) throws URISyntaxException, IOException {
			URL url = ActivityDefinitionApplyProviderIT.class.getResource(testDirectory);
			File testDir = new File(url.toURI());
			return uploadTests(testDir.listFiles());
	}
	
	protected Map<String, IBaseResource>  uploadTests(File[] files) throws IOException {
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
