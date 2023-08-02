package org.opencds.cqf.ruler.sdc;

import org.opencds.cqf.ruler.sdc.dstu3.ExtractProvider;
import org.opencds.cqf.ruler.sdc.dstu3.TransformProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;

public class SDCProviderFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getExtractProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(ExtractProvider.class);
			case R4:
				return myApplicationContext
					.getBean(org.opencds.cqf.ruler.sdc.r4.ExtractProvider.class);
			default:
				throw new ConfigurationException("ExtractProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getTransformProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(TransformProvider.class);
			case R4:
				return myApplicationContext
					.getBean(org.opencds.cqf.ruler.sdc.r4.TransformProvider.class);
			default:
				throw new ConfigurationException("TransformProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}
