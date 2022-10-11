package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface JpaEvalFhirDalFactory {
	JpaEvalFhirDal create(RequestDetails requestDetails);
}
