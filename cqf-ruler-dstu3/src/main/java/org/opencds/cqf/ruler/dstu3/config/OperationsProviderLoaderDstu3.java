package org.opencds.cqf.ruler.dstu3.config;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

import org.opencds.cqf.ruler.dstu3.providers.QuestionnaireProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class OperationsProviderLoaderDstu3 {
	private static final Logger myLogger = LoggerFactory.getLogger(OperationsProviderLoaderDstu3.class);
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;

	@PostConstruct
	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				myLogger.info("Registering CQF-Ruler Providers");
				myResourceProviderFactory.addSupplier(() -> new QuestionnaireProvider(myFhirContext));
				break;
			default:
				throw new ConfigurationException("CQL not supported for FHIR version " + myFhirContext.getVersion().getVersion());
		}
	}
}
