package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.UriParam;
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

	default List<DetectedIssue> getOriginalIssues(String measureReportReference) {
		return search(DetectedIssue.class, SearchParameterMap.newSynchronous()
			.add(DetectedIssue.SP_IMPLICATED, new ReferenceParam(measureReportReference))
			.add("_profile", new UriParam(RAConstants.ORIGINAL_ISSUE_PROFILE_URL)))
			.getAllResourcesTyped();
	}

	default List<DetectedIssue> getAssociatedIssues(String measureReportReference) {
		return search(DetectedIssue.class, SearchParameterMap.newSynchronous()
			.add(DetectedIssue.SP_IMPLICATED, new ReferenceParam(measureReportReference)))
			.getAllResourcesTyped();
	}

	default Map<MeasureReport, List<DetectedIssue>> getIssueMap(List<MeasureReport> reports) {
		Map<MeasureReport, List<DetectedIssue>> issues = new HashMap<>();
		for (MeasureReport report : reports) {
			issues.put(report, getAssociatedIssues(report.getIdElement().getValue()));
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

	default Composition buildComposition(String subject, MeasureReport report, List<DetectedIssue> issues) {
		Composition composition = new Composition();
		composition.setMeta(RAConstants.COMPOSITION_META);
		composition.setIdentifier(
			new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID()));
		composition.setStatus(Composition.CompositionStatus.PRELIMINARY)
			.setType(RAConstants.COMPOSITION_TYPE).setSubject(new Reference(subject))
			.setDate(Date.from(Instant.now()));
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
											section.addEntry(new Reference(issue.getIdElement().getValue()));
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
