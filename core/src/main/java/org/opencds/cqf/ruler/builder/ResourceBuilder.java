package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.common.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.utility.Resources;

public abstract class ResourceBuilder<SELF, T extends IBaseResource> {

	public static final String DEFAULT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986";
	public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";

	private Class<T> myResourceClass;

	protected String myId;
	protected List<String> myProfile;
	protected Pair<String, String> myIdentifier;
	protected T myResource;

	protected ResourceBuilder(Class<T> theResourceClass) {
		checkNotNull(theResourceClass);
		myResourceClass = theResourceClass;
	}

	@SuppressWarnings("unchecked")
	private SELF self() {
		return (SELF) this;
	}

	public SELF withId(String theId) {
		myId = theId;

		return self();
	}

	public SELF withProfile(String theProfile) {
		List<String> profile = new ArrayList<>();
		profile.add(theProfile);
		withProfile(profile);

		return self();
	}

	public SELF withProfile(List<String> theProfile) {
		myProfile = theProfile;

		return self();
	}

	public SELF addIdentifier(Pair<String, String> theIdentifier) {
		myIdentifier = theIdentifier;

		return self();
	}

	public SELF withDefaults() {
		if (Strings.isNullOrEmpty(myId)) {
			myId = UUID.randomUUID().toString();
		}

		if (myIdentifier == null) {
			myIdentifier = new ImmutablePair<>(DEFAULT_IDENTIFIER_SYSTEM,
					DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString());
		}

		return self();
	}

	public T build() {
		myResource = Resources
				.newResource(myResourceClass, myId);

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

		return myResource;
	}

	private void addProfiles() {
		myProfile
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
