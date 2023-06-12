package org.opencds.cqf.ruler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

public class CrOperationLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(CrOperationLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final CrOperationFactory myCqlProviderFactory;

	public CrOperationLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			CrOperationFactory theCqlProviderFactory) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myCqlProviderFactory = theCqlProviderFactory;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
				myLogger.info("Registering Clinical Reasoning Providers");
				myResourceProviderFactory.addSupplier(myCqlProviderFactory::getMeasureOperationsProvider);
				myResourceProviderFactory.addSupplier(myCqlProviderFactory::getActivityDefinitionProvider);
				break;
			default:
				throw new ConfigurationException(
						Msg.code(1653) + "Clinical Reasoning not supported for FHIR version "
								+ myFhirContext.getVersion().getVersion());
		}
	}
}
