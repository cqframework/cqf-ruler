package org.opencds.cqf.ruler.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;

import java.util.List;

public interface CustomResourceRegisterer {
	void register(FhirContext context);
	List<IResourceProvider> getResourceProviders(FhirContext context);

}
