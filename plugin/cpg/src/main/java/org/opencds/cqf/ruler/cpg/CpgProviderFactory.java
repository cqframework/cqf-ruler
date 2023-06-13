package org.opencds.cqf.ruler.cpg;


import org.opencds.cqf.ruler.cpg.dstu3.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.dstu3.provider.LibraryEvaluationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;

public class CpgProviderFactory {
	@Autowired 
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getCqlExecutionProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(CqlExecutionProvider.class);
			case R4:
				return myApplicationContext.getBean(org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider.class);
			default:
				throw new ConfigurationException("CqlExecutionProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getLibraryEvalProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				return myApplicationContext.getBean(LibraryEvaluationProvider.class);
			case R4:
				return myApplicationContext
					.getBean(org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider.class);
			default:
				throw new ConfigurationException("LibraryEvaluationProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}
