package org.opencds.cqf.ruler.plugin.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaLibraryContentProviderFactory {
	JpaLibraryContentProvider create(RequestDetails requestDetails);
}
