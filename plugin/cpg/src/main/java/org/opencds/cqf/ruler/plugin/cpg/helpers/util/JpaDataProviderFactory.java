package org.opencds.cqf.ruler.plugin.cpg.helpers.util;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.cql.engine.data.DataProvider;

@FunctionalInterface
public interface JpaDataProviderFactory {
	DataProvider create(RequestDetails requestDetails);
}
