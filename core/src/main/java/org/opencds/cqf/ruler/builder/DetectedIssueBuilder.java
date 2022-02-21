package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu2016may.model.Identifier;
import org.hl7.fhir.dstu2016may.model.Reference;
import org.hl7.fhir.dstu3.model.DetectedIssue.DetectedIssueStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueEvidenceComponent;
import org.hl7.fhir.r5.model.Enumerations.ObservationStatus;

import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;

public class DetectedIssueBuilder<T extends IBaseResource> extends BaseElementBuilder<DetectedIssueBuilder<T>, T> {

	protected String myStatus;
	protected CodeableConceptSettings myCode;
	protected String myPatient;

	protected List<String> myEvidenceDetail = new ArrayList<>();

	public DetectedIssueBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	public DetectedIssueBuilder(Class<T> theResourceClass, String theId) {
		super(theResourceClass, theId);
	}

	public DetectedIssueBuilder(Class<T> theResourceClass, String theId, String theStatus,
			List<String> theEvidenceDetail) {
		super(theResourceClass, theId);
		checkNotNull(theStatus, theEvidenceDetail);
		myStatus = theStatus;
		myEvidenceDetail = theEvidenceDetail;
	}

	public DetectedIssueBuilder<T> withStatus(String theStatus) {
		myStatus = theStatus;

		return this;
	}

	public DetectedIssueBuilder<T> withCode(CodeableConceptSettings theCode) {
		checkNotNull(theCode);

		myCode = theCode;

		return this;
	}

	public DetectedIssueBuilder<T> withPatient(String thePatient) {
		myPatient = ensurePatientReference(thePatient);

		return this;
	}

	public DetectedIssueBuilder<T> withEvidenceDetail(String theEvidenceDetail) {
		List<String> evidenceDetail = new ArrayList<>();
		evidenceDetail.add(theEvidenceDetail);
		withEvidenceDetail(evidenceDetail);

		return this;
	}

	public DetectedIssueBuilder<T> withEvidenceDetail(List<String> theEvidenceDetail) {
		checkNotNull(theEvidenceDetail);
		myEvidenceDetail = theEvidenceDetail;

		return this;
	}

	@Override
	public T build() {
		checkNotNull(myStatus, myEvidenceDetail);
		checkArgument(!myEvidenceDetail.isEmpty());

		return super.build();
	}

	private CodingSettings getCodeSetting() {
		return myCode.getCodingSettingsArray()[0];
	}

	@Override
	protected void initializeDstu2(T theResource) {
		super.initializeDstu2(theResource);
		ca.uhn.fhir.model.dstu2.resource.DetectedIssue detectedIssue = (ca.uhn.fhir.model.dstu2.resource.DetectedIssue) theResource;

		detectedIssue
				.setIdentifier(new IdentifierDt(myIdentifier.getKey(), myIdentifier.getValue()))
				.setPatient(new ResourceReferenceDt(myPatient));
		myEvidenceDetail.forEach(detectedIssue::setReference);
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);
		org.hl7.fhir.dstu2016may.model.DetectedIssue detectedIssue = (org.hl7.fhir.dstu2016may.model.DetectedIssue) theResource;

		detectedIssue
				.setIdentifier(new Identifier().setSystem(myIdentifier.getKey()).setValue(myIdentifier.getValue()))
				.setPatient(new Reference(myPatient));
		myEvidenceDetail.forEach(detectedIssue::setReference);
	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);
		org.hl7.fhir.dstu2.model.DetectedIssue detectedIssue = (org.hl7.fhir.dstu2.model.DetectedIssue) theResource;

		detectedIssue
				.setIdentifier(new org.hl7.fhir.dstu2.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setPatient(new org.hl7.fhir.dstu2.model.Reference(myPatient));
		myEvidenceDetail.forEach(detectedIssue::setReference);
	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);
		org.hl7.fhir.dstu3.model.DetectedIssue detectedIssue = (org.hl7.fhir.dstu3.model.DetectedIssue) theResource;

		detectedIssue
				.setIdentifier(new org.hl7.fhir.dstu3.model.Identifier().setSystem(myIdentifier.getKey())
						.setValue(myIdentifier.getValue()))
				.setPatient(new org.hl7.fhir.dstu3.model.Reference(myPatient))
				.setStatus(DetectedIssueStatus.valueOf(myStatus));
		myEvidenceDetail.forEach(detectedIssue::setReference);
	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);
		org.hl7.fhir.r4.model.DetectedIssue detectedIssue = (org.hl7.fhir.r4.model.DetectedIssue) theResource;

		List<org.hl7.fhir.r4.model.Identifier> identifier = new ArrayList<>();
		identifier.add(new org.hl7.fhir.r4.model.Identifier().setSystem(myIdentifier.getKey()));

		detectedIssue
				.setIdentifier(identifier)
				.setPatient(new org.hl7.fhir.r4.model.Reference(myPatient))
				.setStatus(org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueStatus.valueOf(myStatus))
				.setCode(new CodeableConcept()
						.addCoding(new Coding()
								.setSystem(getCodeSetting().getSystem())
								.setCode(getCodeSetting().getCode())
								.setDisplay(getCodeSetting().getDisplay())));
		myEvidenceDetail.forEach(evidence -> detectedIssue.addEvidence(
				new DetectedIssueEvidenceComponent().addDetail(new org.hl7.fhir.r4.model.Reference(evidence))));
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);
		org.hl7.fhir.r5.model.DetectedIssue detectedIssue = (org.hl7.fhir.r5.model.DetectedIssue) theResource;

		List<org.hl7.fhir.r5.model.Identifier> identifier = new ArrayList<>();
		identifier.add(new org.hl7.fhir.r5.model.Identifier().setSystem(myIdentifier.getKey()));

		detectedIssue
				.setIdentifier(identifier)
				.setPatient(new org.hl7.fhir.r5.model.Reference(myPatient))
				.setStatus(ObservationStatus.valueOf(myStatus))
				.setCode(new org.hl7.fhir.r5.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r5.model.Coding()
								.setSystem(getCodeSetting().getSystem())
								.setCode(getCodeSetting().getCode())
								.setDisplay(getCodeSetting().getDisplay())));
		myEvidenceDetail.forEach(evidence -> detectedIssue.addEvidence(
				new org.hl7.fhir.r5.model.DetectedIssue.DetectedIssueEvidenceComponent()
						.addDetail(new org.hl7.fhir.r5.model.Reference(evidence))));
	}
}
