package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.ruler.utility.Resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class BundleBuilder extends ResourceBuilder {

	protected BundleBuilder(IBaseBundle theBundle, BundleSettings theBundleSettings) {
		super(theBundle, theBundleSettings);
	}

	/**
	 * Creates the appropriate IBaseBundle for a given FhirContext
	 * 
	 * @param theFhirContext    the FHIR version to generate a Bundle for
	 * @param theBundleSettings the settings for the Bundle
	 * @return the Bundle
	 */
	public static IBaseBundle create(FhirContext theFhirContext, BundleSettings theBundleSettings) {
		return create(theFhirContext.getVersion().getVersion(), theBundleSettings);
	}

	/**
	 * Creates the appropriate IBaseBundle for a given FhirVersionEnum
	 * 
	 * @param theFhirVersionEnum the FHIR version to generate a Bundle for
	 * @param theBundleSettings  the settings for the Bundle
	 * @return the Bundle
	 */
	@SuppressWarnings("unchecked")
	public static IBaseBundle create(FhirVersionEnum theFhirVersionEnum, BundleSettings theBundleSettings) {
		checkNotNull(theFhirVersionEnum, theBundleSettings);
		checkArgument(theBundleSettings instanceof BundleSettings);

		Class myClass = null;
		switch (theFhirVersionEnum) {
			case DSTU2:
				myClass = ca.uhn.fhir.model.dstu2.resource.Bundle.class;
				break;
			case DSTU2_1:
				myClass = org.hl7.fhir.dstu2016may.model.Bundle.class;
				break;
			case DSTU2_HL7ORG:
				myClass = org.hl7.fhir.dstu2.model.Bundle.class;
				break;
			case DSTU3:
				myClass = org.hl7.fhir.dstu3.model.Bundle.class;
				break;
			case R4:
				myClass = org.hl7.fhir.r4.model.Bundle.class;
				break;
			case R5:
				myClass = org.hl7.fhir.r5.model.Bundle.class;
				break;
			default:
				throw new IllegalArgumentException(String.format("BundleBuilder.create does not support FHIR version %s",
						theFhirVersionEnum.getFhirVersionString()));
		}
		BundleBuilder myBuilder = new BundleBuilder((IBaseBundle) Resources
				.newResource(myClass, theBundleSettings.id()),
				theBundleSettings);
		return myBuilder.getResource();
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
		// no timestamp
	}

	@Override
	protected void initializeDstu2_1() {
		super.initializeDstu2_1();
		org.hl7.fhir.dstu2016may.model.Bundle myBundle = (org.hl7.fhir.dstu2016may.model.Bundle) getResource();
		myBundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(getSettings().type()));
		// no timestamp
	}

	@Override
	protected void initializeDstu2_HL7Org() {
		super.initializeDstu2_HL7Org();
		org.hl7.fhir.dstu2.model.Bundle myBundle = (org.hl7.fhir.dstu2.model.Bundle) getResource();
		myBundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(getSettings().type()));
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
