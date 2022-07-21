package org.opencds.cqf.ruler.cr.r4.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.ConfigurationUser;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.builder.BundleBuilder;
import org.opencds.cqf.ruler.builder.CodeableConceptSettings;
import org.opencds.cqf.ruler.builder.CompositionBuilder;
import org.opencds.cqf.ruler.builder.CompositionSectionComponentBuilder;
import org.opencds.cqf.ruler.builder.DetectedIssueBuilder;
import org.opencds.cqf.ruler.builder.NarrativeSettings;
import org.opencds.cqf.ruler.cr.CrProperties;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.Operations;
import org.opencds.cqf.ruler.utility.Resources;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CareGapsProvider extends DaoRegistryOperationProvider
		implements ParameterUser, ConfigurationUser, ResourceCreator, MeasureReportUser {
	private static final Logger ourLog = LoggerFactory.getLogger(CareGapsProvider.class);
	@Autowired
	private MeasureEvaluateProvider measureEvaluateProvider;
	@Autowired
	private CrProperties crProperties;
	@Autowired
	private Executor cqlExecutor;
	private Map<String, Resource> configuredResources = new HashMap<>();
	private static final Pattern CARE_GAPS_STATUS = Pattern.compile("(open-gap|closed-gap|not-applicable)");
	private static final String CARE_GAPS_REPORT_PROFILE = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm";
	private static final String CARE_GAPS_BUNDLE_PROFILE = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-bundle-deqm";
	private static final String CARE_GAPS_COMPOSITION_PROFILE = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-composition-deqm";
	private static final String CARE_GAPS_DETECTEDISSUE_PROFILE = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-detectedissue-deqm";
	private static final String CARE_GAPS_GAP_STATUS_EXTENSION = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-gapStatus";
	private static final String CARE_GAPS_GAP_STATUS_SYSTEM = "http://hl7.org/fhir/us/davinci-deqm/CodeSystem/gaps-status";
	public static final String CARE_GAPS_MEASUREREPORT_REPORTER_EXTENSION = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-reporterGroup";
	private static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES;
	static {
		CARE_GAPS_CODES = new HashMap<>();
		CARE_GAPS_CODES.put("http://loinc.org/96315-7",
				new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in care report"));
		CARE_GAPS_CODES.put("http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP", new CodeableConceptSettings()
				.add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care Gaps"));
	}

	// TODO: I guess this isn't available yet? Replace when we update to newer
	// version of Java.
	// = ofEntries(
	// new AbstractMap.SimpleEntry<String,
	// CodeableConceptSettings>("http://loinc.org/96315-7",
	// new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in
	// care report")),
	// new AbstractMap.SimpleEntry<String, CodeableConceptSettings>(
	// "http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP", new
	// CodeableConceptSettings()
	// .add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care
	// Gaps")));

	public enum CareGapsStatusCode {
		OPEN_GAP("open-gap"), CLOSED_GAP("closed-gap"), NOT_APPLICABLE("not-applicable");

		private final String myValue;

		CareGapsStatusCode(final String theValue) {
			myValue = theValue;
		}

		@Override
		public String toString() {
			return myValue;
		}

		public String toDisplayString() {
			if (myValue.equals("open-gap")) {
				return "Open Gap";
			}

			if (myValue.equals("closed-gap")) {
				return "Closed Gap";
			}

			if (myValue.equals("not-applicable")) {
				return "Not Applicable";
			}

			throw new IllegalArgumentException();
		}
	}

	/**
	 * Implements the <a href=
	 * "http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html">$care-gaps</a>
	 * operation found in the
	 * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
	 * FHIR Implementation Guide</a> that overrides the <a href=
	 * "http://build.fhir.org/operation-measure-care-gaps.html">$care-gaps</a>
	 * operation found in the
	 * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
	 * Reasoning Module</a>.
	 * 
	 * The operation calculates measures describing gaps in care. For more details,
	 * reference the <a href=
	 * "http://build.fhir.org/ig/HL7/davinci-deqm/gaps-in-care-reporting.html">Gaps
	 * in Care Reporting</a> section of the
	 * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
	 * FHIR Implementation Guide</a>.
	 * 
	 * A Parameters resource that includes zero to many document bundles that
	 * include Care Gap Measure Reports will be returned.
	 * 
	 * Usage:
	 * URL: [base]/Measure/$care-gaps
	 * 
	 * @param theRequestDetails generally auto-populated by the HAPI server
	 *                          framework.
	 * @param periodStart       the start of the gaps through period
	 * @param periodEnd         the end of the gaps through period
	 * @param topic             the category of the measures that is of interest for
	 *                          the care gaps report
	 * @param subject           a reference to either a Patient or Group for which
	 *                          the gaps in care report(s) will be generated
	 * @param practitioner      a reference to a Practitioner for which the gaps in
	 *                          care report(s) will be generated
	 * @param organization      a reference to an Organization for which the gaps in
	 *                          care report(s) will be generated
	 * @param status            the status code of gaps in care reports that will be
	 *                          included in the result
	 * @param measureId         the id of Measure(s) for which the gaps in care
	 *                          report(s) will be calculated
	 * @param measureIdentifier the identifier of Measure(s) for which the gaps in
	 *                          care report(s) will be calculated
	 * @param measureUrl        the canonical URL of Measure(s) for which the gaps
	 *                          in care report(s) will be calculated
	 * @param program           the program that a provider (either clinician or
	 *                          clinical organization) participates in
	 * @return Parameters of bundles of Care Gap Measure Reports
	 */
	@SuppressWarnings("squid:S00107") // warning for greater than 7 parameters
	@Description(shortDefinition = "$care-gaps", value = "Implements the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/index.html\">Da Vinci DEQM FHIR Implementation Guide</a> which is an extension of the <a href=\"http://build.fhir.org/operation-measure-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$care-gaps", idempotent = true, type = Measure.class)
	public Parameters careGapsReport(RequestDetails theRequestDetails,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd,
			@OperationParam(name = "topic") List<String> topic,
			@OperationParam(name = "subject") String subject,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "organization") String organization,
			@OperationParam(name = "status") List<String> status,
			@OperationParam(name = "measureId") List<String> measureId,
			@OperationParam(name = "measureIdentifier") List<String> measureIdentifier,
			@OperationParam(name = "measureUrl") List<CanonicalType> measureUrl,
			@OperationParam(name = "program") List<String> program) {

		validateConfiguration(theRequestDetails);
		validateParameters(theRequestDetails);

		// TODO: filter by topic.
		// TODO: filter by program.
		List<Measure> measures = ensureMeasures(getMeasures(measureId, measureIdentifier, measureUrl, theRequestDetails));

		List<Patient> patients;
		if (!Strings.isNullOrEmpty(subject)) {
			patients = getPatientListFromSubject(subject);
		} else {
			// TODO: implement non subject parameters (practitioner and organization)
			throw new NotImplementedException("Non subject parameters have not been implemented.");
		}

		ensureSupplementalDataElementSearchParameter(theRequestDetails);
		List<CompletableFuture<Parameters.ParametersParameterComponent>> futures = new ArrayList<>();
		Parameters result = initializeResult();
		if (crProperties.getThreadedCareGapsEnabled()) {
			(patients)
					.forEach(
							patient -> {
								futures.add(CompletableFuture.supplyAsync(() -> patientReports(theRequestDetails,
										periodStart, periodEnd, patient, status, measures, organization), cqlExecutor));
							});

			futures.forEach(x -> result.addParameter(x.join()));
		} else {
			(patients).forEach(
					patient -> {
						Parameters.ParametersParameterComponent patientParameter = patientReports(theRequestDetails,
								periodStart, periodEnd, patient, status, measures, organization);
						if (patientParameter != null) {
							result.addParameter(patientParameter);
						}
					});
		}
		return result;
	}

	private <T extends Resource> T putConfiguredResource(Class<T> theResourceClass, String theId, String theKey,
			RequestDetails theRequestDetails) {
		T resource = search(theResourceClass, Searches.byId(theId), theRequestDetails).firstOrNull();
		if (resource != null) {
			configuredResources.put(theKey, resource);
		}
		return resource;
	}

	@Override
	public void validateConfiguration(RequestDetails theRequestDetails) {
		checkNotNull(crProperties.getMeasureReport(),
				"The measure_report setting is required for the $care-gaps operation.");
		checkArgument(!Strings.isNullOrEmpty(crProperties.getMeasureReport().getReporter()),
				"The measure_report.care_gaps_reporter setting is required for the $care-gaps operation.");
		checkArgument(!Strings.isNullOrEmpty(crProperties.getMeasureReport().getCompositionAuthor()),
				"The measure_report.care_gaps_composition_section_author setting is required for the $care-gaps operation.");

		Resource configuredReporter = putConfiguredResource(Organization.class,
				crProperties.getMeasureReport().getReporter(), "care_gaps_reporter", theRequestDetails);
		Resource configuredAuthor = putConfiguredResource(Organization.class,
				crProperties.getMeasureReport().getCompositionAuthor(), "care_gaps_composition_section_author",
				theRequestDetails);

		checkNotNull(configuredReporter, String.format(
				"The %s Resource is configured as the measure_report.care_gaps_reporter but the Resource could not be read.",
				crProperties.getMeasureReport().getReporter()));
		checkNotNull(configuredAuthor, String.format(
				"The %s Resource is configured as the measure_report.care_gaps_composition_section_author but the Resource could not be read.",
				crProperties.getMeasureReport().getCompositionAuthor()));
	}

	@SuppressWarnings("squid:S1192") // warning for using the same string value more than 5 times
	public void validateParameters(RequestDetails theRequestDetails) {
		Operations.validatePeriod(theRequestDetails, "periodStart", "periodEnd");
		Operations.validateCardinality(theRequestDetails, "subject", 0, 1);
		Operations.validateSingularPattern(theRequestDetails, "subject", Operations.PATIENT_OR_GROUP_REFERENCE);
		Operations.validateCardinality(theRequestDetails, "status", 1);
		Operations.validateSingularPattern(theRequestDetails, "status", CARE_GAPS_STATUS);
		Operations.validateExclusive(theRequestDetails, "subject", "organization", "practitioner");
		Operations.validateExclusive(theRequestDetails, "organization", "subject");
		Operations.validateInclusive(theRequestDetails, "practitioner", "organization");
		Operations.validateExclusiveOr(theRequestDetails, "subject", "organization");
		Operations.validateAtLeastOne(theRequestDetails, "measureId", "measureIdentifier", "measureUrl");
	}

	private List<Measure> ensureMeasures(List<Measure> measures) {
		measures.forEach(measure -> {
			if (!measure.hasScoring()) {
				ourLog.info("Measure does not specify a scoring so skipping: {}.", measure.getId());
				measures.remove(measure);
			}
			if (!measure.hasImprovementNotation()) {
				ourLog.info("Measure does not specify an improvement notation so skipping: {}.", measure.getId());
				measures.remove(measure);
			}
		});
		return measures;
	}

	private Parameters initializeResult() {
		return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID().toString());
	}

	@SuppressWarnings("squid:S00107") // warning for greater than 7 parameters
	private Parameters.ParametersParameterComponent patientReports(RequestDetails requestDetails, String periodStart,
			String periodEnd, Patient patient, List<String> status, List<Measure> measures, String organization) {
		// TODO: add organization to report, if it exists.
		Composition composition = getComposition(patient);
		List<DetectedIssue> detectedIssues = new ArrayList<>();
		Map<String, Resource> evalPlusSDE = new HashMap<>();
		List<MeasureReport> reports = getReports(requestDetails, periodStart, periodEnd, patient, status, measures,
				composition, detectedIssues, evalPlusSDE);

		if (reports.isEmpty()) {
			return null;
		}

		return initializePatientParameter(patient).setResource(
				addBundleEntries(requestDetails.getFhirServerBase(), composition, detectedIssues, reports, evalPlusSDE));
	}

	@SuppressWarnings("squid:S00107") // warning for greater than 7 parameters
	private List<MeasureReport> getReports(RequestDetails requestDetails, String periodStart, String periodEnd,
			Patient patient, List<String> status, List<Measure> measures, Composition composition,
			List<DetectedIssue> detectedIssues, Map<String, Resource> evalPlusSDE) {
		List<MeasureReport> reports = new ArrayList<>();
		MeasureReport report;
		for (Measure measure : measures) {
			report = measureEvaluateProvider.evaluateMeasure(requestDetails, measure.getIdElement(), periodStart,
					periodEnd, "patient", Ids.simple(patient), null, null, null, null, null);
			if (!report.hasGroup()) {
				ourLog.info("Report does not include a group so skipping.\nSubject: {}\nMeasure: {}",
						Ids.simple(patient),
						Ids.simplePart(measure));
				continue;
			}

			initializeReport(report);

			CareGapsStatusCode gapStatus = getGapStatus(measure, report);
			if (!status.contains(gapStatus.toString())) {
				continue;
			}

			DetectedIssue detectedIssue = getDetectedIssue(patient, report, gapStatus);
			detectedIssues.add(detectedIssue);
			composition.addSection(getSection(measure, report, detectedIssue, gapStatus));
			getEvaluatedResources(report, evalPlusSDE).getSDE(report, evalPlusSDE);
			reports.add(report);
		}

		return reports;
	}

	private void initializeReport(MeasureReport report) {
		if (Strings.isNullOrEmpty(report.getId())) {
			IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
			report.setId(id);
		}
		Reference reporter = new Reference().setReference(crProperties.getMeasureReport().getReporter());
		// TODO: figure out what this extension is for
		// reporter.addExtension(new
		// Extension().setUrl(CARE_GAPS_MEASUREREPORT_REPORTER_EXTENSION));
		report.setReporter(reporter);
		if (report.hasMeta()) {
			report.getMeta().addProfile(CARE_GAPS_REPORT_PROFILE);
		} else {
			report.setMeta(new Meta().addProfile(CARE_GAPS_REPORT_PROFILE));
		}
	}

	private Parameters.ParametersParameterComponent initializePatientParameter(Patient patient) {
		Parameters.ParametersParameterComponent patientParameter = Resources
				.newBackboneElement(Parameters.ParametersParameterComponent.class)
				.setName("return");
		patientParameter.setId("subject-" + Ids.simplePart(patient));
		return patientParameter;
	}

	private Bundle addBundleEntries(String serverBase, Composition composition, List<DetectedIssue> detectedIssues,
			List<MeasureReport> reports, Map<String, Resource> evalPlusSDE) {
		Bundle reportBundle = getBundle();
		reportBundle.addEntry(getBundleEntry(serverBase, composition));
		reports.forEach(report -> reportBundle.addEntry(getBundleEntry(serverBase, report)));
		detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
		configuredResources.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
		evalPlusSDE.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
		return reportBundle;
	}

	private CareGapsStatusCode getGapStatus(Measure measure, MeasureReport report) {
		Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
		report.getGroup().forEach(group -> group.getPopulation().forEach(population -> {
			if (population.hasCode()
					&& population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumerator.getKey())
					&& population.getCount() == 1) {
				inNumerator.setValue(true);
			}
		}));

		boolean isPositive = measure.getImprovementNotation().hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
				"increase");

		if ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue())) {
			return CareGapsStatusCode.OPEN_GAP;
		}

		return CareGapsStatusCode.CLOSED_GAP;
	}

	private BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
		return new BundleEntryComponent().setResource(resource)
				.setFullUrl(Operations.getFullUrl(serverBase, resource));
	}

	private Composition.SectionComponent getSection(Measure measure, MeasureReport report, DetectedIssue detectedIssue,
			CareGapsStatusCode gapStatus) {
		String narrative = String.format(HTML_DIV_PARAGRAPH_CONTENT,
				gapStatus == CareGapsStatusCode.CLOSED_GAP ? "No detected issues."
						: String.format("Issues detected.  See %s for details.", Ids.simple(detectedIssue)));
		return new CompositionSectionComponentBuilder<>(Composition.SectionComponent.class)
				.withTitle(measure.hasTitle() ? measure.getTitle() : measure.getUrl())
				.withFocus(Ids.simple(report))
				.withText(new NarrativeSettings(narrative))
				.withEntry(Ids.simple(detectedIssue))
				.build();
	}

	private Bundle getBundle() {
		return new BundleBuilder<>(Bundle.class)
				.withProfile(CARE_GAPS_BUNDLE_PROFILE)
				.withType(BundleType.DOCUMENT.toString())
				.build();
	}

	private Composition getComposition(Patient patient) {
		return new CompositionBuilder<>(Composition.class)
				.withProfile(CARE_GAPS_COMPOSITION_PROFILE)
				.withType(CARE_GAPS_CODES.get("http://loinc.org/96315-7"))
				.withStatus(Composition.CompositionStatus.FINAL.toString())
				.withTitle("Care Gap Report for " + Ids.simplePart(patient))
				.withSubject(Ids.simple(patient))
				.withAuthor(Ids.simple(configuredResources.get("care_gaps_composition_section_author")))
				// .withCustodian(organization) // TODO: Optional: identifies the organization
				// who is responsible for ongoing maintenance of and accessing to this gaps in
				// care report. Add as a setting and optionally read if it's there.
				.build();
	}

	private DetectedIssue getDetectedIssue(Patient patient, MeasureReport report, CareGapsStatusCode gapStatus) {
		return new DetectedIssueBuilder<>(DetectedIssue.class)
				.withProfile(CARE_GAPS_DETECTEDISSUE_PROFILE)
				.withStatus(DetectedIssue.DetectedIssueStatus.FINAL.toString())
				.withCode(CARE_GAPS_CODES.get("http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP"))
				.withPatient(Ids.simple(patient))
				.withEvidenceDetail(Ids.simple(report))
				.withModifierExtension(new ImmutablePair<>(
						CARE_GAPS_GAP_STATUS_EXTENSION,
						new CodeableConceptSettings().add(CARE_GAPS_GAP_STATUS_SYSTEM, gapStatus.toString(),
								gapStatus.toDisplayString())))
				.build();
	}
}
