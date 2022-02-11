package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleBuilder extends ResourceBuilder {

	protected <T extends IBaseBundle> BundleBuilder(Class<T> theBundleClass, BundleSettings theBundleSettings) {
		super(theBundleClass, theBundleSettings);
	}

	/**
	 * Creates the appropriate IBaseBundle for a given bundle class
	 * 
	 * @param theBundleClass    the FHIR version to generate a Bundle for
	 * @param theBundleSettings the settings for the Bundle
	 * @return the Bundle
	 */
	public static <T extends IBaseBundle> T create(Class<T> theBundleClass, BundleSettings theBundleSettings) {
		checkNotNull(theBundleClass, theBundleSettings);

		BundleBuilder myBuilder = new BundleBuilder(theBundleClass,
				theBundleSettings);

		@SuppressWarnings("unchecked")
		T myBundle = (T) myBuilder.getResource();
		return myBundle;
	}

	@Override
	protected IBaseBundle getResource() {
		return (IBaseBundle) super.getResource();
	}

	@Override
	protected BundleSettings getSettings() {
		return (BundleSettings) super.getSettings();
	}

	@Override
	protected void initializeDstu2() {
		super.initializeDstu2();
		ca.uhn.fhir.model.dstu2.resource.Bundle myBundle = (ca.uhn.fhir.model.dstu2.resource.Bundle) getResource();
		myBundle.setType(ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum.forCode(getSettings().type()));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_1() {
		super.initializeDstu2_1();
		org.hl7.fhir.dstu2016may.model.Bundle myBundle = (org.hl7.fhir.dstu2016may.model.Bundle) getResource();
		myBundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(getSettings().type()));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_HL7Org() {
		super.initializeDstu2_HL7Org();
		org.hl7.fhir.dstu2.model.Bundle myBundle = (org.hl7.fhir.dstu2.model.Bundle) getResource();
		myBundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(getSettings().type()));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu3() {
		super.initializeDstu3();
		org.hl7.fhir.dstu3.model.Bundle myBundle = (org.hl7.fhir.dstu3.model.Bundle) getResource();

		myBundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(getSettings().type()));

		myBundle.setIdentifier(
				new org.hl7.fhir.dstu3.model.Identifier().setSystem(getSettings().identifier().getLeft())
						.setValue(getSettings().identifier().getRight()));
		// no timestamp
	}

	@Override
	protected void initializeR4() {
		super.initializeR4();
		org.hl7.fhir.r4.model.Bundle myBundle = (org.hl7.fhir.r4.model.Bundle) getResource();

		myBundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(getSettings().type()));

		myBundle.setIdentifier(
				new org.hl7.fhir.r4.model.Identifier().setSystem(getSettings().identifier().getLeft())
						.setValue(getSettings().identifier().getRight()));

		myBundle.setTimestamp(getSettings().timestamp());

	}

	@Override
	protected void initializeR5() {
		super.initializeR5();
		org.hl7.fhir.r5.model.Bundle myBundle = (org.hl7.fhir.r5.model.Bundle) getResource();

		myBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(getSettings().type()));

		myBundle.setIdentifier(
				new org.hl7.fhir.r5.model.Identifier().setSystem(getSettings().identifier().getLeft())
						.setValue(getSettings().identifier().getRight()));

		myBundle.setTimestamp(getSettings().timestamp());
	}
}
