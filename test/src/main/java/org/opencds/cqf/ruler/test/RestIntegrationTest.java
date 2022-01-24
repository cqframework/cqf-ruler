package org.opencds.cqf.ruler.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

public class RestIntegrationTest implements ResourceLoader {

	@Autowired
	private FhirContext myCtx;

	@Autowired
	DaoRegistry myDaoRegistry;

	@LocalServerPort
	private int myPort;

	private IGenericClient myClient;

	@Override
	public FhirContext getFhirContext() {
		return myCtx;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}

	protected IGenericClient getClient() {
		return myClient;
	}

	protected int getPort() {
		return myPort;
	}

	@BeforeEach
	void baseBeforeEach() {
		myCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		myCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + myPort + "/fhir/";
		myClient = myCtx.newRestfulGenericClient(ourServerBase);
	}
}
