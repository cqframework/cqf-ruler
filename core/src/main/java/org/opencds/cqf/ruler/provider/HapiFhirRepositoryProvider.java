package org.opencds.cqf.ruler.provider;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;

public class HapiFhirRepositoryProvider implements OperationProvider, DaoRegistryUser {
  @Autowired
	private DaoRegistry myDaoRegistry;

  @Autowired
	RestfulServer myRestfulServer;

	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
  public HapiFhirRepository getRepository(RequestDetails theRequestDetails) {
    return new HapiFhirRepository(myDaoRegistry, theRequestDetails, myRestfulServer);
  }
}
