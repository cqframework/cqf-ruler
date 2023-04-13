package org.opencds.cqf.ruler.cr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;

@Service
public class CrProviderFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	private Object notSupported() {
		throw new ConfigurationException(
				Msg.code(2321) + "CPG is not supported for FHIR version " + myFhirContext.getVersion().getVersion());
	}

	public Object getActivityDefinitionOperationsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext
						.getBean(org.opencds.cqf.ruler.cr.dstu3.provider.ActivityDefinitionOperationsProvider.class);
			case R4:
				return myApplicationContext
						.getBean(org.opencds.cqf.ruler.cr.r4.provider.ActivityDefinitionOperationsProvider.class);
			default:
				return notSupported();
		}
	}

	public Object getPlanDefinitionOperationsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext
						.getBean(org.opencds.cqf.ruler.cr.dstu3.provider.PlanDefinitionOperationsProvider.class);
			case R4:
				return myApplicationContext
						.getBean(org.opencds.cqf.ruler.cr.r4.provider.PlanDefinitionOperationsProvider.class);
			default:
				return notSupported();
		}
	}
}
