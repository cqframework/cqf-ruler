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

	private final Class<T> myResourceClass;

	protected List<String> myProfile;

	protected String myId = UUID.randomUUID().toString();
	protected Pair<String, String> myIdentifier = new ImmutablePair<>(DEFAULT_IDENTIFIER_SYSTEM,
			DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString());

	protected ResourceBuilder(Class<T> theResourceClass) {
		checkNotNull(theResourceClass);
		myResourceClass = theResourceClass;
	}

	protected ResourceBuilder(Class<T> theResourceClass, String theId) {
		this(theResourceClass);
		checkNotNull(theId);

		myId = theId;
	}

	@SuppressWarnings("unchecked")
	protected SELF self() {
		return (SELF) this;
	}

	public static String ensurePatientReference(String thePatientId) {
		if (Strings.isNullOrEmpty(thePatientId) || thePatientId.startsWith("Patient/")) {
			return thePatientId;
		}
		return "Patient/" + thePatientId;
	}

	public static String ensureOrganizationReference(String theOrganizationId) {
		if (Strings.isNullOrEmpty(theOrganizationId) || theOrganizationId.startsWith("Organization/")) {
			return theOrganizationId;
		}
		return "Organization/" + theOrganizationId;
	}

	public SELF withId(String theId) {
		checkNotNull(theId);

		myId = theId;

		return self();
	}

	public SELF withProfile(String theProfile) {
		List<String> profile = new ArrayList<>();
		profile.add(theProfile);

		return withProfile(profile);
	}

	public SELF withProfile(List<String> theProfile) {
		myProfile = theProfile;

		return self();
	}

	public SELF withIdentifier(Pair<String, String> theIdentifier) {
		myIdentifier = theIdentifier;

		return self();
	}

	public T build() {
		T resource = Resources
				.newResource(myResourceClass, myId);

		switch (resource.getStructureFhirVersionEnum()) {
			case DSTU2:
				initializeDstu2(resource);
				break;
			case DSTU2_1:
				initializeDstu2_1(resource);
				break;
			case DSTU2_HL7ORG:
				initializeDstu2_HL7Org(resource);
				break;
			case DSTU3:
				initializeDstu3(resource);
				break;
			case R4:
				initializeR4(resource);
				break;
			case R5:
				initializeR5(resource);
				break;
			default:
				throw new IllegalArgumentException(
						String.format("ResourceBuilder.initializeResource does not support FHIR version %s",
								resource.getStructureFhirVersionEnum().getFhirVersionString()));
		}

		return resource;
	}

	private void addProfiles(T theResource) {
		myProfile
				.forEach(profile -> theResource.getMeta().addProfile(profile));
	}

	protected void initializeDstu2(T theResource) {
		addProfiles(theResource);
		// no identifier
	}

	protected void initializeDstu2_1(T theResource) {
		addProfiles(theResource);
		// no identifier
	}

	protected void initializeDstu2_HL7Org(T theResource) {
		addProfiles(theResource);
		// no identifier
	}

	protected void initializeDstu3(T theResource) {
		addProfiles(theResource);
	}

	protected void initializeR4(T theResource) {
		addProfiles(theResource);
	}

	protected void initializeR5(T theResource) {
		addProfiles(theResource);
	}
}
