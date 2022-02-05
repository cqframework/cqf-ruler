package org.opencds.cqf.ruler.utility;

public interface IResourceSettings {
	public static final String DEFAULT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986";
	public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";

	public abstract ResourceSettings withDefaults();
}
