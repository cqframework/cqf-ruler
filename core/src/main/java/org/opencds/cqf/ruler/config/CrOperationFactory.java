package org.opencds.cqf.ruler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionOperationsProvider;
import ca.uhn.fhir.cr.dstu3.measure.MeasureOperationsProvider;
import ca.uhn.fhir.i18n.Msg;

public class CrOperationFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getMeasureOperationsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(MeasureOperationsProvider.class);
			case R4:
				return myApplicationContext.getBean(ca.uhn.fhir.cr.r4.measure.MeasureOperationsProvider.class);
			default:
				throw new ConfigurationException(
						Msg.code(1654) + "Measure operations are not supported for FHIR version "
								+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getActivityDefinitionProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(ActivityDefinitionOperationsProvider.class);
			case R4:
				return myApplicationContext
						.getBean(ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider.class);
			default:
				throw new ConfigurationException(
						Msg.code(1654) + "ActivityDefinition operations are not supported for FHIR version "
								+ myFhirContext.getVersion().getVersion());
		}
	}
}

// Register the Cr Providers
// if(appProperties.getCr_enabled()) {
// switch (fhirSystemDao.getContext().getVersion().getVersion()) {
// case DSTU3:
// ourLog.info("Registering Dstu3 CR providers");
// fhirServer.registerProviders(theDstu3MeasureOperationProvider.get()
// ,theDstu3ActivityDefinitionProvider.get()
// ,theDstu3PlanDefinitionOperationProvider.get()
// ,theDstu3QuestionnaireResponseOperationProvider.get()
// ,theDstu3QuestionnaireOperationsProvider.get());
// break;
// case R4:
// ourLog.info("Registering R4 CR providers");
// fhirServer.registerProviders(theR4MeasureOperationProvider.get()
// ,theR4ActivityDefinitionProvider.get()
// ,theR4PlanDefinitionOperationProvider.get()
// ,theR4CareGapsOperationProvider.get()
// ,theR4SubmitDataProvider.get()
// ,theR4QuestionnaireResponseOperationProvider.get()
// ,theR4QuestionnaireOperationsProvider.get());
// break;
// default:
// break;
// }
// } Stuff {

// }
