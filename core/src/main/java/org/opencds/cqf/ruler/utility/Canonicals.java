package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.FhirVersionEnum;

public class Canonicals {

	private Canonicals() {
	}

	interface CanonicalParts<IdType extends IIdType> {
		String getVersion();

		String getUrl();

		IdType getId();
	}

	public static <CanonicalType extends IPrimitiveType<String>> String getId(CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.getValue() != null);

		return getId(theCanonicalType.getValue());
	}

	@SuppressWarnings("unchecked")
	public static <CanonicalType extends IPrimitiveType<String>, IdType extends IIdType> IdType getIdElement(
			CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.getValue() != null);

		String id = getId(theCanonicalType.getValue());
		String resourceName = getResourceName(theCanonicalType.getValue());

		return (IdType) Ids.newId(theCanonicalType.getClass(), resourceName, id);
	}

	public static <CanonicalType extends IPrimitiveType<String>> String getResourceName(CanonicalType canonicalType) {
		if (canonicalType == null || !canonicalType.hasValue()) {
			throw new IllegalArgumentException("CanonicalType must have a value for id extraction");
		}

		return getResourceName(canonicalType.getValue());
	}

	public static String getResourceName(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("/")) {
			return null;
		}

		theCanonical = theCanonical.replace(theCanonical.substring(theCanonical.lastIndexOf("/")), "");
		return theCanonical.contains("/") ? theCanonical.substring(theCanonical.lastIndexOf("/") + 1) : theCanonical;
	}

	public static String getId(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("/")) {
			return null;
		}

		return theCanonical.contains("/") ? theCanonical.substring(theCanonical.lastIndexOf("/") + 1) : theCanonical;
	}

	public static String getVersion(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("|")) {
			return null;
		}

		String[] urlParts = theCanonical.split("\\|");
		if (urlParts.length <= 1) {
			return null;
		}

		return urlParts[1];
	}

	public static String getUrl(String theCanonical) {
		checkNotNull(theCanonical);

		if (!theCanonical.contains("|")) {
			return theCanonical;
		}

		String[] urlParts = theCanonical.split("\\|");
		return urlParts[0];
	}

	public static <CanonicalType extends IPrimitiveType<String>, IdType extends IIdType> CanonicalParts<IdType> getCanonicalParts(
			CanonicalType theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.getValue() != null, "theCanonicalType must have a value");

		String version = getVersion(theCanonicalType.getValue());
		String url = getUrl(theCanonicalType.getValue());
		IdType id = getIdElement(theCanonicalType);
		return new CanonicalParts<IdType>() {
			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public IdType getId() {
				return id;
			}

			@Override
			public String getUrl() {
				return url;
			}
		};
	}

	public static <IdType extends IIdType> CanonicalParts<IdType> getCanonicalParts(FhirVersionEnum theFhirVersionEnum,
			String theCanonical) {
		checkNotNull(theFhirVersionEnum);
		checkNotNull(theCanonical);

		String version = getVersion(theCanonical);
		String url = getUrl(theCanonical);
		String resourceType = getResourceName(theCanonical);
		String id = getId(theCanonical);
		IdType idElement = Ids.newId(theFhirVersionEnum, resourceType, id);
		return new CanonicalParts<IdType>() {
			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public IdType getId() {
				return idElement;
			}

			@Override
			public String getUrl() {
				return url;
			}
		};
	}
}
