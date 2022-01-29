package org.opencds.cqf.ruler.provider;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

public class DaoRegistryOperationProvider implements OperationProvider, DaoRegistryUser {
	@Autowired
	private DaoRegistry myDaoRegistry;

	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
}
