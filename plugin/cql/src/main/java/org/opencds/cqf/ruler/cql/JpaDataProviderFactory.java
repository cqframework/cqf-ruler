package org.opencds.cqf.ruler.cql;

import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

import ca.uhn.fhir.rest.api.server.RequestDetails;

@FunctionalInterface
public interface JpaDataProviderFactory {
	DataProvider create(RequestDetails requestDetails, TerminologyProvider terminologyProvider);
}
