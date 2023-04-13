package org.opencds.cqf.ruler.cr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

@Service
public class CrProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(CrProviderLoader.class);
	private final FhirContext myFhirContext;
	private final ResourceProviderFactory myResourceProviderFactory;
	private final CrProviderFactory myCrProviderFactory;

	public CrProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			CrProviderFactory theCrProviderFactory) {
		myFhirContext = theFhirContext;
		myResourceProviderFactory = theResourceProviderFactory;
		myCrProviderFactory = theCrProviderFactory;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
				myLogger.info("Registering CR Provider");
				myResourceProviderFactory.addSupplier(() -> myCrProviderFactory.getActivityDefinitionOperationsProvider());
				myResourceProviderFactory.addSupplier(() -> myCrProviderFactory.getPlanDefinitionOperationsProvider());
				break;
			default:
				throw new ConfigurationException(
						Msg.code(1653) + "CQL not supported for FHIR version " + myFhirContext.getVersion().getVersion());
		}
	}
}
