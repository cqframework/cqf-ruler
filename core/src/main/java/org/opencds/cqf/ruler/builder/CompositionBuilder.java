package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu2016may.model.CodeableConcept;
import org.hl7.fhir.dstu2016may.model.Coding;
import org.hl7.fhir.dstu2016may.model.Identifier;
import org.hl7.fhir.dstu2016may.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Enumerations.CompositionStatus;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.valueset.CompositionStatusEnum;

public class CompositionBuilder<T extends IBaseResource> extends ResourceBuilder<CompositionBuilder<T>, T> {

	protected String myStatus;
	protected String myTitle;
	protected CodeableConceptSettings myType;
	protected String mySubject;
	protected String myAuthor;
	protected String myCustodian;

	protected Date myDate = new Date();

	public CompositionBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	public CompositionBuilder(Class<T> theResourceClass, String theId) {
		super(theResourceClass, theId);
	}

	public CompositionBuilder(Class<T> theResourceClass, String theId, CodeableConceptSettings theType, String theStatus,
			String theAuthor, String theTitle) {
		this(theResourceClass, theId);
		checkNotNull(theType, theStatus, theAuthor, theTitle);

		myType = theType;
		myStatus = theStatus;
		myAuthor = theAuthor;
		myTitle = theTitle;
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
		mySubject = ensurePatientReference(theSubject);

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
		myCustodian = ensureOrganizationReference(theCustodian);

		return this;
	}

	@Override
	public T build() {
		checkNotNull(myType, myStatus, myAuthor, myTitle);
		checkArgument(!myType.getCodingSettings().isEmpty() &&
				myType.getCodingSettings().size() == 1);

		return super.build();
	}

	private CodingSettings getTypeSetting() {
		return myType.getCodingSettingsArray()[0];
	}

	@Override
	protected void initializeDstu2(T theResource) {
		super.initializeDstu2(theResource);
		ca.uhn.fhir.model.dstu2.resource.Composition composition = (ca.uhn.fhir.model.dstu2.resource.Composition) theResource;

		List<ResourceReferenceDt> author = new ArrayList<>();
		author.add(new ResourceReferenceDt(myAuthor));

		composition
				.setIdentifier(new IdentifierDt(myIdentifier.getKey(), myIdentifier.getValue()))
				.setStatus(CompositionStatusEnum.forCode(myStatus))
				.setSubject(new ResourceReferenceDt(mySubject))
				.setTitle(myTitle)
				.setType(new CodeableConceptDt()
						.addCoding(new CodingDt()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.setAuthor(author)
				.setCustodian(new ResourceReferenceDt(myCustodian));
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);
		org.hl7.fhir.dstu2016may.model.Composition composition = (org.hl7.fhir.dstu2016may.model.Composition) theResource;

		composition
				.setIdentifier(new Identifier().setSystem(myIdentifier.getKey()).setValue(myIdentifier.getValue()))
				.setStatus(org.hl7.fhir.dstu2016may.model.Composition.CompositionStatus.valueOf(myStatus))
				.setSubject(new Reference(mySubject))
				.setTitle(myTitle)
				.setType(new CodeableConcept()
						.addCoding(new Coding()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.addAuthor(new Reference(myAuthor))
				.setCustodian(new Reference(myCustodian));
	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);
		org.hl7.fhir.dstu2.model.Composition composition = (org.hl7.fhir.dstu2.model.Composition) theResource;

		composition
				.setIdentifier(new org.hl7.fhir.dstu2.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setStatus(org.hl7.fhir.dstu2.model.Composition.CompositionStatus.valueOf(myStatus))
				.setSubject(new org.hl7.fhir.dstu2.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.dstu2.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu2.model.Coding()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu2.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.dstu2.model.Reference(myCustodian));
	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);
		org.hl7.fhir.dstu3.model.Composition composition = (org.hl7.fhir.dstu3.model.Composition) theResource;

		composition
				.setIdentifier(new org.hl7.fhir.dstu3.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.valueOf(myStatus))
				.setSubject(new org.hl7.fhir.dstu3.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.dstu3.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu3.model.Coding()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu3.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.dstu3.model.Reference(myCustodian));
	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);
		org.hl7.fhir.r4.model.Composition composition = (org.hl7.fhir.r4.model.Composition) theResource;

		composition
				.setIdentifier(new org.hl7.fhir.r4.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.valueOf(myStatus))
				.setSubject(new org.hl7.fhir.r4.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.r4.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r4.model.Coding()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.addAuthor(new org.hl7.fhir.r4.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.r4.model.Reference(myCustodian));
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);
		org.hl7.fhir.r5.model.Composition composition = (org.hl7.fhir.r5.model.Composition) theResource;

		composition
				.setIdentifier(new org.hl7.fhir.r5.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setStatus(CompositionStatus.valueOf(myStatus))
				.setSubject(new org.hl7.fhir.r5.model.Reference(mySubject))
				.setTitle(myTitle)
				.setType(new org.hl7.fhir.r5.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r5.model.Coding()
								.setSystem(getTypeSetting().getSystem())
								.setCode(getTypeSetting().getCode())
								.setDisplay(getTypeSetting().getDisplay())))
				.addAuthor(new org.hl7.fhir.r5.model.Reference(myAuthor))
				.setCustodian(new org.hl7.fhir.r5.model.Reference(myCustodian));
	}
}
