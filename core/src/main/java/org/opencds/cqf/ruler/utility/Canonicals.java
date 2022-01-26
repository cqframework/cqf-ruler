package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class Canonicals {

	private Canonicals() {
	}

	interface CanonicalParts {
		String getUrl();

		String getIdPart();

		String getResourceType();

		String getVersion();

		String getFragment();

	}

	/**
	 * Gets the Resource type component of a canonical url
	 * 
	 * @param <CanonicalType> A CanonicalType
	 * @param theCanonicalType the canonical url to parse
	 * @return the Resource type, or null if one can not be parsed
	 */
	public static <CanonicalType extends IPrimitiveType<String>> String getResourceType(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getResourceType(theCanonicalType.getValue());
	}

	/**
	 * Gets the ResourceType component of a canonical url
	 * 
	 * @param theCanonical the canonical url to parse
	 * @return the ResourceType, or null if one can not be parsed
	 */

	public static String getResourceType(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("/")) {
			return null;
		}

		theCanonical = theCanonical.replace(theCanonical.substring(theCanonical.lastIndexOf("/")), "");
		return theCanonical.contains("/") ? theCanonical.substring(theCanonical.lastIndexOf("/") + 1) : theCanonical;
	}

	/**
	 * Gets the ID component of a canonical url. Does not include resource name if present in the url.
	 * 
	 * @param <CanonicalType> A CanonicalType
	 * @param theCanonicalType the canonical url to parse
	 * @return the Id, or null if one can not be parsed
	 */
	public static <CanonicalType extends IPrimitiveType<String>>  String getIdPart(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getIdPart(theCanonicalType.getValue());
	}

	/**
	 * Gets the ID component of a canonical url. Does not include resource name if present in the url.
	 * 
	 * @param theCanonical the canonical url to parse
	 * @return the Id, or null if one can not be parsed
	 */
	public static String getIdPart(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("/")) {
			return null;
		}

		int lastIndex = Math.min(theCanonical.lastIndexOf("|"), theCanonical.lastIndexOf("#"));
		if (lastIndex == -1) {
			lastIndex = theCanonical.length();
		}

		return theCanonical.substring(theCanonical.lastIndexOf("/") + 1, lastIndex);
	}

	/**
	 * Gets the Version component of a canonical url
	 * 
	 * @param <CanonicalType> A CanonicalType
	 * @param theCanonicalType the canonical url to parse
	 * @return the Version, or null if one can not be parsed
	 */
	public static  <CanonicalType extends IPrimitiveType<String>> String getVersion(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getVersion(theCanonicalType.getValue());
	}

	/**
	 * Gets the Version component of a canonical url
	 *
	 * @param theCanonical the canonical url to parse
	 * @return the Version, or null if one can not be parsed
	 */
	public static String getVersion(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("|")) {
			return null;
		}

		int lastIndex = theCanonical.lastIndexOf("#");
		if (lastIndex == -1) {
			lastIndex = theCanonical.length();
		}

		return theCanonical.substring(theCanonical.lastIndexOf("|") + 1, lastIndex);
	}

	/**
	 * Gets the Url component of a canonical url. Includes the base url, the resource type, and the id if present.
	 * 
	 * @param <CanonicalType> A CanonicalType
	 * @param theCanonicalType the canonical url to parse
	 * @return the Url, or null if one can not be parsed
	 */
	public static <CanonicalType extends IPrimitiveType<String>>  String getUrl(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getUrl(theCanonicalType.getValue());
	}


	/**
	 * Get the Url component of a canonical url. Includes the base url, the resource type, and the id if present.
	 * 
	 * @param theCanonical the canonical url to parse
	 * @return the Url, or null if one can not be parsed
	 */
	public static String getUrl(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("/")) {
			return null;
		}

		int lastIndex = Math.min(theCanonical.lastIndexOf("|"), theCanonical.lastIndexOf("#"));
		if (lastIndex == -1) {
			lastIndex = theCanonical.length();
		}

		return theCanonical.substring(0, lastIndex);
	}

	/**
	 * Gets the Fragment component of a canonical url.
	 * 
	 * @param <CanonicalType> A CanonicalType
	 * @param theCanonicalType the canonical url to parse
	 * @return the Fragment, or null if one can not be parsed
	 */
	public static <CanonicalType extends IPrimitiveType<String>> String  getFragment(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getFragment(theCanonicalType.getValue());
	}


	/**
	 * Gets the Fragment component of a canonical url.
	 * 
	 * @param theCanonical the canonical url to parse
	 * @return the Fragment, or null if one can not be parsed
	 */
	public static String getFragment(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("#")) {
			return null;
		}

		return theCanonical.substring(theCanonical.lastIndexOf("#") + 1);
	}

	public static <CanonicalType extends IPrimitiveType<String>> CanonicalParts getCanonicalParts(
			CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return getCanonicalParts(theCanonicalType.getValue());

	}

	public static CanonicalParts getCanonicalParts(String theCanonical) {
		checkNotNull(theCanonical);

		String url = getUrl(theCanonical);
		String resourceType = getResourceType(theCanonical);
		String id = getIdPart(theCanonical);
		String version = getVersion(theCanonical);
		String fragment = getFragment(theCanonical);
		return new CanonicalParts() {
			@Override
			public String getUrl() {
				return url;
			}

			@Override
			public String getResourceType() {
				return resourceType;
			}

			@Override
			public String getIdPart() {
				return id;
			}

			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public String getFragment() {
				return fragment;
			}
		};
	}
}
