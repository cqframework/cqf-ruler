package org.opencds.cqf.ruler.plugin.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaTerminologyProviderFactory{
	JpaTerminologyProvider create(RequestDetails requestDetails);
}
