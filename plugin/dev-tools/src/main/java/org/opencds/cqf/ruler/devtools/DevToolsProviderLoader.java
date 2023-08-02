package org.opencds.cqf.ruler.devtools;

import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

public class DevToolsProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(DevToolsProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final DevToolsProviderFactory myDevToolsProviderFactory;

	private final PostInitProviderRegisterer myPostInitProviderRegisterer;
	public DevToolsProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			DevToolsProviderFactory theCrProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myDevToolsProviderFactory = theCrProviderFactory;
		this.myPostInitProviderRegisterer = thePostInitProviderRegisterer;
		loadProvider();
	}

	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				myLogger.info("Registering DSTU3 Ruler DevTools Providers");
				myResourceProviderFactory.addSupplier(myDevToolsProviderFactory::getCodeSystemUpdateProvider);
				myResourceProviderFactory.addSupplier(myDevToolsProviderFactory::getCacheValueSetsProvider);
				break;
			case R4:
				myLogger.info("Registering R4 Ruler DevTools Providers");
				myResourceProviderFactory.addSupplier(myDevToolsProviderFactory::getCodeSystemUpdateProvider);
				myResourceProviderFactory.addSupplier(myDevToolsProviderFactory::getCacheValueSetsProvider);
				break;
			default:
				throw new ConfigurationException("Clinical Reasoning not supported for FHIR version "
						+ myFhirContext.getVersion().getVersion());
		}
	}
}
