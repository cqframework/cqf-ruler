package org.opencds.cqf.ruler.plugin.utility;

import java.util.Objects;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.FhirVersionEnum;

public interface CanonicalUtilities extends IdUtilities {

	public interface CanonicalParts<IdType extends IIdType> {
		public String getVersion();

		public String getUrl();

		public IdType getId();
	}

	public default <CanonicalType extends IPrimitiveType<String>> String getId(CanonicalType canonicalType) {
		if (canonicalType == null || !canonicalType.hasValue()) {
			throw new IllegalArgumentException("CanonicalType must have a value for id extraction");
		}

		return this.getId(canonicalType.getValue());
	}

	@SuppressWarnings("unchecked")
	public default <CanonicalType extends IPrimitiveType<String>, IdType extends IIdType> IdType getIdElement(
			CanonicalType canonicalType) {
		if (canonicalType == null || !canonicalType.hasValue()) {
			throw new IllegalArgumentException("CanonicalType must have a value for id extraction");
		}

		String id = this.getId(canonicalType.getValue());
		String resourceName = this.getResourceName(canonicalType.getValue());

		return (IdType) this.createId(canonicalType.getClass(), resourceName, id);
	}

	public default <CanonicalType extends IPrimitiveType<String>> String getResourceName(CanonicalType canonicalType) {
		if (canonicalType == null || !canonicalType.hasValue()) {
			throw new IllegalArgumentException("CanonicalType must have a value for id extraction");
		}

		return this.getResourceName(canonicalType.getValue());
	}

	public default String getResourceName(String theCanonical) {
		Objects.requireNonNull("theCanonical must not be null");
		if (!theCanonical.contains("/")) {
			return null;
		}

		theCanonical = theCanonical.replace(theCanonical.substring(theCanonical.lastIndexOf("/")), "");
		return theCanonical.contains("/") ? theCanonical.substring(theCanonical.lastIndexOf("/") + 1) : theCanonical;
	}

	public default String getId(String theCanonical) {
		if (!theCanonical.contains("/")) {
			return null;
		}

		theCanonical = theCanonical.replace(theCanonical.substring(theCanonical.lastIndexOf("/")), "");
		return theCanonical.contains("/") ? theCanonical.substring(theCanonical.lastIndexOf("/") + 1) : theCanonical;
	}

	public default String getVersion(String theCanonical) {
		if (!theCanonical.contains("|")) {
			return null;
		}

		String[] urlParts = theCanonical.split("\\|");
		if (urlParts.length <= 1) {
			return null;
		}

		return urlParts[1];
	}

	public default String getUrl(String theCanonical) {
		if (!theCanonical.contains("|")) {
			return theCanonical;
		}

		String[] urlParts = theCanonical.split("\\|");
		return urlParts[0];
	}

	public default <CanonicalType extends IPrimitiveType<String>, IdType extends IIdType> CanonicalParts<IdType> getCanonicalParts(
			CanonicalType theCanonicalType) {
		Objects.requireNonNull("canonicalUrl must not be null");

		String version = this.getVersion(theCanonicalType.getValue());
		String url = this.getUrl(theCanonicalType.getValue());
		IdType id = this.getIdElement(theCanonicalType);
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

	public default <IdType extends IIdType> CanonicalParts<IdType> getCanonicalParts(FhirVersionEnum theFhirVersionEnum,
			String theCanonical) {
		Objects.requireNonNull("canonicalUrl must not be null");

		String version = this.getVersion(theCanonical);
		String url = this.getUrl(theCanonical);
		String resourceType = this.getResourceName(theCanonical);
		String id = this.getId(theCanonical);
		IdType idElement = this.createId(theFhirVersionEnum, resourceType, id);
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
