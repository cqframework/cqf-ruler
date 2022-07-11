package org.opencds.cqf.ruler.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.external.AppProperties;
import org.opencds.cqf.ruler.test.behavior.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

public class TestInitService implements ResourceLoader, ResourceCreator, IdCreator {

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

	void baseBeforeEach() {
		myCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		myCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		myServerBase = "http://localhost:" + myPort + "/fhir/";
		myAppProperties.setServer_address(myServerBase);
		myClient = myCtx.newRestfulGenericClient(myServerBase);
	}

	void baseAfterAll() {
		myDbService.resetDatabase();
	}
}
