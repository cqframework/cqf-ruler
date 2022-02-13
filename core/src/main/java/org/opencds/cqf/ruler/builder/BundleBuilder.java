package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleBuilder<T extends IBaseBundle> extends ResourceBuilder<BundleBuilder<T>, T> {

	protected String myType;

	protected Date myTimestamp = new Date();

	public BundleBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	public BundleBuilder<T> withType(String theType) {
		myType = theType;

		return this;
	}

	public BundleBuilder<T> withTimestamp(Date theTimestamp) {
		myTimestamp = theTimestamp;

		return this;
	}

	@Override
	public T build() {
		T resource = super.build();
		checkNotNull(myType);

		return resource;
	}

	@Override
	protected void initializeDstu2(T theResource) {
		super.initializeDstu2(theResource);
		ca.uhn.fhir.model.dstu2.resource.Bundle bundle = (ca.uhn.fhir.model.dstu2.resource.Bundle) theResource;
		bundle.setType(ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum.forCode(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);
		org.hl7.fhir.dstu2016may.model.Bundle bundle = (org.hl7.fhir.dstu2016may.model.Bundle) theResource;
		bundle.setType(org.hl7.fhir.dstu2016may.model.Bundle.BundleType.valueOf(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);
		org.hl7.fhir.dstu2.model.Bundle bundle = (org.hl7.fhir.dstu2.model.Bundle) theResource;
		bundle.setType(org.hl7.fhir.dstu2.model.Bundle.BundleType.valueOf(myType));
		// no identifier
		// no timestamp
	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);
		org.hl7.fhir.dstu3.model.Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) theResource;

		bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.dstu3.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));
		// no timestamp
	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) theResource;

		bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.r4.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));

		bundle.setTimestamp(myTimestamp);
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);
		org.hl7.fhir.r5.model.Bundle bundle = (org.hl7.fhir.r5.model.Bundle) theResource;

		bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(myType));

		bundle.setIdentifier(
				new org.hl7.fhir.r5.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()));

		bundle.setTimestamp(myTimestamp);
	}
}
