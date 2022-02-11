package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaTerminologyProviderFactory{
	JpaTerminologyProvider create(RequestDetails requestDetails);
}
