package org.opencds.cqf.ruler.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.test.behavior.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.batch2.config.Batch2JobRegisterer;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@Import(Application.class)
@TestPropertySource(properties = {
		"debug=true",
		"loader.debug=true",
		"scheduling_disabled=true",
		"spring.datasource.url=jdbc:h2:mem:test",
		"spring.flyway.enabled=false",
		"spring.main.allow-circular-references=true",
		"spring.main.lazy-initialization=true",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.batch.job.enabled=false",
		// "spring.jpa.properties.hibernate.show_sql=true",
		// "spring.jpa.properties.hibernate.format_sql=true",
		// "spring.jpa.properties.hibernate.use_sql_comments=true",
		"hapi.fhir.allow_external_references=true",
		"hapi.fhir.enforce_referential_integrity_on_write=false",
		"hapi.fhir.auto_create_placeholder_reference_targets=true",
		"hapi.fhir.client_id_strategy=ANY",
})
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
public class RestIntegrationTest
		implements ResourceLoader, ResourceCreator, IdCreator {

	// This isn't used directly by the tests, but it forces the Batch2JobRegisterer
	// to be created even though lazy initialization is set up for Spring
	@Autowired
	Batch2JobRegisterer batch2JobRegisterer;

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

	protected TestDbService getDbService() {
		return myDbService;
	}

	@BeforeEach
	public void baseBeforeEach() {
		myCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		myCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		myServerBase = "http://localhost:" + getPort() + "/fhir/";
		myAppProperties.setServer_address(getServerBase());
		myClient = myCtx.newRestfulGenericClient(getServerBase());
	}

	@AfterAll
	public void baseAfterAll() {
		getDbService().resetDatabase();
	}
}
