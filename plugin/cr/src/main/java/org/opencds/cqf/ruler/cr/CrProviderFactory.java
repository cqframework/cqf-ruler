package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.ruler.cr.dstu3.provider.CollectDataProvider;
import org.opencds.cqf.ruler.cr.dstu3.provider.DataOperationsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;

public class CrProviderFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getCollectDataProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(CollectDataProvider.class);
			case R4:
				return myApplicationContext.getBean(org.opencds.cqf.ruler.cr.r4.provider.CollectDataProvider.class);
			default:
				throw new ConfigurationException("CollectDataProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getDataOperationsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(DataOperationsProvider.class);
			case R4:
				return myApplicationContext
					.getBean(org.opencds.cqf.ruler.cr.r4.provider.DataOperationsProvider.class);
			default:
				throw new ConfigurationException("DataOperationsProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}

