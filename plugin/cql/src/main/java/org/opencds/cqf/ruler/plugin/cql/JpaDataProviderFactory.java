package org.opencds.cqf.ruler.plugin.cql;

import org.opencds.cqf.cql.engine.data.DataProvider;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaDataProviderFactory {
	DataProvider create(RequestDetails requestDetails);
}
