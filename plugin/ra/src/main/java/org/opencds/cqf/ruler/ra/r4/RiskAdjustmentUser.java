package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Ids;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface RiskAdjustmentUser extends MeasureReportUser {

	default List<MeasureReport> getMeasureReports(
		String subject, String periodStart, String periodEnd) {
		return search(MeasureReport.class,
			SearchParameterMap.newSynchronous()
				.add(MeasureReport.SP_SUBJECT, new ReferenceParam(subject))
				.add(MeasureReport.SP_PERIOD, new DateRangeParam(periodStart, periodEnd)))
			.getAllResourcesTyped();
	}

	default List<MeasureReport> getMeasureReportsWithMeasureReference(
		String subject, String periodStart, String periodEnd, List<String> measureReference) {
		return getMeasureReports(subject, periodStart, periodEnd).stream().filter(
			report -> {
				if (report.hasMeasure()) {
					for (String ref : measureReference) {
						if (report.getMeasure().endsWith(ref))
							return true;
					}
				}
				return false;
			}).collect(Collectors.toList());
	}

	default List<Bundle> getMostRecentCodingGapReportBundles(String subject) {
		// Something like the following should work, but the composition search parameter doesn't appear to be implemented
//		return search(Bundle.class, SearchParameterMap.newSynchronous()
//			.add(Bundle.SP_TYPE, new TokenParam("document"))
//			.add(Bundle.SP_COMPOSITION, new ReferenceParam("Composition", Composition.SP_SUBJECT, subject))
//			.setSort(new SortSpec(Bundle.SP_TIMESTAMP, SortOrderEnum.DESC))).getAllResourcesTyped();
		return search(Bundle.class, SearchParameterMap.newSynchronous()
			.add(Bundle.SP_TYPE, new TokenParam("document"))
			.add("_profile", new UriParam(RAConstants.CODING_GAP_BUNDLE_URL))
			.setSort(new SortSpec(Bundle.SP_TIMESTAMP, SortOrderEnum.DESC)))
			.getAllResourcesTyped();
	}

	default Bundle getMostRecentCodingGapReportBundle(String subject) {
		return getMostRecentCodingGapReportBundles(subject).stream().filter(
				bundle -> bundle.hasEntry() && bundle.getEntryFirstRep().hasResource()
					&& bundle.getEntryFirstRep().getResource() instanceof Composition
					&& ((Composition) bundle.getEntryFirstRep().getResource()).getSubject().getReference().endsWith(subject))
			.collect(Collectors.toList()).stream().findFirst().orElse(null);
	}

	default Bundle getMostRecentCodingGapReportBundle(String subject, Date periodStart, Date periodEnd) {
		return getMostRecentCodingGapReportBundles(subject).stream().filter(
				bundle -> bundle.hasEntry() && bundle.getEntryFirstRep().hasResource()
					&& bundle.getEntryFirstRep().getResource() instanceof Composition
					&& ((Composition) bundle.getEntryFirstRep().getResource()).getSubject().getReference().endsWith(subject)
					&& bundle.getEntry().stream().anyMatch(
						entry -> entry.hasResource() && entry.getResource() instanceof MeasureReport
							&& ((MeasureReport) entry.getResource()).hasDate()
							&& ((MeasureReport) entry.getResource()).getDate().compareTo(periodStart) >= 0
							&& ((MeasureReport) entry.getResource()).getDate().compareTo(periodEnd) <= 0))
			.collect(Collectors.toList()).stream().findFirst().orElse(null);
	}

	default List<MeasureReport> getReportsFromBundles(List<Bundle> bundles) {
		List<MeasureReport> reports = new ArrayList<>();
		for (Bundle bundle : bundles) {
			reports.addAll(BundleUtil.toListOfResourcesOfType(getFhirContext(), bundle, MeasureReport.class));
		}
		return reports;
	}

	default Composition getCompositionFromBundle(Bundle bundle) {
		return BundleUtil.toListOfResourcesOfType(
			getFhirContext(), bundle, Composition.class).stream().findFirst().orElse(null);
	}

	default MeasureReport getReportFromBundle(Bundle bundle) {
		return BundleUtil.toListOfResourcesOfType(
			getFhirContext(), bundle, MeasureReport.class).stream().findFirst().orElse(null);
	}

	default List<DetectedIssue> getIssuesFromBundle(Bundle bundle) {
		return BundleUtil.toListOfResourcesOfType(getFhirContext(), bundle, DetectedIssue.class);
	}

	default List<DetectedIssue> getOriginalIssues(String measureReportReference) {
		return search(DetectedIssue.class, SearchParameterMap.newSynchronous()
			.add(DetectedIssue.SP_IMPLICATED, new ReferenceParam(measureReportReference))
			.add("_profile", new UriParam(RAConstants.ORIGINAL_ISSUE_PROFILE_URL)))
			.getAllResourcesTyped();
	}

	default List<DetectedIssue> getAssociatedIssues(String measureReportReference) {
		return search(DetectedIssue.class, SearchParameterMap.newSynchronous()
			.add(DetectedIssue.SP_IMPLICATED, new ReferenceParam(measureReportReference))
			.add("_profile", new UriParam(RAConstants.CLINICAL_EVALUATION_ISSUE_PROFILE_URL)))
			.getAllResourcesTyped();
	}

	default List<DetectedIssue> getAllIssues(String measureReportReference) {
		List<DetectedIssue> allIssues = new ArrayList<>();
		allIssues.addAll(getOriginalIssues(measureReportReference));
		allIssues.addAll(getAssociatedIssues(measureReportReference));
		return allIssues;
	}

	default List<Reference> getEvidenceById(String groupId, MeasureReport report) {
		List<Reference> evidence = new ArrayList<>();
		if (report.hasEvaluatedResource()) {
			report.getEvaluatedResource().forEach(
				resource -> {
					if (resource.hasExtension()) {
						resource.getExtensionsByUrl(RAConstants.GROUP_REFERENCE_URL).forEach(
							extension -> {
								if (extension.getValue().primitiveValue().equals(groupId)) {
									evidence.add(resource);
								}
							}
						);
					}
				}
			);
		}
		return evidence;
	}

	default DetectedIssue buildOriginalIssueStart(MeasureReport report, String groupId) {
		DetectedIssue originalIssue = new DetectedIssue();
		originalIssue.setIdElement(new IdType(
			"DetectedIssue", report.getIdElement().getIdPart() + "-" + groupId));
		originalIssue.setMeta(new Meta().addProfile(
			RAConstants.ORIGINAL_ISSUE_PROFILE_URL).setLastUpdated(new Date()));
		originalIssue.addExtension().setUrl(RAConstants.GROUP_REFERENCE_URL).setValue(new StringType(groupId));
		originalIssue.setStatus(DetectedIssue.DetectedIssueStatus.PRELIMINARY);
		originalIssue.setCode(RAConstants.CODING_GAP_CODE);
		if (report.getSubject().getReference().startsWith("Patient/")) {
			originalIssue.setPatient(report.getSubject());
		}
		originalIssue.addImplicated(new Reference(report.getIdElement()));

		return originalIssue;
	}

	default List<DetectedIssue> buildOriginalIssues(MeasureReport report) {
		List<DetectedIssue> issues = new ArrayList<>();
		if (report.hasGroup()) {
			report.getGroup().forEach(
				group -> issues.add(buildOriginalIssueStart(report, group.getId()).setEvidence(
					getEvidenceById(group.getId(), report).stream().map(
						ref -> new DetectedIssue.DetectedIssueEvidenceComponent().addDetail(ref))
						.collect(Collectors.toList())
				))
			);
		}
		return issues;
	}

	default List<Bundle> buildCompositionsAndBundles(String subject, Map<MeasureReport, List<DetectedIssue>> issues) {
		List<Bundle> raBundles = new ArrayList<>();
		for (Map.Entry<MeasureReport, List<DetectedIssue>> issuesSet : issues.entrySet()) {
			raBundles.add(buildCodingGapReportBundle(buildComposition(
				subject, issuesSet.getKey(), issuesSet.getValue()), issuesSet.getValue(), issuesSet.getKey()));
		}

		return raBundles;
	}

	default void updateComposition(Composition composition, MeasureReport report, List<DetectedIssue> issues) {
		composition.setMeta(RAConstants.COMPOSITION_META);
		composition.setSection(new ArrayList<>());
		resolveIssues(composition, report, issues);
	}

	default void resolveIssues(Composition composition, MeasureReport report, List<DetectedIssue> issues) {
		issues.forEach(
			issue -> {
				Composition.SectionComponent section = new Composition.SectionComponent();
				if (issue.hasExtension()) {
					issue.getExtension().forEach(
						extension -> {
							if (extension.hasUrl() && extension.getUrl().equals(RAConstants.GROUP_REFERENCE_URL)) {
								report.getGroup().forEach(
									group -> {
										if (group.hasId() && group.getId().equals(extension.getValue().toString())) {
											section.addEntry(new Reference(issue.getIdElement()));
											if (group.hasCode()) {
												section.setCode(group.getCode());
											}
										}
									}
								);
								section.setFocus(new Reference(report.getIdElement().getValue()));
							}
						}
					);
					composition.addSection(section);
				}
			}
		);
	}

	default Composition buildComposition(String subject, MeasureReport report, List<DetectedIssue> issues) {
		Composition composition = new Composition();
		composition.setMeta(RAConstants.COMPOSITION_META);
		composition.setIdentifier(
			new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID()));
		composition.setStatus(Composition.CompositionStatus.PRELIMINARY)
			.setType(RAConstants.COMPOSITION_TYPE).setSubject(new Reference(subject))
			.setDate(Date.from(Instant.now()))
			.setAuthor(Collections.singletonList(report.getReporter()));
		resolveIssues(composition, report, issues);
		return composition;
	}

	default Bundle startCodingGapReportBundle() {
		Bundle codingGapReportBundle = new Bundle();
		codingGapReportBundle.setMeta(new Meta().setProfile(
			Collections.singletonList(new CanonicalType(RAConstants.CODING_GAP_BUNDLE_URL)))
			.setLastUpdated(new Date()));
		codingGapReportBundle.setIdentifier(
			new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID()));
		codingGapReportBundle.setType(Bundle.BundleType.DOCUMENT);
		codingGapReportBundle.setTimestamp(new Date());
		return codingGapReportBundle;
	}

	default Bundle buildCodingGapReportBundle(Composition composition, List<DetectedIssue> issues, MeasureReport report) {
		Bundle codingGapReportBundle = startCodingGapReportBundle();
		codingGapReportBundle.addEntry().setResource(composition);
		Map<String, Resource> evaluatedResources = new HashMap<>();
		for (DetectedIssue issue : issues) {
			codingGapReportBundle.addEntry().setResource(issue);
			evaluatedResources.putAll(getEvidenceResources(issue));
		}
		codingGapReportBundle.addEntry().setResource(report);
		getEvaluatedResources(report, evaluatedResources);

		for (Map.Entry<String, Resource> evaluatedResourcesSet : evaluatedResources.entrySet()) {
			codingGapReportBundle.addEntry().setResource(evaluatedResourcesSet.getValue());
		}

		return codingGapReportBundle;
	}

	default Bundle buildMissingMeasureReportCodingGapReportBundle(Patient patient) {
		Bundle codingGapReportBundle = startCodingGapReportBundle();
		codingGapReportBundle.addEntry().setResource(patient);
		return codingGapReportBundle;
	}

	default Map<String, Resource> getEvidenceResources(DetectedIssue issue) {
		Map<String, Resource> evidenceResources = new HashMap<>();
		for (DetectedIssue.DetectedIssueEvidenceComponent evidence : issue.getEvidence()) {
			if (evidence.hasDetail()) {
				for (Reference detail : evidence.getDetail()) {
					if (detail.getReference().startsWith("MeasureReport/")) continue;
					evidenceResources.put(Ids.simple(new IdType(detail.getReference())), read(new IdType(detail.getReference())));
				}
			}
		}

		return evidenceResources;
	}

	default List<String> getMeasureReferences(
		List<String> measureId, List<String> measureIdentifier, List<String> measureUrl) {
		if (measureUrl != null) {
			return measureUrl;
		}
		else if (measureId != null) {
			return measureId;
		}
		return measureIdentifier;
	}
}
