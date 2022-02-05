package org.opencds.cqf.ruler.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class ResourceSettings implements IResourceSettings {

	private String id;
	private List<String> profile;
	private Pair<String, String> identifier;

	public ResourceSettings withDefaults() {
		if (Strings.isNullOrEmpty(this.id)) {
			this.id = UUID.randomUUID().toString();
		}

		if (this.identifier == null) {
			this.identifier = new ImmutablePair<>(DEFAULT_IDENTIFIER_SYSTEM,
					DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString());
		}

		return this;
	}

	public ResourceSettings addId(String theId) {
		this.id = theId;

		return this;
	}

	public ResourceSettings addProfile(String theProfile) {
		List<String> myProfile = new ArrayList<>();
		myProfile.add(theProfile);
		this.addProfile(myProfile);

		return this;
	}

	public ResourceSettings addProfile(List<String> theProfile) {
		this.profile = theProfile;

		return this;
	}

	public ResourceSettings addIdentifier(Pair<String, String> theIdentifier) {
		this.identifier = theIdentifier;

		return this;
	}

	public String id() {
		return this.id;
	}

	public List<String> profile() {
		return this.profile;
	}

	public Pair<String, String> identifier() {
		return this.identifier;
	}
}
