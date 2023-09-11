package org.opencds.cqf.ruler.devtools;

import org.opencds.cqf.ruler.devtools.dstu3.CacheValueSetsProvider;
import org.opencds.cqf.ruler.devtools.dstu3.CodeSystemUpdateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;

public class DevToolsProviderFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getCacheValueSetsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(CacheValueSetsProvider.class);
			case R4:
				return myApplicationContext.getBean(org.opencds.cqf.ruler.devtools.r4.CacheValueSetsProvider.class);
			default:
				throw new ConfigurationException("CacheValueSetsProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getCodeSystemUpdateProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(CodeSystemUpdateProvider.class);
			case R4:
				return myApplicationContext
					.getBean(org.opencds.cqf.ruler.devtools.r4.CodeSystemUpdateProvider.class);
			default:
				throw new ConfigurationException("LibraryEvaluationProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}
