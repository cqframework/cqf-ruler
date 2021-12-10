package org.opencds.cqf.ruler.plugin.cpg.helpers.util;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaTerminologyProviderFactory{
	JpaTerminologyProvider create(RequestDetails requestDetails);
}
