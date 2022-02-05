package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class BundleSettings {

	public static final String DEFAULT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986";
	public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";

	private String id;
	private String type;
	private List<String> profile;
	private Pair<String, String> identifier;
	private Date timestamp;

	public BundleSettings withDefaults() {
		checkNotNull(this.type);
		if (Strings.isNullOrEmpty(this.id)) {
			this.id = UUID.randomUUID().toString();
		}
		if (this.identifier == null) {
			this.identifier = new ImmutablePair<>(DEFAULT_IDENTIFIER_SYSTEM,
					DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString());
		}
		if (this.timestamp == null) {
			this.timestamp = new Date();
		}

		return this;
	}

	public BundleSettings addId(String theId) {
		this.id = theId;
		return this;
	}

	public BundleSettings addType(String theType) {
		this.type = theType;
		return this;
	}

	public BundleSettings addProfile(String theProfile) {
		List<String> myProfile = new ArrayList<>();
		myProfile.add(theProfile);
		this.addProfile(myProfile);
		return this;
	}

	public BundleSettings addProfile(List<String> theProfile) {
		this.profile = theProfile;
		return this;
	}

	public BundleSettings addIdentifier(Pair<String, String> theIdentifier) {
		this.identifier = theIdentifier;
		return this;
	}

	public BundleSettings addTimestamp(Date theTimestamp) {
		this.timestamp = theTimestamp;
		return this;
	}

	public String id() {
		return this.id;
	}

	public String type() {
		return this.type;
	}

	public List<String> profile() {
		return this.profile;
	}

	public Pair<String, String> identifier() {
		return this.identifier;
	}

	public Date timestamp() {
		return this.timestamp;
	}
}
