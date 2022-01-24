package org.opencds.cqf.ruler.test;

import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

public class DaoIntegrationTest implements ResourceLoader {

	@Autowired
	private FhirContext myCtx;

	@Autowired
	DaoRegistry myDaoRegistry;

	@Override
	public FhirContext getFhirContext() {
		return myCtx;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
	
}
