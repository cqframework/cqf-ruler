package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Strings;
import org.hl7.fhir.dstu2016may.model.CodeableConcept;
import org.hl7.fhir.dstu2016may.model.Coding;
import org.hl7.fhir.dstu2016may.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Enumerations.CompositionStatus;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.valueset.CompositionStatusEnum;

public class CompositionBuilder<T extends IBaseResource> extends ResourceBuilder<CompositionBuilder<T>, T> {

	protected String myStatus;
	protected String myTitle;
	protected CodeableConceptSettings myType;
	protected Date myDate;
	protected String mySubject;
	protected String myAuthor;
	protected String myCustodian;

	public CompositionBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	@Override
	public CompositionBuilder<T> withDefaults() {

		checkNotNull(myType, myStatus, myAuthor, myTitle);
		checkArgument(!myType.getCodingSettings().isEmpty() && myType.getCodingSettings().size() == 1);
		super.withDefaults();

		if (myDate == null) {
			myDate = new Date();
		}

		return this;
	}

	public CompositionBuilder<T> withStatus(String theStatus) {
		checkNotNull(theStatus);

		myStatus = theStatus;

		return this;
	}

	public CompositionBuilder<T> withTitle(String theTitle) {
		checkNotNull(theTitle);

		myTitle = theTitle;

		return this;
	}

	public CompositionBuilder<T> withType(CodeableConceptSettings theType) {
		checkNotNull(theType);

		myType = theType;

		return this;
	}

	public CompositionBuilder<T> withDate(Date theDate) {
		myDate = theDate;

		return this;
	}

	public CompositionBuilder<T> withSubject(String theSubject) {
		mySubject = theSubject;
		if (Strings.isNullOrEmpty(mySubject) || mySubject.startsWith("Patient/")) {
			return this;
		}
		mySubject = "Patient/" + mySubject;

		return this;
	}

	public CompositionBuilder<T> withAuthor(String theAuthor) {
		checkNotNull(theAuthor);
		checkArgument(theAuthor.startsWith("Practitioner") || theAuthor.startsWith("PractitionerRole")
				|| theAuthor.startsWith("Device") || theAuthor.startsWith("Patient")
				|| theAuthor.startsWith("RelatedPerson") || theAuthor.startsWith("Organization"));
		myAuthor = theAuthor;

		return this;
	}

	public CompositionBuilder<T> withCustodian(String theCustodian) {
		myCustodian = theCustodian;
		if (Strings.isNullOrEmpty(myCustodian) || myCustodian.startsWith("Organization/")) {
			return this;
		}
		myCustodian = "Organization/" + myCustodian;

		return this;
	}
	
	private CodingSettings myCodingSettings;

	private CodingSettings getTypeSettings() {
		if (myCodingSettings == null) {
			myCodingSettings = myType.getCodingSettings().toArray(new CodingSettings[0])[0];
		}
		return myCodingSettings;
	}

	@Override
	protected void initializeDstu2() {
		super.initializeDstu2();
		ca.uhn.fhir.model.dstu2.resource.Composition composition = (ca.uhn.fhir.model.dstu2.resource.Composition) myResource;

		List<ResourceReferenceDt> author = new ArrayList<>();
		author.add(new ResourceReferenceDt(myAuthor));

		composition.setStatus(CompositionStatusEnum.forCode(myStatus))
				.setSubject(new ResourceReferenceDt(mySubject))
				.setTitle(myTitle)
				.setType(new CodeableConceptDt()
						.addCoding(new CodingDt()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.setAuthor(author)
				.setCustodian(new ResourceReferenceDt(myCustodian));
	}

	@Override
	protected void initializeDstu2_1() {
		super.initializeDstu2_1();
		org.hl7.fhir.dstu2016may.model.Composition composition = (org.hl7.fhir.dstu2016may.model.Composition) myResource;

		composition
				.setStatus(org.hl7.fhir.dstu2016may.model.Composition.CompositionStatus.fromCode(myStatus))
				.setSubject(new Reference(mySubject))
				.setTitle(myTitle)
				.setType(new CodeableConcept()
						.addCoding(new Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new Reference(myAuthor))
				.setCustodian(new Reference(myCustodian));
	}

	@Override
	protected void initializeDstu2_HL7Org() {
		super.initializeDstu2_HL7Org();
		org.hl7.fhir.dstu2.model.Composition composition = (org.hl7.fhir.dstu2.model.Composition) myResource;

		composition
				.setStatus(org.hl7.fhir.dstu2.model.Composition.CompositionStatus.fromCode(myStatus))
				.setSubject(new org.hl7.fhir.dstu2.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.dstu2.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu2.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu2.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.dstu2.model.Reference(myCustodian));
	}

	@Override
	protected void initializeDstu3() {
		super.initializeDstu3();
		org.hl7.fhir.dstu3.model.Composition composition = (org.hl7.fhir.dstu3.model.Composition) myResource;

		composition
				.setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.fromCode(myStatus))
				.setSubject(new org.hl7.fhir.dstu3.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.dstu3.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu3.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu3.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.dstu3.model.Reference(myCustodian));
	}

	@Override
	protected void initializeR4() {
		super.initializeR4();
		org.hl7.fhir.r4.model.Composition composition = (org.hl7.fhir.r4.model.Composition) myResource;

		composition
				.setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.fromCode(myStatus))
				.setSubject(new org.hl7.fhir.r4.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.r4.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r4.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.r4.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.r4.model.Reference(myCustodian));

	}

	@Override
	protected void initializeR5() {
		super.initializeR5();
		org.hl7.fhir.r5.model.Composition composition = (org.hl7.fhir.r5.model.Composition) myResource;

		composition
				.setStatus(CompositionStatus.fromCode(myStatus))
				.setSubject(new org.hl7.fhir.r5.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.r5.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r5.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.r5.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.r5.model.Reference(myCustodian));
	}
}
