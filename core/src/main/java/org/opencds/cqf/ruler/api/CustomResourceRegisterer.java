package org.opencds.cqf.ruler.api;

import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;

public interface CustomResourceRegisterer {
	void register(FhirContext context);
	List<IResourceProvider> getResourceProviders(FhirContext context);

}