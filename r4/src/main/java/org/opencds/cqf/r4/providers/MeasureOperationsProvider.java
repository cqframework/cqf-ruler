package org.opencds.cqf.r4.providers;

import java.util.*;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hibernate.cfg.NotYetImplementedException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.helpers.DateHelper;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.r4.evaluation.MeasureEvaluation;
import org.opencds.cqf.r4.evaluation.MeasureEvaluationSeed;
import org.opencds.cqf.r4.helpers.LibraryHelper;
import org.opencds.cqf.tooling.library.r4.NarrativeProvider;
import org.opencds.cqf.tooling.measure.r4.CqfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.r4.provider.JpaTerminologyProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@Component
public class MeasureOperationsProvider {

    private NarrativeProvider narrativeProvider;
    private HQMFProvider hqmfProvider;
    private DataRequirementsProvider dataRequirementsProvider;

    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResolutionProvider;
    private MeasureResourceProvider measureResourceProvider;
    private DaoRegistry registry;
    private String serverAddress = HapiProperties.getServerAddress();

    private static final Logger logger = LoggerFactory.getLogger(MeasureOperationsProvider.class);

    private JpaTerminologyProvider jpaTerminologyProvider;
    private DataProvider dataProvider;
    private LibraryContentProvider libraryContentProvider;
    private FhirDal fhirDal;
    private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> globalLibraryCache;
    
    @Inject
    public MeasureOperationsProvider(DaoRegistry registry, EvaluationProviderFactory factory,
            NarrativeProvider narrativeProvider, HQMFProvider hqmfProvider,
            LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResolutionProvider,
            MeasureResourceProvider measureResourceProvider, DataRequirementsProvider dataRequirementsProvider,
            JpaTerminologyProvider jpaTerminologyProvider,
            DataProvider dataProvider,
            LibraryContentProvider libraryContentProvider,
            FhirDal fhirDal,
            Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> globalLibraryCache) {
        this.registry = registry;
        this.libraryResolutionProvider = libraryResolutionProvider;
        this.narrativeProvider = narrativeProvider;
        this.hqmfProvider = hqmfProvider;
        this.dataRequirementsProvider = dataRequirementsProvider;
        this.measureResourceProvider = measureResourceProvider;

        this.jpaTerminologyProvider = jpaTerminologyProvider;
        this.dataProvider = dataProvider;
        this.libraryContentProvider = libraryContentProvider;
        this.fhirDal = fhirDal;
        this.globalLibraryCache = globalLibraryCache;
    }

    @Operation(name = "$hqmf", idempotent = true, type = Measure.class)
    public Parameters hqmf(@IdParam IdType theId) {
        Measure theResource = this.measureResourceProvider.getDao().read(theId);
        String hqmf = this.generateHQMF(theResource);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(hqmf));
        return p;
    }

    @Operation(name = "$refresh-generated-content", type = Measure.class)
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
            @IdParam IdType theId) {
        Measure theResource = this.measureResourceProvider.getDao().read(theId);

        theResource.getRelatedArtifact().removeIf(
                relatedArtifact -> relatedArtifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON));

        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource,
                this.libraryResolutionProvider);

        // Ensure All Related Artifacts for all referenced Libraries
        if (!cqfMeasure.getRelatedArtifact().isEmpty()) {
            for (RelatedArtifact relatedArtifact : cqfMeasure.getRelatedArtifact()) {
                boolean artifactExists = false;
                // logger.info("Related Artifact: " + relatedArtifact.getUrl());
                for (RelatedArtifact resourceArtifact : theResource.getRelatedArtifact()) {
                    if (resourceArtifact.equalsDeep(relatedArtifact)) {
                        // logger.info("Equals deep true");
                        artifactExists = true;
                        break;
                    }
                }
                if (!artifactExists) {
                    theResource.addRelatedArtifact(relatedArtifact.copy());
                }
            }
        }

        try {
            Narrative n = this.narrativeProvider.getNarrative(this.measureResourceProvider.getContext(), cqfMeasure);
            theResource.setText(n.copy());
        } catch (Exception e) {
            logger.info("Error generating narrative", e);
        }

        return this.measureResourceProvider.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-narrative", idempotent = true, type = Measure.class)
    public Parameters getNarrative(@IdParam IdType theId) {
        Measure theResource = this.measureResourceProvider.getDao().read(theId);
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource,
                this.libraryResolutionProvider);
        Narrative n = this.narrativeProvider.getNarrative(this.measureResourceProvider.getContext(), cqfMeasure);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }

    private String generateHQMF(Measure theResource) {
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource,
                this.libraryResolutionProvider);
        return this.hqmfProvider.generateHQMF(cqfMeasure);
    }

    /*
     *
     * NOTE that the source, user, and pass parameters are not standard parameters
     * for the FHIR $evaluate-measure operation
     *
     */
    @Operation(name = "$evaluate-measure", idempotent = true, type = Measure.class)
    public MeasureReport evaluateMeasure(@IdParam IdType theId,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "measure") String measureRef,
            @OperationParam(name = "reportType") String reportType, @OperationParam(name = "patient") String patientRef,
            @OperationParam(name = "productLine") String productLine,
            @OperationParam(name = "practitioner") String practitionerRef,
            @OperationParam(name = "lastReceivedOn") String lastReceivedOn,
            @OperationParam(name = "source") String source, @OperationParam(name = "user") String user,
            @OperationParam(name = "pass") String pass) throws InternalErrorException, FHIRException {
        org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor measureProcessor = new org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor(null, null, null, null, null, jpaTerminologyProvider, libraryContentProvider, dataProvider, fhirDal, null, this.globalLibraryCache);
        Measure measure = this.registry.getResourceDao(Measure.class).read(theId);
        MeasureReport report = measureProcessor.evaluateMeasure(measure.getUrl(), periodStart, periodEnd, reportType, patientRef, null, lastReceivedOn, null, null, null, null);

        if (productLine != null) {
            Extension ext = new Extension();
            ext.setUrl("http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine");
            ext.setValue(new StringType(productLine));
            report.addExtension(ext);
        }

        return report;
    }

    // @Operation(name = "$evaluate-measure-with-source", idempotent = true)
    // public MeasureReport evaluateMeasure(@IdParam IdType theId,
    // @OperationParam(name = "sourceData", min = 1, max = 1, type = Bundle.class)
    // Bundle sourceData,
    // @OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
    // @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd) {
    // if (periodStart == null || periodEnd == null) {
    // throw new IllegalArgumentException("periodStart and periodEnd are required
    // for measure evaluation");
    // }
    // LibraryLoader libraryLoader =
    // LibraryHelper.createLibraryLoader(this.libraryResourceProvider);
    // MeasureEvaluationSeed seed = new MeasureEvaluationSeed(this.factory,
    // libraryLoader, this.libraryResourceProvider);
    // Measure measure = this.getDao().read(theId);

    // if (measure == null) {
    // throw new RuntimeException("Could not find Measure/" + theId.getIdPart());
    // }

    // seed.setup(measure, periodStart, periodEnd, null, null, null, null);
    // BundleDataProviderStu3 bundleProvider = new
    // BundleDataProviderStu3(sourceData);
    // bundleProvider.setTerminologyProvider(provider.getTerminologyProvider());
    // seed.getContext().registerDataProvider("http://hl7.org/fhir",
    // bundleProvider);
    // MeasureEvaluation evaluator = new MeasureEvaluation(bundleProvider,
    // seed.getMeasurementPeriod());
    // return evaluator.evaluatePatientMeasure(seed.getMeasure(), seed.getContext(),
    // "");
    // }

    @Operation(name = "$care-gaps", idempotent = true, type = Measure.class)
    public Parameters careGapsReport(
        @OperationParam(name = "periodStart") List <String> periodStart,
        @OperationParam(name = "periodEnd") List <String> periodEnd,
        @OperationParam(name = "subject") List <String> subject,
        @OperationParam(name = "topic") String topic,
        @OperationParam(name = "practitioner") String practitioner,
        @OperationParam(name = "measureId") List <String> measureId,
        @OperationParam(name = "measureIdentifier") List <String> measureIdentifier,
        @OperationParam(name = "measureUrl") List <CanonicalType > measureUrl,
        @OperationParam(name = "status") List <String> status,
        @OperationParam(name = "organization") String organization,
        @OperationParam(name = "program") String program) {
        //TODO: topic should allow many and be a union of them
        //TODO: "The Server needs to make sure that practitioner is authorized to get the gaps in care report for and know what measures the practitioner are eligible or qualified."
        Parameters returnParams = new Parameters();
        returnParams.setId((UUID.randomUUID().toString()));

        // Setting periodStart, periodEnd, and subject to lists to check if multiple have been supplied.
        // This is a hack and I hate it. I don't know how to just pull the current url due to
        // the amount of abstraction going on. I didn't want to waste too much time here.
        // If there's a better way of doing this, please ping me. - Carter

        // Carter, this can be done with OperationParam args.  See the $report implementation.  - Rob

        String _periodStart = periodStart.get(0);
        String _periodEnd = periodEnd.get(0);
        String _subject = subject.get(0);

        if (careGapParameterValidation(
                periodStart,
                periodEnd,
                subject,
                topic,
                practitioner,
                measureId,
                measureIdentifier,
                measureUrl,
                status,
                organization,
                program,
                _periodStart,
                _periodEnd,
                _subject)) {

            List < Measure > measures = resolveMeasures(measureId, measureIdentifier, measureUrl);
            if (_subject.startsWith("Patient/")) {
                resolvePatientGapBundleForMeasures(_periodStart, _periodEnd, _subject, topic, status, returnParams, measures, "return", organization);
            } else if (_subject.startsWith("Group/")) {
                returnParams.setId(status + "-" + _subject.replace("/", "-") + "-report");
                (getPatientListFromGroup(_subject))
                .forEach(
                    groupSubject -> resolvePatientGapBundleForMeasures(
                        _periodStart,
                        _periodEnd,
                        groupSubject.getReference(),
                        topic,
                        status,
                        returnParams,
                        measures,
                        "return",
                        organization
                    ));
            } else if (Strings.isNullOrEmpty(practitioner)) {
                String parameterName = "Gaps in Care Report - " + subject;
                resolvePatientGapBundleForMeasures(
                    _periodStart, _periodEnd, _subject, topic, status, returnParams, measures, parameterName, organization
                );
            }
            return returnParams;
        }
        return returnParams;
    }

    private Boolean careGapParameterValidation(List<String> periodStart, List<String> periodEnd, List<String> subject, String topic,
            String practitioner, List<String> measureId, List<String> measureIdentifier, List<CanonicalType> measureUrl, List<String> status,
            String organization, String program, String periodStartIndice, String periodEndIndice, String subjectIndice) {

        if (periodStart.size() > 1)
            throw new IllegalArgumentException("Only one periodStart argument can be supplied.");

        if (periodEnd.size() > 1)
            throw new IllegalArgumentException("Only one periodEnd argument can be supplied.");

        if (subject.size() > 1 || subject.size() <= 0)
            throw new IllegalArgumentException("You must supply one and only one subject argument.");

        if(Strings.isNullOrEmpty(periodStartIndice) || Strings.isNullOrEmpty(periodEndIndice)) {
            throw new IllegalArgumentException("periodStart and periodEnd are required.");
        }

        //TODO - remove this - covered in check of subject/practitioner/organization - left in for now 'cause we need a subject to develop
        if (Strings.isNullOrEmpty(subjectIndice)) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (!Strings.isNullOrEmpty(organization)) {
            // //TODO - add this - left out for now 'cause we need a subject to develop
            // if (!Strings.isNullOrEmpty(subject)) {
            //     throw new IllegalArgumentException("If a organization is specified then only organization or practitioner may be specified.");
            // }
        }
        if(!Strings.isNullOrEmpty(practitioner) && Strings.isNullOrEmpty(organization)){
            throw new IllegalArgumentException("If a practitioner is specified then an organization must also be specified.");
        }
        if (!Strings.isNullOrEmpty(practitioner) && Strings.isNullOrEmpty(organization)) {
            // //TODO - add this - left out for now 'cause we need a subject to develop
            // if (!Strings.isNullOrEmpty(subject)) {
            //     throw new IllegalArgumentException("If practitioner and organization is specified then subject may not be specified.");
            // }
        }
        if(Strings.isNullOrEmpty(subjectIndice) && Strings.isNullOrEmpty(practitioner) && Strings.isNullOrEmpty(organization)) {
            throw new IllegalArgumentException("periodStart AND periodEnd AND (subject OR organization OR (practitioner AND organization)) MUST be provided");
        }
        if(!Strings.isNullOrEmpty(subjectIndice)) {
            if (!subjectIndice.startsWith("Patient/") && !subjectIndice.startsWith("Group/")) {
                throw new IllegalArgumentException("Subject must follow the format of either 'Patient/ID' OR 'Group/ID'.");
            }
        }
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status is required.");
        }
        for (String statusValue: status) {
            if(!Strings.isNullOrEmpty(statusValue) && (!statusValue.equalsIgnoreCase("open-gap") && !statusValue.equalsIgnoreCase("closed-gap") && !statusValue.equalsIgnoreCase("not-in-ip") )){
                throw new IllegalArgumentException("Status must be one or more of 'open-gap', 'closed-gap', or 'not-in-ip'.");
            }
        }
        if (measureIdentifier != null && !measureIdentifier.isEmpty()) {
            throw new NotYetImplementedException("measureIdentifier Not Yet Implemented.");
        }
        return true;
    }

    private List<Measure> resolveMeasures(List<String> measureId, List<String> measureIdentifier, List<CanonicalType> measureUrl) {
        List<Measure> measures = new ArrayList<Measure>();
        if (measureId == null || measureId.isEmpty()) {
            logger.info("No measure Ids found.");
        } else {
            measureId.forEach(id -> {
                Measure measure = this.measureResourceProvider.getDao().read(new IdType(id));
                if (measure != null) {
                    measures.add(measure);
                }
            });
        }
        logger.info("measureIdentifier: " + measureIdentifier + "Not Yet Supported");
        if (measureUrl == null || measureUrl.isEmpty()) {
            logger.info("No measure urls found.");
        } else {
            measureUrl.forEach(url -> {
                Measure measure = resolveMeasureByUrl(url);
                if (measure != null) {
                    measures.add(measure);
                }
            });
        }
        return measures;
    }

    private Measure resolveMeasureByUrl(CanonicalType url) {
        String urlValue = url.getValueAsString();
        if (!urlValue.contains("/Measure/")) {
            throw new IllegalArgumentException("Invalid resource type for determining Measure from url: " + url);
        }
        String [] urlsplit = urlValue.split("/Measure/");
        if (urlsplit.length != 2) {
            throw new IllegalArgumentException("Invalid url, Measure.url SHALL be <CanonicalBase>/Measure/<MeasureName>");
        }

        //TODO: need to do a lookup based on Measure name in order to get the Id.
        String measureName = urlsplit[1];
        IdType measureIdType = new IdType();
        if (measureName.contains("|")) {
            String[] nameVersion = measureName.split("\\|");
            String name = nameVersion[0];
            String version = nameVersion[1];
            measureIdType.setValue(name).withVersion(version);

        } else {
            measureIdType.setValue(measureName);
        }
        Measure measure = this.measureResourceProvider.getDao().read(measureIdType);
        return measure;
    }

    private void resolvePatientGapBundleForMeasures(String periodStart, String periodEnd, String subject, String topic, List<String> status,
            Parameters returnParams, List<Measure> measures, String name, String organization) {
        Bundle patientGapBundle = patientCareGap(periodStart, periodEnd, subject, topic, measures, status, organization);
        if (patientGapBundle != null) {
            Parameters.ParametersParameterComponent newParameter = new Parameters.ParametersParameterComponent()
                    .setName(name)
                    .setResource(patientGapBundle);
            //TODO - is this supposed to be something like "id": "multiple-gaps-indv-report01"??
            newParameter.setId(UUID.randomUUID().toString());
            returnParams.addParameter(newParameter);
        }
    }

    private List<Reference> getPatientListFromGroup(String subjectGroupRef){
        List<Reference> patientList = new ArrayList<>();
        IBaseResource baseGroup = registry.getResourceDao("Group").read(new IdType(subjectGroupRef));
        if (baseGroup == null) {
            throw new RuntimeException("Could not find Group/" + subjectGroupRef);
        }
        Group group = (Group)baseGroup;       
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

    private Bundle patientCareGap(String periodStart, String periodEnd, String subject, String topic, List<Measure> measures, List<String> status, String organization) {
        SearchParameterMap theParams = SearchParameterMap.newSynchronous();

        // if (theId != null) {
        //     var measureParam = new StringParam(theId.getIdPart());
        //     theParams.add("_id", measureParam);
        // }

        if (topic != null && !topic.equals("")) {
            TokenParam topicParam = new TokenParam(topic);
            theParams.add("topic", topicParam);
        }

        Bundle careGapReport = new Bundle();
        careGapReport.setMeta(new Meta().addProfile("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-bundle-deqm"));
        careGapReport.setType(Bundle.BundleType.DOCUMENT);
        careGapReport.setTimestamp(new Date());
        careGapReport.setId(UUID.randomUUID().toString());
        //TODO - this MIGHT be a specific string
        careGapReport.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID().toString()));

        Composition composition = new Composition();
        composition.setMeta(new Meta().addProfile("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-composition-deqm"));
        composition.setId(UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL)
                .setSubject(new Reference(subject.startsWith("Patient/") ? subject : "Patient/" + subject))
                .setTitle("Care Gap Report for " + subject)
                .setDate(new Date())
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setCode("96315-7")
                                .setSystem("http://loinc.org")
                                .setDisplay("Gaps in care report")));

        if (organization != null) {
            composition.setCustodian(new Reference(organization.startsWith("Organization/") ? organization : "Organization/" + organization));
        }

        Resource compositionAuthor = getCompositionAuthor();
        try {
            composition.setAuthor(Arrays.asList(new Reference(compositionAuthor)));
        } catch (Exception e) {
            logger.error(String.format("Composition author required."));
        }

        List<MeasureReport> reports = new ArrayList<>();
        List<DetectedIssue> detectedIssues = new ArrayList<DetectedIssue>();
        MeasureReport report = null;

        for (Measure measure : measures) {

            Composition.SectionComponent section = new Composition.SectionComponent();

            if (measure.hasTitle()) {
                section.setTitle(measure.getTitle());
            }

            // TODO - this is configured for patient-level evaluation only
            report = evaluateMeasure(measure.getIdElement(), periodStart, periodEnd, null, "patient", subject, null,
            null, null, null, null, null);

            report.setId(UUID.randomUUID().toString());
            report.setDate(new Date());
            report.setImprovementNotation(measure.getImprovementNotation());
            //TODO: this is an org hack && requires an Organization to be in the ruler
            Resource org = getReportingOrganization();
            if (org != null) {
                report.setReporter(new Reference(org));
            }
            report.setMeta(new Meta().addProfile("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm"));
            section.setFocus(new Reference("MeasureReport/" + report.getId()));
            //TODO: DetectedIssue
            //section.addEntry(new Reference("MeasureReport/" + report.getId()));

            if (report.hasGroup() && measure.hasScoring()) {
                double proportion = resolveProportion(report, measure);
                // TODO - this is super hacky ... change once improvementNotation is specified
                // as a code
                String improvementNotation = measure.getImprovementNotation().getCodingFirstRep().getCode().toLowerCase();
                DetectedIssue detectedIssue = new DetectedIssue();
                detectedIssue.setMeta(new Meta().addProfile("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-detectedissue-deqm"));
                if (closedGap(improvementNotation, proportion)) {
                        if (notReportingClosedGaps(status)) {
                            continue;
                        }
                        else {
                            detectedIssue.addModifierExtension(
                                new Extension("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-gapStatus",
                                new CodeableConcept(
                                    new Coding("http://hl7.org/fhir/us/davinci-deqm/CodeSystem/gaps-status", "closed-gap", null)
                                ))
                            );
                        }
                } else {
                    if (notReportingOpenGaps(status)) {
                        continue;
                    }
                    else {
                        detectedIssue.addModifierExtension(
                            new Extension("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-gapStatus",
                            new CodeableConcept(
                                new Coding("http://hl7.org/fhir/us/davinci-deqm/CodeSystem/gaps-status", "open-gap", null)
                            ))
                        );
                    }
                    section.setText(new Narrative()
                            .setStatus(Narrative.NarrativeStatus.GENERATED)
                            .setDiv(new XhtmlNode().setValue("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>No detected issues.</p></div>")));
                }

                detectedIssue.setId(UUID.randomUUID().toString());
                detectedIssue.setStatus(DetectedIssue.DetectedIssueStatus.FINAL);
                detectedIssue.setPatient(new Reference(subject.startsWith("Patient/") ? subject : "Patient/" + subject));
                detectedIssue.getEvidence().add(new DetectedIssue.DetectedIssueEvidenceComponent().addDetail(new Reference("MeasureReport/" + report.getId())));
                CodeableConcept code = new CodeableConcept()
                    .addCoding(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                    .setCode("CAREGAP")
                    .setDisplay("Care Gaps"));
                detectedIssue.setCode(code);

                section.addEntry(
                     new Reference("DetectedIssue/" + detectedIssue.getIdElement().getIdPart()));
                detectedIssues.add(detectedIssue);
                composition.addSection(section);
                reports.add(report);

                // TODO - add other types of improvement notation cases
            }
        }
        if (reports.isEmpty()) {
            return null;
        }
        
        careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(composition).setFullUrl(getFullUrl(composition.fhirType(), composition.getIdElement().getIdPart())));
        careGapReport.addEntry(new BundleEntryComponent().setResource(compositionAuthor).setFullUrl(getFullUrl(compositionAuthor.fhirType(), compositionAuthor.getIdElement().getIdPart())));
        for (MeasureReport rep : reports) {
            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(rep).setFullUrl(getFullUrl(rep.fhirType(), rep.getIdElement().getIdPart())));
            if (report.hasEvaluatedResource()) {
                for (Reference evaluatedResource : report.getEvaluatedResource()) {
                    // Assuming data is local only for now... 
                    IIdType theId = evaluatedResource.getReferenceElement();
                    String resourceType = theId.getResourceType();
                    if (resourceType != null) {
                        IBaseResource resourceBase = registry.getResourceDao(resourceType).read(theId);
                        if (resourceBase != null && resourceBase instanceof Resource) {
                            Resource resource = (Resource) resourceBase;
                            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(resource.fhirType(), resource.getIdElement().getIdPart())));
                        }
                    }
                }
            }
        }
        for (DetectedIssue detectedIssue : detectedIssues) {
            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(detectedIssue).setFullUrl(getFullUrl(detectedIssue.fhirType(), detectedIssue.getIdElement().getIdPart())));
        }
 
        return careGapReport;
    }

    private String getFullUrl(String fhirType, String elementId) {
        String fullUrl = String.format("%s%s/%s", serverAddress, fhirType, elementId);
        return fullUrl;
    }

    private Resource getCompositionAuthor() {
        return getLocalOrganization();
    }

    private Resource getReportingOrganization() {
        return getLocalOrganization();
    }

    private Organization localOrganization = null;
    private Resource getLocalOrganization() {
        //TODO: this is an org hack.  Need to figure out what the right thing is.
        if (localOrganization != null) {
            return localOrganization;
        } else {
            //getting weird behavior where the organization isn't updating.  I think this grabs the first version so if you update you don't see it.
            IFhirResourceDao<Organization> orgDao = this.registry.getResourceDao(Organization.class);
            List<IBaseResource> org = orgDao.search(SearchParameterMap.newSynchronous()).getResources(0, 1);
            if (org.isEmpty()) {
                return null;
            }
            IBaseResource baseOrganization = org.get(0);
            if (baseOrganization != null && baseOrganization instanceof Organization) {
                localOrganization = (Organization) baseOrganization;
                return localOrganization;
            }
            else return null;
        }
        
    }

    private double resolveProportion(MeasureReport report, Measure measure) {
        int numerator = 0;
        int denominator = 0;
        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            if (group.hasPopulation()) {
                for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                    // TODO - currently configured for measures with only 1 numerator and 1
                    // denominator
                    if (population.hasCode()) {
                        if (population.getCode().hasCoding()) {
                            for (Coding coding : population.getCode().getCoding()) {
                                if (coding.hasCode()) {
                                    if (coding.getCode().equals("numerator") && population.hasCount()) {
                                        numerator = population.getCount();
                                    } else if (coding.getCode().equals("denominator")
                                            && population.hasCount()) {
                                        denominator = population.getCount();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //TODO: implement this per the spec
        //Holding off on implementation using Measure Score pending guidance re consideration for programs that don't perform the calculation (they just use numer/denom)
        double proportion = 0.0;
        if (measure.getScoring().hasCoding() && denominator != 0) {
            for (Coding coding : measure.getScoring().getCoding()) {
                if (coding.hasCode() && coding.getCode().equals("proportion")) {
                    if (denominator != 0.0 ) {
                        proportion = numerator / denominator;
                    }
                }
            }
        }
        return proportion;
    }

    private boolean notReportingOpenGaps(List<String> status) {
        return !status.stream().anyMatch(x -> x.equalsIgnoreCase("open-gap"));
    }

    private boolean notReportingClosedGaps(List<String> status) {
        return !status.stream().anyMatch(x -> x.equalsIgnoreCase("closed-gap"));
    }

    private boolean closedGap(String improvementNotation, double proportion) {
        return ((improvementNotation.equals("increase")) && (proportion > 0.0))
                        ||  ((improvementNotation.equals("decrease")) && (proportion < 1.0));
    }

    @Operation(name = "$report", idempotent = true, type = MeasureReport.class)
    public Parameters report(@OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
                                     @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
                                     @OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException { 
        if (periodStart == null) {
            throw new IllegalArgumentException("Parameter 'periodStart' is required.");
        }    
        if (periodEnd == null) {
            throw new IllegalArgumentException("Parameter 'periodEnd' is required.");
        }    
        Date periodStartDate = DateHelper.resolveRequestDate(periodStart, true);
        Date periodEndDate = DateHelper.resolveRequestDate(periodEnd, false);
        if (periodStartDate.after(periodEndDate)) {
            throw new IllegalArgumentException("Parameter 'periodStart' must be before 'periodEnd'.");
        }
 
        if (subject == null) {
            throw new IllegalArgumentException("Parameter 'subject' is required.");
        }
        if (!subject.startsWith("Patient/") && !subject.startsWith("Group/")) {
            throw new IllegalArgumentException("Parameter 'subject' must be in the format 'Patient/[id]' or 'Group/[id]'.");
        }

        Parameters returnParams = new Parameters();
        returnParams.setId(subject.replace("/", "-") + "-report");
               
        (getPatientListFromSubject(subject))
            .forEach(
                patientSubject -> {
                    Parameters.ParametersParameterComponent patientParameter = patientReport(periodStartDate, periodEndDate, patientSubject.getReference());
                    returnParams.addParameter(patientParameter);
                }
            );

        return returnParams;
    }

    private Patient ensurePatient(String patient) {
        String patientId = patient.replace("Patient/", "");
        IFhirResourceDao<Patient> patientDao = this.registry.getResourceDao(Patient.class);
        Patient patientResource = patientDao.read(new IdType(patientId));
        if (patientResource == null) {
            throw new RuntimeException("Could not find Patient: " + patientId);
        }
        return patientResource;
    }

    private static String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";

    private Parameters.ParametersParameterComponent patientReport(Date periodStart, Date periodEnd, String subject) {
        Patient patient = ensurePatient(subject);
        final Map<IIdType, IAnyResource> patientResources = new HashMap<>();
        patientResources.put(patient.getIdElement(), patient);

        SearchParameterMap theParams = SearchParameterMap.newSynchronous();
        ReferenceParam subjectParam = new ReferenceParam(subject);
        theParams.add("subject", subjectParam);

        Bundle patientReportBundle = new Bundle();
            patientReportBundle.setMeta(new Meta().addProfile(PATIENT_REPORT_PROFILE_URL));
            patientReportBundle.setType(Bundle.BundleType.COLLECTION);
            patientReportBundle.setTimestamp(new Date());
            patientReportBundle.setId(subject.replace("/", "-") + "-report");
            patientReportBundle.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID().toString()));

        IFhirResourceDao<MeasureReport> measureReportDao = this.registry.getResourceDao(MeasureReport.class);
        measureReportDao.search(theParams).getAllResources().forEach(baseResource -> {
            MeasureReport measureReport = (MeasureReport)baseResource;        
            if (measureReport.getPeriod().getStart().before(periodStart) || measureReport.getPeriod().getStart().after(periodEnd)) {
                return;
            }           
            
            patientReportBundle.addEntry(
                new Bundle.BundleEntryComponent()
                    .setResource(measureReport)
                    .setFullUrl(getFullUrl(measureReport.fhirType(), measureReport.getIdElement().getIdPart()))
            );

            List<IAnyResource> resources;
            resources = addEvaluatedResources(measureReport);
            resources.forEach(resource -> {
                patientResources.putIfAbsent(resource.getIdElement(), resource);
            });
        });

        patientResources.entrySet().forEach(resource -> {
            patientReportBundle.addEntry(
                new Bundle.BundleEntryComponent()
                    .setResource((Resource) resource.getValue())
                    .setFullUrl(getFullUrl(resource.getValue().fhirType(), resource.getValue().getIdElement().getIdPart()))
            );
        });

        Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();
            patientParameter.setResource(patientReportBundle);
            patientParameter.setId(subject.replace("/", "-") + "-report");
            patientParameter.setName("return");
        return patientParameter;
    }

    private List<Reference> getPatientListFromSubject(String subject) {
        List<Reference> patientList = null;
        if (subject.startsWith("Patient/")) {
            Reference patientReference = new Reference(subject);
            patientList = new ArrayList<Reference>();
            patientList.add(patientReference);
        } else if (subject.startsWith("Group/")) {
            patientList = getPatientListFromGroup(subject);
        }
        return patientList;
    }

    @Operation(name = "$collect-data", idempotent = true, type = Measure.class)
    public Parameters collectData(@IdParam IdType theId, @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "patient") String patientRef,
            @OperationParam(name = "practitioner") String practitionerRef,
            @OperationParam(name = "lastReceivedOn") String lastReceivedOn) throws FHIRException {

        MeasureReport report = evaluateMeasure(theId, periodStart, periodEnd, null, null, patientRef, null,
            practitionerRef, lastReceivedOn, null, null, null);
        report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
        report.setGroup(null);

        Parameters parameters = new Parameters();
        parameters.addParameter(new Parameters.ParametersParameterComponent().setName("measureReport").setResource(report));

        addEvaluatedResourcesToParameters(report, parameters);

        return parameters;
    }

    private List<IAnyResource> addEvaluatedResources(MeasureReport report){
        List<IAnyResource> resources = new ArrayList<>();
        for (Reference evaluatedResource : report.getEvaluatedResource()) {
            IIdType theEvaluatedId = evaluatedResource.getReferenceElement();
            String resourceType = theEvaluatedId.getResourceType();
            if (resourceType != null) {
                IBaseResource resourceBase = registry.getResourceDao(resourceType).read(theEvaluatedId);
                if (resourceBase != null && resourceBase instanceof Resource) {
                    Resource resource = (Resource) resourceBase;
                    resources.add(resource);
                }
            }
        }
        return resources;
    }

    private void addEvaluatedResourcesToParameters(MeasureReport report, Parameters parameters) {
        List<IAnyResource> resources;
        resources = addEvaluatedResources(report);
        resources.forEach(resource -> {
            parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource").setResource((Resource) resource));
            }
        );
    }

    // TODO - this needs a lot of work
    @Operation(name = "$data-requirements", idempotent = true, type = Measure.class)
    public Library dataRequirements(@IdParam IdType theId,
            @OperationParam(name = "startPeriod") String startPeriod,
            @OperationParam(name = "endPeriod") String endPeriod) throws InternalErrorException, FHIRException {

        Measure measure = this.measureResourceProvider.getDao().read(theId);

        ModelManager modelManager = libraryHelper.getModelManager();
        LibraryManager libraryManager = libraryHelper.getLibraryManager(libraryResolutionProvider);

        Library library = dataRequirementsProvider.getLibraryFromMeasure(measure, libraryResolutionProvider);
        if (library == null) {
            throw new RuntimeException("Could not load measure library.");
        }

        CqlTranslator translator = TranslatorHelper.getTranslator(
                LibraryHelper.extractContentStream(library), libraryManager, modelManager);
        if (translator.getErrors().size() > 0) {
            throw new RuntimeException("Errors during library compilation.");
        }

        return this.dataRequirementsProvider.getModuleDefinitionLibrary(measure, libraryManager,
                translator.getTranslatedLibrary(), TranslatorHelper.getTranslatorOptions());
    }


    @SuppressWarnings("unchecked")
    @Operation(name = "$submit-data", idempotent = true, type = Measure.class)
    public Resource submitData(RequestDetails details, @IdParam IdType theId,
            @OperationParam(name = "measureReport", min = 1, max = 1, type = MeasureReport.class) MeasureReport report,
            @OperationParam(name = "resource") List<IAnyResource> resources) {
        Bundle transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);

        /*
         * TODO - resource validation using $data-requirements operation (params are the
         * provided id and the measurement period from the MeasureReport)
         * 
         * TODO - profile validation ... not sure how that would work ... (get
         * StructureDefinition from URL or must it be stored in Ruler?)
         */

        transactionBundle.addEntry(createTransactionEntry(report));

        if (resources != null) {
            for (IAnyResource resource : resources) {
                Resource res = (Resource) resource;
                if (res instanceof Bundle) {
                    for (Bundle.BundleEntryComponent entry : createTransactionBundle((Bundle) res).getEntry()) {
                        transactionBundle.addEntry(entry);
                    }
                } else {
                    // Build transaction bundle
                    transactionBundle.addEntry(createTransactionEntry(res));
                }
            }
        }

        return (Resource) this.registry.getSystemDao().transaction(details, transactionBundle);
    }

    private Bundle createTransactionBundle(Bundle bundle) {
        Bundle transactionBundle;
        if (bundle != null) {
            if (bundle.hasType() && bundle.getType() == Bundle.BundleType.TRANSACTION) {
                transactionBundle = bundle;
            } else {
                transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
                if (bundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasResource()) {
                            transactionBundle.addEntry(createTransactionEntry(entry.getResource()));
                        }
                    }
                }
            }
        } else {
            transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION).setEntry(new ArrayList<>());
        }

        return transactionBundle;
    }

    private Bundle.BundleEntryComponent createTransactionEntry(Resource resource) {
        Bundle.BundleEntryComponent transactionEntry = new Bundle.BundleEntryComponent().setResource(resource);
        if (resource.hasId()) {
            transactionEntry.setRequest(
                    new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT).setUrl(resource.getId()));
        } else {
            transactionEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(resource.fhirType()));
        }
        return transactionEntry;
    }

    
    @Operation(name = "$extract-line-list-data", idempotent = false)
    public Bundle extractLineListData(RequestDetails details,
        @OperationParam(name = "measureReport", min = 0, max = 1, type = MeasureReport.class) MeasureReport report,
        @OperationParam(name = "subjectList") List<String> subjectList) {
            IVersionSpecificBundleFactory bundleFactory = measureResourceProvider.getContext().newBundleFactory();
            Map<String, Reference> populationSubjectListReferenceMap = new HashMap<String, Reference>();
            gatherSubjectList(report, subjectList, populationSubjectListReferenceMap);
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
        for (Entry<String, Reference> entry : populationSubjectListReferenceMap.entrySet()) {
            // IQueryParameterType compositionReferenceParam = new ReferenceParam("subject", entry.getKey());
            SearchParameterMap map = SearchParameterMap.newSynchronous();//.add("composition", compositionReferenceParam);
            logger.warn(map.toNormalizedQueryString(measureResourceProvider.getContext()));
            List<IBaseResource> bundles = registry.getResourceDao(Bundle.class).search(map).getAllResources();
            for (IBaseResource baseBundle : bundles) {
                if (baseBundle instanceof Bundle) {
                    Bundle bundle = (Bundle) baseBundle;
                    if (bundle.hasEntry() && !bundle.getEntry().isEmpty() && bundle.hasType() && bundle.getType().equals(Bundle.BundleType.DOCUMENT)) {
                        IBaseResource firstEntry = bundle.getEntry().get(0).getResource();
                        if (!(firstEntry instanceof Composition)) {
                            logger.debug(String.format("Any bundle of type document must have the first entry of type Composition, but found: %s", firstEntry.fhirType()));
                        } else {
                            Composition composition = (Composition) firstEntry;
                            String referenceId = String.format("%s/%s", composition.getSubject().getReferenceElement().getResourceType(), composition.getSubject().getReferenceElement().getIdPart());
                            if (composition.getSubject().equals(entry.getValue()) || composition.getSubject().getReference().equals(entry.getKey())) {
                                eicrs.putIfAbsent(entry.getKey(), bundle);
                            } else if (referenceId != null && referenceId.equals(entry.getKey())) {
                                eicrs.putIfAbsent(entry.getKey(), bundle);
                            }
                        }
                    }
                }
            }
            // if (bundles != null && !bundles.isEmpty() && bundles.size() == 1) {
            //     bundleFactory.addResourcesToBundle(Arrays.asList(bundles.get(0)), BundleTypeEnum.COLLECTION, null, null, null);
            // }
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
        if (report.getSubject() != null && report.getSubject().hasReference()) {
            populationSubjectListReferenceMap.putIfAbsent(report.getSubject().getReference(), report.getSubject());
        }
        for (MeasureReportGroupComponent group : report.getGroup()) {
            for (MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                for (Reference subject : getSubjectResultsFromList(report, population.getSubjectResults())) {
                    if (subject.getReferenceElement().hasResourceType() && subject.getReferenceElement().getResourceType().equals("Patient")) {
                        populationSubjectListReferenceMap.putIfAbsent(subject.getReference(), subject);
                    }
                    if (subject.getReferenceElement().hasResourceType() && subject.getReferenceElement().getResourceType().equals("Group")) {
                        getPatientListFromGroup(subject.getReference()).forEach(patient -> {
                            populationSubjectListReferenceMap.putIfAbsent(patient.getReference(), patient);
                        });
                    }
                }
            }
        }
    }

    private List<Reference> getSubjectResultsFromList(MeasureReport report, Reference subjectResults) {
        List<Reference> results = new ArrayList<Reference>();
        if (subjectResults.getReference() == null) {
            logger.debug("No subject results found.");
            return results;
        }
        ListResource list = null;
        if (subjectResults.getReference().startsWith("#") && report.hasContained()) {
            for(Resource contained : report.getContained()) {
                if (contained instanceof ListResource) {
                    if (contained.getIdElement().getIdPart().equals(subjectResults.getReference())) {
                        list = (ListResource)contained; continue;
                    }
                }
            }
        } else {
            IBaseResource baseList = registry.getResourceDao(subjectResults.getReferenceElement().getResourceType()).read(subjectResults.getReferenceElement());
            if (baseList == null) {
                logger.debug(String.format("No subject results found for: %s", subjectResults.getReference()));
                return results;
            }
            if (!(baseList instanceof ListResource)) {
                throw new RuntimeException(String.format("Population subject reference was not a List, found: %s", baseList.fhirType()));
            }
            list = (ListResource) baseList;
        }
        if (list == null) {
            logger.debug("No subject results found.");
            return results;
        }
        for(ListEntryComponent entry: list.getEntry()) {
            if (entry.getItem().getReferenceElement().getResourceType().equals("Patient") || entry.getItem().getReferenceElement().getResourceType().equals("Group")) {
                results.add(entry.getItem());
            }
        }
        return results;
    }
}
