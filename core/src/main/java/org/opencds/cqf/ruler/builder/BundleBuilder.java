package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleBuilder<T extends IBaseBundle> extends ResourceBuilder<BundleBuilder<T>, T> {

	protected String myType;
	protected Date myTimestamp;

	public BundleBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	@Override
	public BundleBuilder<T> withDefaults() {
		checkNotNull(myType);
		super.withDefaults();

		if (myTimestamp == null) {
			myTimestamp = new Date();
		}

		return this;
	}

	public BundleBuilder<T> withType(String theType) {
		myType = theType;

		return this;
	}

	public BundleBuilder<T> addTimestamp(Date theTimestamp) {
		myTimestamp = theTimestamp;

		return this;
	}

	@Override
	protected void initializeDstu2() {
		super.initializeDstu2();
		ca.uhn.fhir.model.dstu2.resource.Bundle bundle = (ca.uhn.fhir.model.dstu2.resource.Bundle) myResource;
		bundle.setType(ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum.forCode(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_1() {
		super.initializeDstu2_1();
		org.hl7.fhir.dstu2016may.model.Bundle bundle = (org.hl7.fhir.dstu2016may.model.Bundle) myResource;
		bundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_HL7Org() {
		super.initializeDstu2_HL7Org();
		org.hl7.fhir.dstu2.model.Bundle bundle = (org.hl7.fhir.dstu2.model.Bundle) myResource;
		bundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu3() {
		super.initializeDstu3();
		org.hl7.fhir.dstu3.model.Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) myResource;

		bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.dstu3.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));
		// no timestamp
	}

	@Override
	protected void initializeR4() {
		super.initializeR4();
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) myResource;

		bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.r4.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));

		bundle.setTimestamp(myTimestamp);

	}

	@Override
	protected void initializeR5() {
		super.initializeR5();
		org.hl7.fhir.r5.model.Bundle bundle = (org.hl7.fhir.r5.model.Bundle) myResource;

		bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.r5.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));

		bundle.setTimestamp(myTimestamp);
	}
}
