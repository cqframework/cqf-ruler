package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.utility.Resources;

public abstract class ResourceBuilder {

	protected IBaseResource myResource;
	protected IResourceSettings myResourceSettings;

	protected <T extends IBaseResource> ResourceBuilder(Class<T> theResourceClass,
			IResourceSettings theResourceSettings) {
		checkNotNull(theResourceClass, theResourceSettings);

		myResourceSettings = theResourceSettings;
		myResource = Resources
				.newResource(theResourceClass, theResourceSettings.id());

		initializeResource();
	}

	protected void initializeResource() {
		switch (myResource.getStructureFhirVersionEnum()) {
			case DSTU2:
				initializeDstu2();
				break;
			case DSTU2_1:
				initializeDstu2_1();
				break;
			case DSTU2_HL7ORG:
				initializeDstu2_HL7Org();
				break;
			case DSTU3:
				initializeDstu3();
				break;
			case R4:
				initializeR4();
				break;
			case R5:
				initializeR5();
				break;
			default:
				throw new IllegalArgumentException(
						String.format("ResourceBuilder.initializeResource does not support FHIR version %s",
								myResource.getStructureFhirVersionEnum().getFhirVersionString()));
		}
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
