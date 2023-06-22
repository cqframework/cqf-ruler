package org.opencds.cqf.ruler.ra;

import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

public class RAProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(RAProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final RAProviderFactory myRAProviderFactory;
	// This is just here to force the observer to register
	private final PostInitProviderRegisterer myPostInitProviderRegisterer;

	public RAProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			RAProviderFactory theCrProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myRAProviderFactory = theCrProviderFactory;
		this.myPostInitProviderRegisterer = thePostInitProviderRegisterer;
		loadProvider();
	}

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
