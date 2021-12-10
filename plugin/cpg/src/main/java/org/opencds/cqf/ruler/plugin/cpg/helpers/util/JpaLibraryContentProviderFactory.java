package org.opencds.cqf.ruler.plugin.cpg.helpers.util;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaLibraryContentProviderFactory {
	JpaLibraryContentProvider create(RequestDetails requestDetails);
}
