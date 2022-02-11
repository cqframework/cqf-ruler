package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseResource;

public abstract class ResourceBuilder {

	private IBaseResource myResource;
	private IResourceSettings myResourceSettings;

	protected ResourceBuilder(IBaseResource theResource, IResourceSettings theResourceSettings) {
		checkNotNull(theResource);
		checkNotNull(theResourceSettings);
		checkArgument(theResourceSettings instanceof ResourceSettings);

		myResource = theResource;
		myResourceSettings = theResourceSettings;
		initializeResource();
	}

	protected void initializeResource() {
		switch (myResource.getStructureFhirVersionEnum()) {
			case DSTU2:
				initializeDstu2();
			case DSTU2_1:
				initializeDstu2_1();
			case DSTU2_HL7ORG:
				initializeDstu2_HL7Org();
			case DSTU3:
				initializeDstu3();
			case R4:
				initializeR4();
			case R5:
				initializeR5();
			default:
				throw new IllegalArgumentException(
						String.format("ResourceBuilder.initializeResource does not support FHIR version %s",
								myResource.getStructureFhirVersionEnum().getFhirVersionString()));
		}
	}

	protected IBaseResource getResource() {
		return myResource;
	}

	protected IResourceSettings getSettings() {
		return myResourceSettings;
	}

	private void addProfiles() {
		myResourceSettings.profile()
				.forEach(profile -> myResource.getMeta().addProfile(profile));
	}

	protected void initializeDstu2() {
		addProfiles();
		// no identifier
	}

	protected void initializeDstu2_1() {
		addProfiles();
		// no identifier
	}

	protected void initializeDstu2_HL7Org() {
		addProfiles();
		// no identifier
	}

	protected void initializeDstu3() {
		addProfiles();
	}

	protected void initializeR4() {
		addProfiles();
	}

	protected void initializeR5() {
		addProfiles();
	}
}
