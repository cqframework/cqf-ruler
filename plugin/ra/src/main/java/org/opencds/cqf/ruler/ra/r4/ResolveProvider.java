package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.Operations;
import org.opencds.cqf.ruler.utility.TypedBundleProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

public class ResolveProvider extends DaoRegistryOperationProvider implements MeasureReportUser {

    private static final Meta COMPOSITION_META = new Meta().addProfile("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-composition");
    private static final CodeableConcept COMPOSITION_TYPE = new CodeableConcept().addCoding(
            new Coding().setSystem("http://loinc.org").setCode("96315-7").setDisplay("Gaps in care report"));

    @Operation(name = "$davinci-ra.resolve", idempotent = true, type = MeasureReport.class)
    public Parameters resolve(
            RequestDetails requestDetails,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "measureId") List<String> measureId,
            @OperationParam(name = "measureIdentifier") List<String> measureIdentifier,
            @OperationParam(name = "measureUrl") List<String> measureUrl) {

        if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
            try {
                Operations.validateCardinality(requestDetails, "periodStart", 1);
                Operations.validateCardinality(requestDetails, "periodEnd", 1);
                Operations.validateCardinality(requestDetails, "subject", 1);
                Operations.validatePeriod(requestDetails, "periodStart", "periodEnd");
                Operations.validatePattern("subject", subject, Operations.PATIENT_OR_GROUP_REFERENCE);
                Operations.validateAtLeastOne(requestDetails, "measureId", "measureIdentifier", "measureUrl");
            } catch (Exception e) {
                return newParameters(newPart("Invalid parameters",
                        generateIssue("error", e.getMessage())));
            }
        }

        List<MeasureReport> reports = new ArrayList<>();
        if (subject.startsWith("Group/")) {
            Group group = read(new IdType(subject));
            for (Group.GroupMemberComponent groupMembers: group.getMember()) {
                if (groupMembers.hasEntity() && groupMembers.getEntity().getReference().startsWith("Patient/")) {
                    reports.addAll(fetchReports(groupMembers.getEntity().getReference(), periodStart,
                            periodEnd, measureId, measureIdentifier, measureUrl));
                }
            }
        }
        else {
            reports.addAll(fetchReports(subject, periodStart, periodEnd,
                    measureId, measureIdentifier, measureUrl));
        }
        Map<MeasureReport, List<DetectedIssue>> issues = fetchIssues(reports);
        List<Bundle> raBundles = generateCompositionsAndBundles(subject, issues);

        Parameters result = new Parameters();
        for (Bundle raBundle : raBundles) {
            result.addParameter(newPart("return", raBundle));
        }

        return result;
    }

    private List<MeasureReport> fetchReports(String subject, String periodStart, String periodEnd,
                                             List<String> measureId, List<String> measureIdentifier,
                                             List<String> measureUrl) {
        List<MeasureReport> reports = new ArrayList<>();
        TypedBundleProvider<MeasureReport> searchResults = search(MeasureReport.class,
                SearchParameterMap.newSynchronous()
                        .add(MeasureReport.SP_SUBJECT, new ReferenceParam(subject))
                        .add(MeasureReport.SP_PERIOD, new DateRangeParam(periodStart, periodEnd)));

        for (MeasureReport report : searchResults.getAllResourcesTyped()) {
            if (!report.hasMeasure()) continue;
            if (measureId != null && !measureId.isEmpty()) {
                for (String id : measureId) {
                    if (report.getMeasure().endsWith(id)) {
                        reports.add(report);
                    }
                }
            }
            else if (measureIdentifier != null && !measureIdentifier.isEmpty()) {
                for (String identifier : measureIdentifier) {
                    if (report.getMeasure().contains(identifier)) {
                        reports.add(report);
                    }
                }
            }
            else {
                for (String url : measureUrl) {
                    if (report.getMeasure().equals(url)) {
                        reports.add(report);
                    }
                }
            }
        }

        return reports;
    }

    private Map<MeasureReport, List<DetectedIssue>> fetchIssues(List<MeasureReport> reports) {
        Map<MeasureReport, List<DetectedIssue>> issues = new HashMap<>();
        for (MeasureReport report : reports) {
            issues.put(report, search(DetectedIssue.class, SearchParameterMap.newSynchronous()
                            .add(DetectedIssue.SP_IMPLICATED, new ReferenceParam(report.getIdElement().getValue()))
                    ).getAllResourcesTyped());
        }

        return issues;
    }

    private List<Bundle> generateCompositionsAndBundles(String subject, Map<MeasureReport, List<DetectedIssue>> issues) {
        List<Bundle> raBundles = new ArrayList<>();
        for (Map.Entry<MeasureReport, List<DetectedIssue>> issuesSet : issues.entrySet()) {
            Composition composition = new Composition();
            composition.setMeta(COMPOSITION_META);
            composition.setStatus(Composition.CompositionStatus.PRELIMINARY)
                    .setType(COMPOSITION_TYPE).setSubject(new Reference(subject))
                    .setDate(Date.from(Instant.now()));
            composition.addSection().setFocus(new Reference(issuesSet.getKey().getIdElement().getValue()))
                    .setEntry(issuesSet.getValue().stream().map(
                            issue -> new Reference(issue.getIdElement().getValue())).collect(Collectors.toList()));

            raBundles.add(generateRaBundle(composition, issuesSet.getValue(), issuesSet.getKey()));
        }

        return raBundles;
    }

    private Bundle generateRaBundle(Composition composition, List<DetectedIssue> issues, MeasureReport report) {
        Bundle raBundle = new Bundle().setType(Bundle.BundleType.DOCUMENT);
        raBundle.addEntry().setResource(composition);
        Map<String, Resource> evaluatedResources = new HashMap<>();
        for (DetectedIssue issue : issues) {
            raBundle.addEntry().setResource(issue);
            evaluatedResources.putAll(getEvidenceResources(issue));
        }
        raBundle.addEntry().setResource(report);
        getEvaluatedResources(report, evaluatedResources);

        for (Map.Entry<String, Resource> evaluatedResourcesSet : evaluatedResources.entrySet()) {
            raBundle.addEntry().setResource(evaluatedResourcesSet.getValue());
        }

        return raBundle;
    }

    private Map<String, Resource> getEvidenceResources(DetectedIssue issue) {
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
}
