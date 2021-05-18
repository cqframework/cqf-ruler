package org.opencds.cqf.ruler.r4.config;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.ruler.common.dal.RulerDal;
import org.opencds.cqf.ruler.r4.providers.PlanDefinitionApplyProvider;
import org.opencds.cqf.ruler.r4.providers.QuestionnaireProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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
				myResourceProviderFactory.addSupplier(() -> new PlanDefinitionApplyProvider(appCtx.getBean(RulerDal.class), appCtx.getBean(FhirContext.class),
						new ActivityDefinitionProcessor(appCtx.getBean(FhirContext.class), appCtx.getBean(RulerDal.class), appCtx.getBean(LibraryProcessor.class)),
						appCtx.getBean(LibraryProcessor.class), appCtx.getBean(DaoRegistry.class).getResourceDao(PlanDefinition.class),
						appCtx.getBean(AdapterFactory.class), appCtx.getBean(FhirTypeConverter.class)));
				break;
			default:
				throw new ConfigurationException("CQL not supported for FHIR version " + myFhirContext.getVersion().getVersion());
		}
	}
}
