package org.opencds.cqf.ruler.casereporting.r4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.ruler.common.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.common.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class MeasureDataProcessProvider extends DaoRegistryOperationProvider {

	private static final Logger logger = LoggerFactory.getLogger(MeasureDataProcessProvider.class);

	@Autowired
	private MeasureResourceProvider measureResourceProvider;

	@Operation(name = "$extract-line-list-data", idempotent = true, type = MeasureReport.class)
	public Bundle extractLineListData(RequestDetails details,
												 @OperationParam(name = "measureReport", min = 0, max = 1, type = MeasureReport.class) MeasureReport measureReport,
												 @OperationParam(name = "subjectList") List<String> subjectList) {
		IVersionSpecificBundleFactory bundleFactory = measureResourceProvider.getContext().newBundleFactory();

		Map<String, Reference> populationSubjectListReferenceMap = new HashMap<String, Reference>();
		gatherSubjectList(measureReport, subjectList, populationSubjectListReferenceMap);
		gatherEicrs(bundleFactory, populationSubjectListReferenceMap);
		Bundle bundle = (Bundle) bundleFactory.getResourceBundle();

		if (bundle != null) {
			if (bundle.getEntry().size() == 1 && bundle.getEntry().get(0).getResource() instanceof Bundle) {
				return (Bundle) bundle.getEntry().get(0).getResource();
			}
		}
		return bundle;
	}

	private void gatherEicrs(IVersionSpecificBundleFactory bundleFactory, Map<String, Reference> populationSubjectListReferenceMap) {
		Map<String, Bundle> eicrs = new HashMap<String, Bundle>();
		for (Map.Entry<String, Reference> entry : populationSubjectListReferenceMap.entrySet()) {
			List<Bundle> bundles = search(Bundle.class, Searches.all()).getAllResourcesTyped();
			for (Bundle bundle : bundles) {
					if (bundle.hasEntry() && !bundle.getEntry().isEmpty() && bundle.hasType() && bundle.getType().equals(Bundle.BundleType.DOCUMENT)) {
						IBaseResource firstEntry = bundle.getEntry().get(0).getResource();
						if (!(firstEntry instanceof Composition)) {
							logger.debug("Any bundle of type document must have the first entry of type Composition, but found: {}", firstEntry.fhirType());
						} else {
							Composition composition = (Composition) firstEntry;
							String[] referenceSplit = composition.getSubject().getReference().split("/");
							if (composition.getSubject().equals(entry.getValue()) || composition.getSubject().getReference().equals(entry.getKey())) {
								eicrs.putIfAbsent(entry.getKey(), bundle);
							} else if (referenceSplit.length > 1 && referenceSplit[1].equals(entry.getKey())) {
								eicrs.putIfAbsent(entry.getKey(), bundle);
							}
						}
					}
			}
		}

		bundleFactory.addResourcesToBundle(eicrs.values().stream().collect(Collectors.toList()), BundleTypeEnum.COLLECTION, null, null, null);
	}

	private void gatherSubjectList(MeasureReport report, List<String> subjectList, Map<String, Reference> populationSubjectListReferenceMap) {
		if (subjectList != null && !subjectList.isEmpty()) {
			for (String subject : subjectList) {
				populationSubjectListReferenceMap.putIfAbsent(subject, new Reference(subject));
			}
		} else if (report != null) {
			gatherPatientsFromReport(report, populationSubjectListReferenceMap);
		} else {
			throw new RuntimeException("Must have either a measureReport or a subjectList or both.");
		}
	}

	private void gatherPatientsFromReport(MeasureReport report, Map<String, Reference> populationSubjectListReferenceMap) {
		if (report.getSubject() != null) {
			populationSubjectListReferenceMap.putIfAbsent(report.getSubject().getReference(), report.getSubject());
		}
		for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
			for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
				for (Reference subject : getSubjectResultsFromList(population.getSubjectResults())) {
					if (subject.fhirType().equals("Patient")) {
						populationSubjectListReferenceMap.putIfAbsent(subject.getReference(), subject);
					}
					if (subject.fhirType().equals("Group")) {
						getPatientListFromGroup(subject.getReference()).forEach(patient -> {
							populationSubjectListReferenceMap.putIfAbsent(patient.getReference(), patient);
						});
					}
				}
			}
		}
	}

	private List<Reference> getPatientListFromGroup(String subjectGroupRef) {
		List<Reference> patientList = new ArrayList<>();
		Group group = read(new IdType(subjectGroupRef));
		group.getMember().forEach(member -> {
			Reference reference = member.getEntity();
			if (reference.getReferenceElement().getResourceType().equals("Patient")) {
				patientList.add(reference);
			} else if (reference.getReferenceElement().getResourceType().equals("Group")) {
				patientList.addAll(getPatientListFromGroup(reference.getReference()));
			} else {
				logger.info(String.format("Group member was not a Patient or a Group, so skipping. \n%s", reference.getReference()));
			}
		});
		return patientList;
	}

	private List<Reference> getSubjectResultsFromList(Reference subjectResults) {
		List<Reference> results = new ArrayList<Reference>();
		if (subjectResults.getReference() == null) {
			logger.debug("No subject results found.");
			return results;
		}
		IBaseResource baseList = read(subjectResults.getReferenceElement());
		if (baseList == null) {
			logger.debug("No subject results found for: {}", subjectResults.getReference());
			return results;
		}
		if (!(baseList instanceof ListResource)) {
			throw new RuntimeException(String.format("Population subject reference was not a List, found: %s", baseList.fhirType()));
		}
		ListResource list = (ListResource) baseList;
		list.getEntry().forEach(entry -> {
			if (entry.getItemTarget().fhirType().equals("Patient") || entry.getItemTarget().fhirType().equals("Group")) {
				results.add(entry.getItem());
			}
		});
		return results;
	}
}
