package org.opencds.cqf.ruler.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.external.AppProperties;
import org.opencds.cqf.ruler.test.behavior.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@Import(Application.class)
@TestPropertySource(properties = {
	"debug=true",
	"loader.debug=true",
	"scheduling_disabled=true",
	"spring.main.allow-bean-definition-overriding=true",
	"spring.batch.job.enabled=false",
	"hapi.fhir.allow_external_references=true",
	"hapi.fhir.enforce_referential_integrity_on_write=false",
	"hapi.fhir.auto_create_placeholder_reference_targets=true",
	"hapi.fhir.client_id_strategy=ANY",
	"spring.main.lazy-initialization=true",
	"spring.flyway.enabled=false" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestUnitTest implements ResourceLoader, ResourceCreator, IdCreator {

	@Autowired
	AppProperties myAppProperties;

	@Autowired
	TestDbService myDbService;

	@Autowired
	private FhirContext myCtx;

	@Autowired
	DaoRegistry myDaoRegistry;

	@Autowired
	IFulltextSearchSvc myFulltextSearchSvc;

	@LocalServerPort
	private int myPort;

	private IGenericClient myClient;

	private String myServerBase;

	@Override
	public FhirContext getFhirContext() {
		return myCtx;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}

	protected String getServerBase() {
		return myServerBase;
	}

	protected IGenericClient getClient() {
		return myClient;
	}

	protected int getPort() {
		return myPort;
	}

	protected AppProperties getAppProperties() {
		return myAppProperties;
	}

	@BeforeEach
	void baseBeforeEach() {
		myCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		myCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		myServerBase = "http://localhost:" + myPort + "/fhir/";
		myAppProperties.setServer_address(myServerBase);
		myClient = myCtx.newRestfulGenericClient(myServerBase);
	}

	@AfterEach
	void baseAfterEach() {
		myDbService.resetDatabase();
	}
}
