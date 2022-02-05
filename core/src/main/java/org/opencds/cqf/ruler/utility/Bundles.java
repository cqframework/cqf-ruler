package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class Bundles {

	private Bundles() {
	}

	/**
	 * Creates the appropriate IBaseBundle for a given FhirContext
	 * 
	 * @param theFhirContext    the FHIR version to generate a Bundle for
	 * @param theBundleSettings the settings for the Bundle
	 * @return the Bundle
	 */
	public static IBaseBundle newBundle(FhirContext theFhirContext, BundleSettings theBundleSettings) {
		checkNotNull(theFhirContext, theBundleSettings);

		return newBundle(theFhirContext.getVersion().getVersion(), theBundleSettings);
	}

	/**
	 * Creates the appropriate IBaseBundle for a given FhirVersionEnum
	 * 
	 * @param theFhirVersionEnum the FHIR version to generate a Bundle for
	 * @param theBundleSettings  the settings for the Bundle
	 * @return the Bundle
	 */
	public static IBaseBundle newBundle(FhirVersionEnum theFhirVersionEnum, IResourceSettings theBundleSettings) {
		checkNotNull(theFhirVersionEnum);
		checkNotNull(theBundleSettings);
		checkArgument(theBundleSettings instanceof BundleSettings);

		BundleSettings settings = (BundleSettings) theBundleSettings;
		switch (theFhirVersionEnum) {
			case DSTU2:
				return InitializeDstu2(settings);
			case DSTU2_1:
				return InitializeDstu2_1(settings);
			case DSTU2_HL7ORG:
				return InitializeDstu2_HL7Org(settings);
			case DSTU3:
				return InitializeDstu3(settings);
			case R4:
				return InitializeR4(settings);
			case R5:
				return InitializeR5(settings);
			default:
				throw new IllegalArgumentException(String.format("newBundle does not support FHIR version %s",
						theFhirVersionEnum.getFhirVersionString()));
		}
	}

	static IBaseBundle InitializeDstu2(BundleSettings theBundleSettings) {
		ca.uhn.fhir.model.dstu2.resource.Bundle bundle = Resources
				.newResource(ca.uhn.fhir.model.dstu2.resource.Bundle.class, theBundleSettings.id());

		bundle.setType(ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum.forCode(theBundleSettings.type()));

		ca.uhn.fhir.model.base.resource.ResourceMetadataMap theMap = new ca.uhn.fhir.model.base.resource.ResourceMetadataMap();
		theBundleSettings.profile()
				.forEach(profile -> theMap.put(ca.uhn.fhir.model.api.ResourceMetadataKeyEnum.PROFILES, profile));
		bundle.setResourceMetadata(theMap);

		// no identifier
		// no timestamp

		return bundle;
	}

	static IBaseBundle InitializeDstu2_1(BundleSettings theBundleSettings) {
		org.hl7.fhir.dstu2016may.model.Bundle bundle = Resources
				.newResource(org.hl7.fhir.dstu2016may.model.Bundle.class, theBundleSettings.id());

		bundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(theBundleSettings.type()));

		org.hl7.fhir.dstu2016may.model.Meta meta = new org.hl7.fhir.dstu2016may.model.Meta();
		theBundleSettings.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		// no identifier
		// no timestamp

		return bundle;
	}

	static IBaseBundle InitializeDstu2_HL7Org(BundleSettings theBundleSettings) {
		org.hl7.fhir.dstu2.model.Bundle bundle = Resources
				.newResource(org.hl7.fhir.dstu2.model.Bundle.class, theBundleSettings.id());

		bundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(theBundleSettings.type()));

		org.hl7.fhir.dstu2.model.Meta meta = new org.hl7.fhir.dstu2.model.Meta();
		theBundleSettings.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		// no identifier
		// no timestamp

		return bundle;
	}

	static IBaseBundle InitializeDstu3(BundleSettings theBundleSettings) {
		org.hl7.fhir.dstu3.model.Bundle bundle = Resources
				.newResource(org.hl7.fhir.dstu3.model.Bundle.class, theBundleSettings.id());

		bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(theBundleSettings.type()));

		org.hl7.fhir.dstu3.model.Meta meta = new org.hl7.fhir.dstu3.model.Meta();
		theBundleSettings.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.dstu3.model.Identifier().setSystem(theBundleSettings.identifier().getLeft())
						.setValue(theBundleSettings.identifier().getRight()));
		// no timestamp

		return bundle;
	}

	static IBaseBundle InitializeR4(BundleSettings theBundleSettings) {
		org.hl7.fhir.r4.model.Bundle bundle = Resources
				.newResource(org.hl7.fhir.r4.model.Bundle.class, theBundleSettings.id());

		bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(theBundleSettings.type()));

		org.hl7.fhir.r4.model.Meta meta = new org.hl7.fhir.r4.model.Meta();
		theBundleSettings.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.r4.model.Identifier().setSystem(theBundleSettings.identifier().getLeft())
						.setValue(theBundleSettings.identifier().getRight()));

		bundle.setTimestamp(theBundleSettings.timestamp());

		return bundle;
	}

	static IBaseBundle InitializeR5(BundleSettings theBundleSettings) {
		org.hl7.fhir.r5.model.Bundle bundle = Resources
				.newResource(org.hl7.fhir.r5.model.Bundle.class, theBundleSettings.id());

		bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(theBundleSettings.type()));

		org.hl7.fhir.r5.model.Meta meta = new org.hl7.fhir.r5.model.Meta();
		theBundleSettings.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.r5.model.Identifier().setSystem(theBundleSettings.identifier().getLeft())
						.setValue(theBundleSettings.identifier().getRight()));

		bundle.setTimestamp(theBundleSettings.timestamp());

		return bundle;
	}
}
