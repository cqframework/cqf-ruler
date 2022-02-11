package org.opencds.cqf.ruler.builder;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public interface IResourceSettings {
	public static final String DEFAULT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986";
	public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";

	public abstract ResourceSettings withDefaults();

	public String id();

	public List<String> profile();

	public Pair<String, String> identifier();
}
