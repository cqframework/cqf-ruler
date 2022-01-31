package org.opencds.cqf.ruler.casereporting.r4;

import static ca.uhn.fhir.model.valueset.BundleTypeEnum.COLLECTION;
import static org.hl7.fhir.r4.model.Bundle.BundleType.DOCUMENT;

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
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * This class attempts to collect line list data for given MeasureReport
 */
public class MeasureDataProcessProvider extends DaoRegistryOperationProvider {

	private static final Logger logger = LoggerFactory.getLogger(MeasureDataProcessProvider.class);

	@Autowired
	private MeasureResourceProvider measureResourceProvider;

	@Operation(name = "$extract-line-list-data", idempotent = true, type = MeasureReport.class)
	public Bundle extractLineListData(RequestDetails details,
			@OperationParam(name = "measureReport", min = 0, max = 1, type = MeasureReport.class) MeasureReport measureReport,
			@OperationParam(name = "subjectList") List<String> subjectList) {
		IVersionSpecificBundleFactory bundleFactory = measureResourceProvider.getContext().newBundleFactory();

		Map<String, Reference> populationSubjectListReferenceMap = new HashMap<>();
		gatherSubjectList(measureReport, subjectList, populationSubjectListReferenceMap);
		gatherEicrs(bundleFactory, populationSubjectListReferenceMap);
		Bundle bundle = (Bundle) bundleFactory.getResourceBundle();

		if (bundle != null && bundle.hasEntry() && bundle.getEntryFirstRep().getResource() instanceof Bundle) {
			return (Bundle) bundle.getEntryFirstRep().getResource();
		}

		return bundle;
	}

	private void gatherEicrs(IVersionSpecificBundleFactory bundleFactory,
			Map<String, Reference> populationSubjectListReferenceMap) {
		Map<String, Bundle> eicrs = new HashMap<>();
		List<Bundle> documentBundles = search(Bundle.class, Searches.all())
				.getAllResourcesTyped().stream()
				.filter(x -> x.hasEntry() && DOCUMENT.equals(x.getType())).collect(Collectors.toList());

		for (Bundle bundle : documentBundles) {
			IBaseResource firstResource = bundle.getEntryFirstRep().getResource();
			if (!(firstResource instanceof Composition)) {
				logger.debug("Any bundle of type document must have the first entry of type Composition, but found: {}",
						firstResource);
				continue;
			}

			Composition composition = (Composition) firstResource;
			Reference compositionSubject = composition.getSubject();
			String[] referenceSplit = compositionSubject.getReference().split("/");

			for (Map.Entry<String, Reference> entry : populationSubjectListReferenceMap.entrySet()) {
				if (compositionSubject.equals(entry.getValue())
						|| compositionSubject.getReference().equals(entry.getKey())
						|| (referenceSplit.length > 1 && referenceSplit[1].equals(entry.getKey()))) {
					eicrs.putIfAbsent(entry.getKey(), bundle);
				}
			}
		}

		bundleFactory.addResourcesToBundle(eicrs.values().stream().collect(Collectors.toList()), COLLECTION, null, null,
				null);
	}

	private void gatherSubjectList(MeasureReport report, List<String> subjectList,
			Map<String, Reference> populationSubjectListReferenceMap) {
		if (subjectList == null && report == null) {
			throw new IllegalArgumentException("Must have either a measureReport or a subjectList or both.");
		}
		if (report != null) {
			gatherPatientsFromReport(report, populationSubjectListReferenceMap);
		}
		else {
			for (String subject : subjectList) {
				populationSubjectListReferenceMap.putIfAbsent(subject, new Reference(subject));
			}
		}
	}

	private void gatherPatientsFromReport(MeasureReport report,
			Map<String, Reference> populationSubjectListReferenceMap) {
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
						getPatientListFromGroup(subject.getReference()).forEach(patient ->
							populationSubjectListReferenceMap.putIfAbsent(patient.getReference(), patient)
						);
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
				logger.info("Group member was not a Patient or a Group, so skipping. \n{}",reference.getReference());
			}
		});
		return patientList;
	}

	private List<Reference> getSubjectResultsFromList(Reference subjectResults) {
		List<Reference> results = new ArrayList<>();
		if (subjectResults.getReference() == null) {
			logger.debug("No subject results found.");
			return results;
		}

		IBaseResource baseList = read(subjectResults.getReferenceElement());

		if (!(baseList instanceof ListResource)) {
			throw new IllegalArgumentException(
					String.format("Population subject reference was not a List, found: %s", baseList.fhirType()));
		}

		ListResource list = (ListResource) baseList;
		list.getEntry().forEach(entry -> {
			if (entry.getItemTarget().getResourceType() == ResourceType.Patient || entry.getItemTarget().getResourceType() == ResourceType.Group) {
				results.add(entry.getItem());
			}
		});

		return results;
	}
}
