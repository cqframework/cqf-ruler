package org.opencds.cqf.ruler.casereporting;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseReportingProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(CaseReportingProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final CaseReportingProviderFactory myCaseReportingProviderFactory;

	// This is just here to force the observer to register
	private final PostInitProviderRegisterer myPostInitProviderRegisterer;

	public CaseReportingProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
												  CaseReportingProviderFactory theCaseReportingProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myCaseReportingProviderFactory = theCaseReportingProviderFactory;
		this.myPostInitProviderRegisterer = thePostInitProviderRegisterer;
		loadProvider();
	}

	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				myLogger.info("Registering R4 Ruler CaseReporting Providers");
				myResourceProviderFactory.addSupplier(myCaseReportingProviderFactory::getMeasureDataProcessProvider);
				myResourceProviderFactory.addSupplier(myCaseReportingProviderFactory::getProcessMessageProvider);
				break;
			default:
				throw new ConfigurationException("CaseReporting not supported for FHIR version "
						+ myFhirContext.getVersion().getVersion());
		}
	}
}
