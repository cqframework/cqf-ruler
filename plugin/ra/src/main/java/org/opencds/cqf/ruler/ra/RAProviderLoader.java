package org.opencds.cqf.ruler.ra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

public class RAProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(RAProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final RAProviderFactory myRAProviderFactory;

	public RAProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
											RAProviderFactory theCrProviderFactory) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myRAProviderFactory = theCrProviderFactory;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				myLogger.info("Registering R4 Ruler RA Providers");
				myResourceProviderFactory.addSupplier(myRAProviderFactory::getApproveProvider);
				myResourceProviderFactory.addSupplier(myRAProviderFactory::getRACodingGapsProvider);
				myResourceProviderFactory.addSupplier(myRAProviderFactory::getRemediateProvider);
				myResourceProviderFactory.addSupplier(myRAProviderFactory::getResolveProvider);
				myResourceProviderFactory.addSupplier(myRAProviderFactory::getRiskAdjustmentProvider);
				break;
			default:
				throw new ConfigurationException("RA not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}
