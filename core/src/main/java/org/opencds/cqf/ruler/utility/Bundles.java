package org.opencds.cqf.ruler.utility;

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
	 * @param theFhirContext the FHIR version to generate a Bundle for
	 * @param theId          the String representation of the Id of the Bundle
	 * @return the Bundle
	 */
	public static IBaseBundle newBundle(FhirContext theFhirContext, BundleSettings theBundleInitials) {
		checkNotNull(theFhirContext, theBundleInitials);

		return newBundle(theFhirContext.getVersion().getVersion(), theBundleInitials);
	}

	/**
	 * Creates the appropriate IBaseBundle for a given FhirVersionEnum
	 * 
	 * @param theFhirVersionEnum the FHIR version to generate a Bundle for
	 * @param theId              the String representation of the Id of the Bundle
	 *                           to generate
	 * @return the Bundle
	 */
	public static IBaseBundle newBundle(FhirVersionEnum theFhirVersionEnum, BundleSettings theBundleInitials) {
		checkNotNull(theFhirVersionEnum);
		checkNotNull(theBundleInitials);

		switch (theFhirVersionEnum) {
			case DSTU2:
				return InitializeDstu2(theBundleInitials);
			case DSTU2_1:
				return InitializeDstu2_1(theBundleInitials);
			case DSTU2_HL7ORG:
				return InitializeDstu2_HL7Org(theBundleInitials);
			case DSTU3:
				return InitializeDstu3(theBundleInitials);
			case R4:
				return InitializeR4(theBundleInitials);
			case R5:
				return InitializeR5(theBundleInitials);
			default:
				throw new IllegalArgumentException(String.format("newBundle does not support FHIR version %s",
						theFhirVersionEnum.getFhirVersionString()));
		}
	}

	private static IBaseBundle InitializeDstu2(BundleSettings theBundleInitials) {
		ca.uhn.fhir.model.dstu2.resource.Bundle bundle = new ca.uhn.fhir.model.dstu2.resource.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum.forCode(theBundleInitials.type()));

		ca.uhn.fhir.model.base.resource.ResourceMetadataMap theMap = new ca.uhn.fhir.model.base.resource.ResourceMetadataMap();
		theBundleInitials.profile()
				.forEach(profile -> theMap.put(ca.uhn.fhir.model.api.ResourceMetadataKeyEnum.PROFILES, profile));
		bundle.setResourceMetadata(theMap);

		// no identifier
		// no timestamp

		return bundle;
	}

	private static IBaseBundle InitializeDstu2_1(BundleSettings theBundleInitials) {
		org.hl7.fhir.dstu2016may.model.Bundle bundle = new org.hl7.fhir.dstu2016may.model.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(theBundleInitials.type()));

		org.hl7.fhir.dstu2016may.model.Meta meta = new org.hl7.fhir.dstu2016may.model.Meta();
		theBundleInitials.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		// no identifier
		// no timestamp

		return bundle;
	}

	private static IBaseBundle InitializeDstu2_HL7Org(BundleSettings theBundleInitials) {
		org.hl7.fhir.dstu2.model.Bundle bundle = new org.hl7.fhir.dstu2.model.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(theBundleInitials.type()));

		org.hl7.fhir.dstu2.model.Meta meta = new org.hl7.fhir.dstu2.model.Meta();
		theBundleInitials.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		// no identifier
		// no timestamp

		return bundle;
	}

	private static IBaseBundle InitializeDstu3(BundleSettings theBundleInitials) {
		org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(theBundleInitials.type()));

		org.hl7.fhir.dstu3.model.Meta meta = new org.hl7.fhir.dstu3.model.Meta();
		theBundleInitials.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.dstu3.model.Identifier().setSystem(theBundleInitials.identifier().getLeft())
						.setValue(theBundleInitials.identifier().getRight()));
		// no timestamp

		return bundle;
	}

	private static IBaseBundle InitializeR4(BundleSettings theBundleInitials) {
		org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(theBundleInitials.type()));

		org.hl7.fhir.r4.model.Meta meta = new org.hl7.fhir.r4.model.Meta();
		theBundleInitials.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.r4.model.Identifier().setSystem(theBundleInitials.identifier().getLeft())
						.setValue(theBundleInitials.identifier().getRight()));

		bundle.setTimestamp(theBundleInitials.timestamp());

		return bundle;
	}

	private static IBaseBundle InitializeR5(BundleSettings theBundleInitials) {
		org.hl7.fhir.r5.model.Bundle bundle = new org.hl7.fhir.r5.model.Bundle();

		bundle.setId(theBundleInitials.id());
		bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(theBundleInitials.type()));

		org.hl7.fhir.r5.model.Meta meta = new org.hl7.fhir.r5.model.Meta();
		theBundleInitials.profile().forEach(meta::addProfile);
		bundle.setMeta(meta);

		bundle.setIdentifier(
				new org.hl7.fhir.r5.model.Identifier().setSystem(theBundleInitials.identifier().getLeft())
						.setValue(theBundleInitials.identifier().getRight()));

		bundle.setTimestamp(theBundleInitials.timestamp());

		return bundle;
	}
}
