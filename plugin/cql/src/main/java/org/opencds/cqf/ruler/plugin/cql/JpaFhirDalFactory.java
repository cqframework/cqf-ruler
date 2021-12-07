package org.opencds.cqf.ruler.plugin.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface JpaFhirDalFactory {
	JpaFhirDal create(RequestDetails requestDetails);
}
