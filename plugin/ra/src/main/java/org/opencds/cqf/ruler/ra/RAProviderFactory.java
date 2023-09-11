package org.opencds.cqf.ruler.ra;

import org.opencds.cqf.ruler.ra.r4.ApproveProvider;
import org.opencds.cqf.ruler.ra.r4.RACodingGapsProvider;
import org.opencds.cqf.ruler.ra.r4.RemediateProvider;
import org.opencds.cqf.ruler.ra.r4.ResolveProvider;
import org.opencds.cqf.ruler.ra.r4.RiskAdjustmentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;

public class RAProviderFactory {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;

	public Object getApproveProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return myApplicationContext.getBean(ApproveProvider.class);
			default:
				throw new ConfigurationException("ApproveProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	public Object getRACodingGapsProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return myApplicationContext.getBean(RACodingGapsProvider.class);
			default:
				throw new ConfigurationException("RACodingGapsProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
	public Object getRemediateProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return myApplicationContext.getBean(RemediateProvider.class);
			default:
				throw new ConfigurationException("RemediateProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
	public Object getResolveProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return myApplicationContext.getBean(ResolveProvider.class);
			default:
				throw new ConfigurationException("ResolveProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
	public Object getRiskAdjustmentProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return myApplicationContext.getBean(RiskAdjustmentProvider.class);
			default:
				throw new ConfigurationException("RiskAdjustmentProvider not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
	}
}
