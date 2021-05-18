package org.opencds.cqf.ruler.r4.config;

import javax.annotation.PostConstruct;

import org.opencds.cqf.ruler.r4.providers.PlanDefinitionApplyProvider;
import org.opencds.cqf.ruler.r4.providers.QuestionnaireProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

@Service
public class OperationsProviderLoader {
	private static final Logger myLogger = LoggerFactory.getLogger(OperationsProviderLoader.class);
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;

	@Autowired
	private ApplicationContext appCtx;

	@PostConstruct
	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				myLogger.info("Registering CQF-Ruler Providers");
				myResourceProviderFactory.addSupplier(() -> new QuestionnaireProvider(myFhirContext));
				myResourceProviderFactory.addSupplier(() -> appCtx.getBean(PlanDefinitionApplyProvider.class));
				break;
			default:
				throw new ConfigurationException("CQL not supported for FHIR version " + myFhirContext.getVersion().getVersion());
		}
	}
}
