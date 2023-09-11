package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface JpaEvalFhirDalFactory {
	JpaEvalFhirDal create(RequestDetails requestDetails);
}
