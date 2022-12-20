package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface JpaCRFhirDalFactory {
	JpaCRFhirDal create(RequestDetails requestDetails);
}
