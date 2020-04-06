package org.opencds.cqf.dstu3.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.dstu3.evaluation.MeasureEvaluation;
import org.opencds.cqf.dstu3.evaluation.MeasureEvaluationSeed;
import org.opencds.cqf.dstu3.evaluation.ProviderFactory;
import org.opencds.cqf.dstu3.helpers.LibraryHelper;
import org.opencds.cqf.library.stu3.NarrativeProvider;
import org.opencds.cqf.measure.stu3.CqfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.rp.dstu3.MeasureResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

public class MeasureOperationsProvider {

    private NarrativeProvider narrativeProvider;
    private HQMFProvider hqmfProvider;
    private DataRequirementsProvider dataRequirementsProvider;

    private LibraryResolutionProvider<Library> libraryResolutionProvider;
    private MeasureResourceProvider measureResourceProvider;
    private DaoRegistry registry;
    private EvaluationProviderFactory factory;


    private static final Logger logger = LoggerFactory.getLogger(MeasureOperationsProvider.class);

    public MeasureOperationsProvider(DaoRegistry registry, EvaluationProviderFactory factory, NarrativeProvider narrativeProvider, HQMFProvider hqmfProvider, LibraryResolutionProvider<Library> libraryResolutionProvider,
    MeasureResourceProvider measureResourceProvider) {
        this.registry = registry;
        this.factory = factory;

        this.libraryResolutionProvider = libraryResolutionProvider;
        this.narrativeProvider = narrativeProvider;
        this.hqmfProvider = hqmfProvider;
        this.dataRequirementsProvider = new DataRequirementsProvider();
        this.measureResourceProvider = measureResourceProvider;
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
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource, this.libraryResolutionProvider);

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

        Narrative n = this.narrativeProvider.getNarrative(this.measureResourceProvider.getContext(), cqfMeasure);
        theResource.setText(n.copy());
        // logger.info("Narrative: " + n.getDivAsString());
        return this.measureResourceProvider.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-narrative", idempotent = true, type = Measure.class)
    public Parameters getNarrative(@IdParam IdType theId) {
        Measure theResource = this.measureResourceProvider.getDao().read(theId);
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource, this.libraryResolutionProvider);
        Narrative n = this.narrativeProvider.getNarrative(this.measureResourceProvider.getContext(), cqfMeasure);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }

    private String generateHQMF(Measure theResource) {
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource, this.libraryResolutionProvider);
        return this.hqmfProvider.generateHQMF(cqfMeasure);
    }

    /*
     *
     * NOTE that the source, user, and pass parameters are not standard parameters
     * for the FHIR $evaluate-measure operation
     *
     */
    @Operation(name = "$evaluate-measure", idempotent = true, type = Measure.class)
    public MeasureReport evaluateMeasure(@IdParam IdType theId, @RequiredParam(name = "periodStart") String periodStart,
            @RequiredParam(name = "periodEnd") String periodEnd, @OptionalParam(name = "measure") String measureRef,
            @OptionalParam(name = "reportType") String reportType, @OptionalParam(name = "patient") String patientRef,
            @OptionalParam(name = "productLine") String productLine,
            @OptionalParam(name = "practitioner") String practitionerRef,
            @OptionalParam(name = "lastReceivedOn") String lastReceivedOn,
            @OptionalParam(name = "source") String source, @OptionalParam(name = "user") String user,
            @OptionalParam(name = "pass") String pass) throws InternalErrorException, FHIRException {
        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.libraryResolutionProvider);
        MeasureEvaluationSeed seed = new MeasureEvaluationSeed(this.factory, libraryLoader, this.libraryResolutionProvider);
        Measure measure = this.measureResourceProvider.getDao().read(theId);

        if (measure == null) {
            throw new RuntimeException("Could not find Measure/" + theId.getIdPart());
        }

        seed.setup(measure, periodStart, periodEnd, productLine, source, user, pass);

        // resolve report type
        MeasureEvaluation evaluator = new MeasureEvaluation(seed.getDataProvider(), this.registry, seed.getMeasurementPeriod());
        if (reportType != null) {
            switch (reportType) {
            case "patient":
                return evaluator.evaluatePatientMeasure(seed.getMeasure(), seed.getContext(), patientRef);
            case "patient-list": 
                return evaluator.evaluatePatientListMeasure(seed.getMeasure(), seed.getContext(), practitionerRef);
            case "population":
                return evaluator.evaluatePopulationMeasure(seed.getMeasure(), seed.getContext());
            default:
                throw new IllegalArgumentException("Invalid report type: " + reportType);
            }
        }

        // default report type is patient
        MeasureReport report = evaluator.evaluatePatientMeasure(seed.getMeasure(), seed.getContext(), patientRef);
        if (productLine != null)
        {
            Extension ext = new Extension();
            ext.setUrl("http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine");
            ext.setValue(new StringType(productLine));
            report.addExtension(ext);
        }

        return report;
    }

    // @Operation(name = "$evaluate-measure-with-source", idempotent = true)
    // public MeasureReport evaluateMeasure(@IdParam IdType theId,
    //         @OperationParam(name = "sourceData", min = 1, max = 1, type = Bundle.class) Bundle sourceData,
    //         @OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
    //         @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd) {
    //     if (periodStart == null || periodEnd == null) {
    //         throw new IllegalArgumentException("periodStart and periodEnd are required for measure evaluation");
    //     }
    //     LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.libraryResourceProvider);
    //     MeasureEvaluationSeed seed = new MeasureEvaluationSeed(this.factory, libraryLoader, this.libraryResourceProvider);
    //     Measure measure = this.getDao().read(theId);

    //     if (measure == null) {
    //         throw new RuntimeException("Could not find Measure/" + theId.getIdPart());
    //     }

    //     seed.setup(measure, periodStart, periodEnd, null, null, null, null);
    //     BundleDataProviderStu3 bundleProvider = new BundleDataProviderStu3(sourceData);
    //     bundleProvider.setTerminologyProvider(provider.getTerminologyProvider());
    //     seed.getContext().registerDataProvider("http://hl7.org/fhir", bundleProvider);
    //     MeasureEvaluation evaluator = new MeasureEvaluation(bundleProvider, seed.getMeasurementPeriod());
    //     return evaluator.evaluatePatientMeasure(seed.getMeasure(), seed.getContext(), "");
    // }

    @Operation(name = "$care-gaps", idempotent = true)
    public Bundle careGapsReport(@RequiredParam(name = "periodStart") String periodStart,
            @RequiredParam(name = "periodEnd") String periodEnd, @RequiredParam(name = "topic") String topic,
            @RequiredParam(name = "patient") String patientRef) {
        List<IBaseResource> measures = this.measureResourceProvider.getDao().search(new SearchParameterMap().add("topic",
                new TokenParam().setModifier(TokenParamModifier.TEXT).setValue(topic))).getResources(0, 1000);
        Bundle careGapReport = new Bundle();
        careGapReport.setType(Bundle.BundleType.DOCUMENT);

        Composition composition = new Composition();
        // TODO - this is a placeholder code for now ... replace with preferred code
        // once identified
        CodeableConcept typeCode = new CodeableConcept()
                .addCoding(new Coding().setSystem("http://loinc.org").setCode("57024-2"));
        composition.setStatus(Composition.CompositionStatus.FINAL).setType(typeCode)
                .setSubject(new Reference(patientRef.startsWith("Patient/") ? patientRef : "Patient/" + patientRef))
                .setTitle(topic + " Care Gap Report");

        List<MeasureReport> reports = new ArrayList<>();
        MeasureReport report = new MeasureReport();
        for (IBaseResource resource : measures) {
            Composition.SectionComponent section = new Composition.SectionComponent();

            Measure measure = (Measure) resource;
            section.addEntry(
                    new Reference(measure.getIdElement().getResourceType() + "/" + measure.getIdElement().getIdPart()));
            if (measure.hasTitle()) {
                section.setTitle(measure.getTitle());
            }
            String improvementNotation = "increase"; // defaulting to "increase"
            if (measure.hasImprovementNotation()) {
                improvementNotation = measure.getImprovementNotation();
                section.setText(new Narrative().setStatus(Narrative.NarrativeStatus.GENERATED)
                        .setDiv(new XhtmlNode().setValue(improvementNotation)));
            }

            LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.libraryResolutionProvider);
            MeasureEvaluationSeed seed = new MeasureEvaluationSeed(this.factory, libraryLoader, this.libraryResolutionProvider);
            seed.setup(measure, periodStart, periodEnd, null, null, null, null);
            MeasureEvaluation evaluator = new MeasureEvaluation(seed.getDataProvider(), this.registry, seed.getMeasurementPeriod());
            // TODO - this is configured for patient-level evaluation only
            report = evaluator.evaluatePatientMeasure(seed.getMeasure(), seed.getContext(), patientRef);

            if (report.hasGroup() && measure.hasScoring()) {
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

                double proportion = 0.0;
                if (measure.getScoring().hasCoding() && denominator != 0) {
                    for (Coding coding : measure.getScoring().getCoding()) {
                        if (coding.hasCode() && coding.getCode().equals("proportion")) {
                            proportion = numerator / denominator;
                        }
                    }
                }

                // TODO - this is super hacky ... change once improvementNotation is specified
                // as a code
                if (improvementNotation.toLowerCase().contains("increase")) {
                    if (proportion < 1.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                } else if (improvementNotation.toLowerCase().contains("decrease")) {
                    if (proportion > 0.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                }

                // TODO - add other types of improvement notation cases
            }
        }

        careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(composition));

        for (MeasureReport rep : reports) {
            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(rep));
        }

        return careGapReport;
    }

    @Operation(name = "$collect-data", idempotent = true, type = Measure.class)
    public Parameters collectData(@IdParam IdType theId, @RequiredParam(name = "periodStart") String periodStart,
            @RequiredParam(name = "periodEnd") String periodEnd, @OptionalParam(name = "patient") String patientRef,
            @OptionalParam(name = "practitioner") String practitionerRef,
            @OptionalParam(name = "lastReceivedOn") String lastReceivedOn) throws FHIRException {
        // TODO: Spec says that the periods are not required, but I am not sure what to
        // do when they aren't supplied so I made them required
        MeasureReport report = evaluateMeasure(theId, periodStart, periodEnd, null, null,  patientRef, null,
                practitionerRef, lastReceivedOn, null, null, null);
        report.setGroup(null);

        Parameters parameters = new Parameters();

        parameters.addParameter(
                new Parameters.ParametersParameterComponent().setName("measurereport").setResource(report));

        if (report.hasContained()) {
            for (Resource contained : report.getContained()) {
                if (contained instanceof Bundle) {
                    addEvaluatedResourcesToParameters((Bundle) contained, parameters);
                }
            }
        }

        // TODO: need a way to resolve referenced resources within the evaluated
        // resources
        // Should be able to use _include search with * wildcard, but HAPI doesn't
        // support that

        return parameters;
    }

    private void addEvaluatedResourcesToParameters(Bundle contained, Parameters parameters) {
        Map<String, Resource> resourceMap = new HashMap<>();
        if (contained.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : contained.getEntry()) {
                if (entry.hasResource() && !(entry.getResource() instanceof ListResource)) {
                    if (!resourceMap.containsKey(entry.getResource().getIdElement().getValue())) {
                        parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource")
                                .setResource(entry.getResource()));

                        resourceMap.put(entry.getResource().getIdElement().getValue(), entry.getResource());

                        resolveReferences(entry.getResource(), parameters, resourceMap);
                    }
                }
            }
        }
    }

    private void resolveReferences(Resource resource, Parameters parameters, Map<String, Resource> resourceMap) {
        List<IBase> values;
        for (BaseRuntimeChildDefinition child : this.measureResourceProvider.getContext().getResourceDefinition(resource).getChildren()) {
            values = child.getAccessor().getValues(resource);
            if (values == null || values.isEmpty()) {
                continue;
            }

            else if (values.get(0) instanceof Reference
                    && ((Reference) values.get(0)).getReferenceElement().hasResourceType()
                    && ((Reference) values.get(0)).getReferenceElement().hasIdPart()) {
                Resource fetchedResource = (Resource) registry
                        .getResourceDao(((Reference) values.get(0)).getReferenceElement().getResourceType())
                        .read(new IdType(((Reference) values.get(0)).getReferenceElement().getIdPart()));

                if (!resourceMap.containsKey(fetchedResource.getIdElement().getValue())) {
                    parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource")
                            .setResource(fetchedResource));

                    resourceMap.put(fetchedResource.getIdElement().getValue(), fetchedResource);
                }
            }
        }
    }

    // TODO - this needs a lot of work
    @Operation(name = "$data-requirements", idempotent = true, type = Measure.class)
    public org.hl7.fhir.dstu3.model.Library dataRequirements(@IdParam IdType theId,
            @RequiredParam(name = "startPeriod") String startPeriod,
            @RequiredParam(name = "endPeriod") String endPeriod) throws InternalErrorException, FHIRException {
        
        Measure measure = this.measureResourceProvider.getDao().read(theId);
        return this.dataRequirementsProvider.getDataRequirements(measure, this.libraryResolutionProvider);
    }

    @Operation(name = "$submit-data", idempotent = true, type = Measure.class)
    public Resource submitData(RequestDetails details, @IdParam IdType theId,
            @OperationParam(name = "measure-report", min = 1, max = 1, type = MeasureReport.class) MeasureReport report,
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
}
