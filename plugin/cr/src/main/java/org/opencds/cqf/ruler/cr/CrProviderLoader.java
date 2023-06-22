package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

public class CrProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(CrProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final CrProviderFactory myCrProviderFactory;

	// This is just here to force the observer to register
	private final PostInitProviderRegisterer myPostInitProviderRegisterer;

	public CrProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			CrProviderFactory theCrProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myCrProviderFactory = theCrProviderFactory;
		this.myPostInitProviderRegisterer = thePostInitProviderRegisterer;
		loadProvider();
	}

	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				myLogger.info("Registering DSTU3 Ruler Clinical Reasoning Providers");
				myResourceProviderFactory.addSupplier(myCrProviderFactory::getDataOperationsProvider);
				myResourceProviderFactory.addSupplier(myCrProviderFactory::getCollectDataProvider);
				break;
			case R4:
				myLogger.info("Registering R4 Ruler Clinical Reasoning Providers");
				myResourceProviderFactory.addSupplier(myCrProviderFactory::getDataOperationsProvider);
				myResourceProviderFactory.addSupplier(myCrProviderFactory::getCollectDataProvider);
				break;
			default:
				throw new ConfigurationException("Clinical Reasoning not supported for FHIR version "
						+ myFhirContext.getVersion().getVersion());
		}
	}
}
