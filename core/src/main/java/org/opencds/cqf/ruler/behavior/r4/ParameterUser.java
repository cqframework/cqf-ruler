package org.opencds.cqf.ruler.behavior.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface ParameterUser extends DaoRegistryUser, IdCreator {
	static final Logger ourLog = LoggerFactory.getLogger(ParameterUser.class);

	void validateParameters(RequestDetails theRequestDetails);

	// TODO: replace this with version from the evaluator?
	default List<Patient> getPatientListFromSubject(String subject) {
		if (subject.startsWith("Patient/")) {
			Patient patient = ensurePatient(subject);
			return Collections.singletonList(patient);
		} else if (subject.startsWith("Group/")) {
			return getPatientListFromGroup(subject);
		}

		ourLog.info("Subject member was not a Patient or a Group, so skipping. \n{}", subject);
		return Collections.emptyList();
	}

	// TODO: replace this with version from the evaluator?
	default List<Patient> getPatientListFromGroup(String subjectGroupId) {
		List<Patient> patientList = new ArrayList<>();

		Group group = read(newId(subjectGroupId));
		if (group == null) {
			throw new IllegalArgumentException("Could not find Group: " + subjectGroupId);
		}

		group.getMember().forEach(member -> {
			Reference reference = member.getEntity();
			if (reference.getReferenceElement().getResourceType().equals("Patient")) {
				Patient patient = ensurePatient(reference.getReference());
				patientList.add(patient);
			} else if (reference.getReferenceElement().getResourceType().equals("Group")) {
				patientList.addAll(getPatientListFromGroup(reference.getReference()));
			} else {
				ourLog.info("Group member was not a Patient or a Group, so skipping. \n{}", reference.getReference());
			}
		});

		return patientList;
	}

	// TODO: replace this with version from the evaluator?
	default Patient ensurePatient(String patientRef) {
		Patient patient = read(newId(patientRef));
		if (patient == null) {
			throw new IllegalArgumentException("Could not find Patient: " + patientRef);
		}
		return patient;
	}
}
