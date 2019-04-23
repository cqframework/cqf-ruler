package org.opencds.cqf.evaluation;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.builders.MeasureReportBuilder;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.helpers.FhirMeasureBundler;
import org.opencds.cqf.providers.JpaDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MeasureEvaluation {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluation.class);

    private DataProvider provider;
    private Interval measurementPeriod;

    public MeasureEvaluation(DataProvider provider, Interval measurementPeriod) {
        this.provider = provider;
        this.measurementPeriod = measurementPeriod;
    }

    public MeasureReport evaluatePatientMeasure(Measure measure, Context context, String patientId) {
        logger.info("Generating individual report");

        if (patientId == null) {
            return evaluatePopulationMeasure(measure, context);
        }

        Iterable<Object> patientRetrieve = provider.retrieve("Patient", patientId, "Patient", null, null, null, null, null, null, null, null);
        Patient patient = null;
        if (patientRetrieve.iterator().hasNext()) {
            patient = (Patient) patientRetrieve.iterator().next();
        }

        return evaluate(measure, context, patient == null ? Collections.emptyList() : Collections.singletonList(patient), MeasureReport.MeasureReportType.INDIVIDUAL);
    }

    public MeasureReport evaluatePatientListMeasure(Measure measure, Context context, String practitionerRef)
    {
        logger.info("Generating patient-list report");

        List<Patient> patients = practitionerRef == null ? getAllPatients() : getPractitionerPatients(practitionerRef);
        return evaluate(measure, context, patients, MeasureReport.MeasureReportType.PATIENTLIST);
    }

    private List<Patient> getPractitionerPatients(String practitionerRef) {
        SearchParameterMap map = new SearchParameterMap();
        map.add(
                "general-practitioner",
                new ReferenceParam(
                        practitionerRef.startsWith("Practitioner/")
                                ? practitionerRef
                                : "Practitioner/" + practitionerRef
                )
        );

        List<Patient> patients = new ArrayList<>();
        if (provider instanceof JpaDataProvider) {
            IBundleProvider patientProvider = ((JpaDataProvider) provider).resolveResourceProvider("Patient").getDao().search(map);
            List<IBaseResource> patientList = patientProvider.getResources(0, patientProvider.size());
            patientList.forEach(x -> patients.add((Patient) x));
        }
        return patients;
    }

    private List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        if (provider instanceof JpaDataProvider) {
            IBundleProvider patientProvider = ((JpaDataProvider) provider).resolveResourceProvider("Patient").getDao().search(new SearchParameterMap());
            List<IBaseResource> patientList = patientProvider.getResources(0, patientProvider.size());
            patientList.forEach(x -> patients.add((Patient) x));
        }
        return patients;
    }

    public MeasureReport evaluatePopulationMeasure(Measure measure, Context context) {
        logger.info("Generating summary report");

        return evaluate(measure, context, getAllPatients(), MeasureReport.MeasureReportType.SUMMARY);
    }

    public static enum MeasureScoring {
        PROPORTION,
        RATIO,
        CONTINUOUSVARIABLE,
        COHORT;

        private MeasureScoring() {
        }

        public static MeasureEvaluation.MeasureScoring fromCode(String codeString) {
            if(codeString != null && !"".equals(codeString)) {
                if("proportion".equals(codeString)) {
                    return PROPORTION;
                } else if("ratio".equals(codeString)) {
                    return RATIO;
                } else if("continuous-variable".equals(codeString)) {
                    return CONTINUOUSVARIABLE;
                } else if ("cohort".equals(codeString)) {
                    return COHORT;
                } else if(Configuration.isAcceptInvalidEnums()) {
                    return null;
                } else {
                    return null;
                    //throw new FHIRException("Unknown MeasureScoring code \'" + codeString + "\'");
                }
            } else {
                return null;
            }
        }

        public String toCode() {
            switch(ordinal()) {
                case 0:
                    return "proportion";
                case 1:
                    return "ratio";
                case 2:
                    return "continuous-variable";
                case 3:
                    return "cohort";
                default:
                    return "?";
            }
        }

        public String getSystem() {
            return "http://hl7.org/fhir/measure-scoring";
        }

        public String getDefinition() {
            switch(ordinal()) {
                case 0:
                    return "The measure score is defined using a proportion";
                case 1:
                    return "The measure score is defined using a ratio";
                case 2:
                    return "The score is defined by a calculation of some quantity";
                case 3:
                    return "The measure is a cohort definition";
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch(ordinal()) {
                case 0:
                    return "Proportion";
                case 1:
                    return "Ratio";
                case 2:
                    return "Continuous Variable";
                case 3:
                    return "Cohort";
                default:
                    return "?";
            }
        }
    }

    public static enum MeasurePopulationType {
        INITIALPOPULATION,
        NUMERATOR,
        NUMERATOREXCLUSION,
        DENOMINATOR,
        DENOMINATOREXCLUSION,
        DENOMINATOREXCEPTION,
        MEASUREPOPULATION,
        MEASUREPOPULATIONEXCLUSION,
        MEASUREOBSERVATION;

        private MeasurePopulationType() {
        }

        public static MeasureEvaluation.MeasurePopulationType fromCode(String codeString) {
            if(codeString != null && !"".equals(codeString)) {
                if("initial-population".equals(codeString)) {
                    return INITIALPOPULATION;
                } else if("numerator".equals(codeString)) {
                    return NUMERATOR;
                } else if("numerator-exclusion".equals(codeString)) {
                    return NUMERATOREXCLUSION;
                } else if ("denominator".equals(codeString)) {
                    return DENOMINATOR;
                } else if ("denominator-exclusion".equals(codeString)) {
                    return DENOMINATOREXCLUSION;
                } else if ("denominator-exception".equals(codeString)) {
                    return DENOMINATOREXCEPTION;
                } else if("measure-population".equals(codeString)) {
                    return MEASUREPOPULATION;
                } else if ("measure-population-exclusion".equals(codeString)) {
                    return MEASUREPOPULATIONEXCLUSION;
                } else if("measure-observation".equals(codeString)) {
                    return MEASUREOBSERVATION;
                } else if(Configuration.isAcceptInvalidEnums()) {
                    return null;
                } else {
                    return null;
                    //throw new FHIRException("Unknown MeasureScoring code \'" + codeString + "\'");
                }
            } else {
                return null;
            }
        }

        public String toCode() {
            switch(ordinal()) {
                case 0:
                    return "initial-population";
                case 1:
                    return "numerator";
                case 2:
                    return "numerator-exclusion";
                case 3:
                    return "denominator";
                case 4:
                    return "denominator-exclusion";
                case 5:
                    return "denominator-exception";
                case 6:
                    return "measure-population";
                case 7:
                    return "measure-population-exclusion";
                case 8:
                    return "measure-observation";
                default:
                    return "?";
            }
        }

        public String getSystem() {
            return "http://hl7.org/fhir/measure-population";
        }

        public String getDefinition() {
            switch(ordinal()) {
                case 0:
                    return "The initial population refers to all patients or events to be evaluated by a quality measure involving patients who share a common set of specified characterstics. All patients or events counted (for example, as numerator, as denominator) are drawn from the initial population";
                case 1:
                    return "\tThe upper portion of a fraction used to calculate a rate, proportion, or ratio. Also called the measure focus, it is the target process, condition, event, or outcome. Numerator criteria are the processes or outcomes expected for each patient, or event defined in the denominator. A numerator statement describes the clinical action that satisfies the conditions of the measure";
                case 2:
                    return "Numerator exclusion criteria define patients or events to be removed from the numerator. Numerator exclusions are used in proportion and ratio measures to help narrow the numerator (for inverted measures)";
                case 3:
                    return "The lower portion of a fraction used to calculate a rate, proportion, or ratio. The denominator can be the same as the initial population, or a subset of the initial population to further constrain the population for the purpose of the measure";
                case 4:
                    return "Denominator exclusion criteria define patients or events that should be removed from the denominator before determining if numerator criteria are met. Denominator exclusions are used in proportion and ratio measures to help narrow the denominator. For example, patients with bilateral lower extremity amputations would be listed as a denominator exclusion for a measure requiring foot exams";
                case 5:
                    return "Denominator exceptions are conditions that should remove a patient or event from the denominator of a measure only if the numerator criteria are not met. Denominator exception allows for adjustment of the calculated score for those providers with higher risk populations. Denominator exception criteria are only used in proportion measures";
                case 6:
                    return "Measure population criteria define the patients or events for which the individual observation for the measure should be taken. Measure populations are used for continuous variable measures rather than numerator and denominator criteria";
                case 7:
                    return "Measure population criteria define the patients or events that should be removed from the measure population before determining the outcome of one or more continuous variables defined for the measure observation. Measure population exclusion criteria are used within continuous variable measures to help narrow the measure population";
                case 8:
                    return "Defines the individual observation to be performed for each patient or event in the measure population. Measure observations for each case in the population are aggregated to determine the overall measure score for the population";
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch(ordinal()) {
                case 0:
                    return "Initial Population";
                case 1:
                    return "Numerator";
                case 2:
                    return "Numerator Exclusion";
                case 3:
                    return "Denominator";
                case 4:
                    return "Denominator Exclusion";
                case 5:
                    return "Denominator Exception";
                case 6:
                    return "Measure Population";
                case 7:
                    return "Measure Population Exclusion";
                case 8:
                    return "Measure Observation";
                default:
                    return "?";
            }
        }
    }

    private Iterable<Resource> evaluateCriteria(Context context, Patient patient, Measure.MeasureGroupPopulationComponent pop) {
        context.setContextValue("Patient", patient.getIdElement().getIdPart());
        Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
        if (result instanceof Boolean) {
            if (((Boolean)result)) {
                return Collections.singletonList(patient);
            }
            else {
                return Collections.emptyList();
            }
        }

        return (Iterable)result;
    }

    private boolean evaluatePopulationCriteria(Context context, Patient patient,
                                               Measure.MeasureGroupPopulationComponent criteria, HashMap<String, Resource> population, HashMap<String, Patient> populationPatients,
                                               Measure.MeasureGroupPopulationComponent exclusionCriteria, HashMap<String, Resource> exclusionPopulation, HashMap<String, Patient> exclusionPatients
    ) {
        boolean inPopulation = false;
        if (criteria != null) {
            for (Resource resource : evaluateCriteria(context, patient, criteria)) {
                inPopulation = true;
                population.put(resource.getId(), resource);
            }
        }

        if (inPopulation) {
            // Are they in the exclusion?
            if (exclusionCriteria != null) {
                for (Resource resource : evaluateCriteria(context, patient, exclusionCriteria)) {
                    inPopulation = false;
                    exclusionPopulation.put(resource.getId(), resource);
                    population.remove(resource.getId());
                }
            }
        }

        if (inPopulation && populationPatients != null) {
            populationPatients.put(patient.getId(), patient);
        }
        if (!inPopulation && exclusionPatients != null) {
            exclusionPatients.put(patient.getId(), patient);
        }

        return inPopulation;
    }

    private void addPopulationCriteriaReport(MeasureReport report, MeasureReport.MeasureReportGroupComponent reportGroup, Measure.MeasureGroupPopulationComponent populationCriteria, int populationCount, Iterable<Patient> patientPopulation) {
        if (populationCriteria != null) {
            MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationReport.setCode(populationCriteria.getCode());
            populationReport.setIdentifier(populationCriteria.getIdentifier());
            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST && patientPopulation != null) {
                ListResource patientList = new ListResource();
                patientList.setId(UUID.randomUUID().toString());
                populationReport.setPatients(new Reference().setReference("#" + patientList.getId()));
                for (Patient patient : patientPopulation) {
                    ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
                            .setItem(new Reference().setReference(
                                    patient.getId().startsWith("Patient/") ?
                                            patient.getId() :
                                            String.format("Patient/%s", patient.getId()))
                                    .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
                    patientList.addEntry(entry);
                }
                report.addContained(patientList);
            }
            populationReport.setCount(populationCount);
            reportGroup.addPopulation(populationReport);
        }
    }

    private MeasureReport evaluate(Measure measure, Context context, List<Patient> patients, MeasureReport.MeasureReportType type)
    {
        MeasureReportBuilder reportBuilder = new MeasureReportBuilder();
        reportBuilder.buildStatus("complete");
        reportBuilder.buildType(type);
        reportBuilder.buildMeasureReference(measure.getIdElement().getValue());
        if (type == MeasureReport.MeasureReportType.INDIVIDUAL && !patients.isEmpty()) {
            reportBuilder.buildPatientReference(patients.get(0).getIdElement().getValue());
        }
        reportBuilder.buildPeriod(measurementPeriod);

        MeasureReport report = reportBuilder.build();

        List<Patient> initialPopulation = getInitalPopulation(measure, patients, context);

        HashMap<String,Resource> resources = new HashMap<>();
        HashMap<String,HashSet<String>> codeToResourceMap = new HashMap<>();

        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

        MeasureScoring measureScoring = MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        if (measureScoring == null) {
            throw new RuntimeException("Measure scoring is required in order to calculate.");
        }

        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            MeasureReport.MeasureReportGroupComponent reportGroup = new MeasureReport.MeasureReportGroupComponent();
            reportGroup.setIdentifier(group.getIdentifier());
            report.getGroup().add(reportGroup);

            // Declare variables to avoid a hash lookup on every patient
            // TODO: Isn't quite right, there may be multiple initial populations for a ratio measure...
            Measure.MeasureGroupPopulationComponent initialPopulationCriteria = null;
            Measure.MeasureGroupPopulationComponent numeratorCriteria = null;
            Measure.MeasureGroupPopulationComponent numeratorExclusionCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorExclusionCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorExceptionCriteria = null;
            Measure.MeasureGroupPopulationComponent measurePopulationCriteria = null;
            Measure.MeasureGroupPopulationComponent measurePopulationExclusionCriteria = null;
            // TODO: Isn't quite right, there may be multiple measure observations...
            Measure.MeasureGroupPopulationComponent measureObservationCriteria = null;

            HashMap<String, Resource> numerator = null;
            HashMap<String, Resource> numeratorExclusion = null;
            HashMap<String, Resource> denominator = null;
            HashMap<String, Resource> denominatorExclusion = null;
            HashMap<String, Resource> denominatorException = null;
            HashMap<String, Resource> measurePopulation = null;
            HashMap<String, Resource> measurePopulationExclusion = null;
            HashMap<String, Resource> measureObservation = null;

            HashMap<String, Patient> numeratorPatients = null;
            HashMap<String, Patient> numeratorExclusionPatients = null;
            HashMap<String, Patient> denominatorPatients = null;
            HashMap<String, Patient> denominatorExclusionPatients = null;
            HashMap<String, Patient> denominatorExceptionPatients = null;
            HashMap<String, Patient> measurePopulationPatients = null;
            HashMap<String, Patient> measurePopulationExclusionPatients = null;

            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());
                if (populationType != null) {
                    switch (populationType) {
                        case INITIALPOPULATION:
                            initialPopulationCriteria = pop;
                            // Initial population is already computed
                            break;
                        case NUMERATOR:
                            numeratorCriteria = pop;
                            numerator = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case NUMERATOREXCLUSION:
                            numeratorExclusionCriteria = pop;
                            numeratorExclusion = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorExclusionPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case DENOMINATOR:
                            denominatorCriteria = pop;
                            denominator = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case DENOMINATOREXCLUSION:
                            denominatorExclusionCriteria = pop;
                            denominatorExclusion = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExclusionPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case DENOMINATOREXCEPTION:
                            denominatorExceptionCriteria = pop;
                            denominatorException = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExceptionPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case MEASUREPOPULATION:
                            measurePopulationCriteria = pop;
                            measurePopulation = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case MEASUREPOPULATIONEXCLUSION:
                            measurePopulationExclusionCriteria = pop;
                            measurePopulationExclusion = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationExclusionPatients = new HashMap<String, Patient>();
                            }
                            break;
                        case MEASUREOBSERVATION:
                            break;
                    }
                }
            }

            switch (measureScoring) {
                case PROPORTION:
                case RATIO: {

                    // For each patient in the initial population
                    for (Patient patient : initialPopulation) {

                        // Are they in the denominator?
                        boolean inDenominator = evaluatePopulationCriteria(context, patient,
                                denominatorCriteria, denominator, denominatorPatients,
                                denominatorExclusionCriteria, denominatorExclusion, denominatorExclusionPatients);
                        populateResourceMap(context, MeasurePopulationType.DENOMINATOR, resources, codeToResourceMap);

                        if (inDenominator) {
                            // Are they in the numerator?
                            boolean inNumerator = evaluatePopulationCriteria(context, patient,
                                    numeratorCriteria, numerator, numeratorPatients,
                                    numeratorExclusionCriteria, numeratorExclusion, numeratorExclusionPatients);
                            populateResourceMap(context, MeasurePopulationType.NUMERATOR, resources, codeToResourceMap);

                            if (!inNumerator && inDenominator && (denominatorExceptionCriteria != null)) {
                                // Are they in the denominator exception?
                                boolean inException = false;
                                for (Resource resource : evaluateCriteria(context, patient, denominatorExceptionCriteria)) {
                                    inException = true;
                                    denominatorException.put(resource.getId(), resource);
                                    denominator.remove(resource.getId());
                                    populateResourceMap(context, MeasurePopulationType.DENOMINATOREXCEPTION, resources, codeToResourceMap);
                                }
                                if (inException) {
                                    if (denominatorExceptionPatients != null) {
                                        denominatorExceptionPatients.put(patient.getId(), patient);
                                    }
                                    if (denominatorPatients != null) {
                                        denominatorPatients.remove(patient.getId());
                                    }
                                }
                            }
                        }
                    }

                    // Calculate actual measure score, Count(numerator) / Count(denominator)
                    if (denominator != null && numerator != null && denominator.size() > 0) {
                        reportGroup.setMeasureScore(numerator.size() / (double)denominator.size());
                    }

                    break;
                }
                case CONTINUOUSVARIABLE: {

                    // For each patient in the initial population
                    for (Patient patient : initialPopulation) {

                        // Are they in the measure population?
                        boolean inMeasurePopulation = evaluatePopulationCriteria(context, patient,
                                measurePopulationCriteria, measurePopulation, measurePopulationPatients,
                                measurePopulationExclusionCriteria, measurePopulationExclusion, measurePopulationExclusionPatients);

                        if (inMeasurePopulation) {
                            // TODO: Evaluate measure observations
                            for (Resource resource : evaluateCriteria(context, patient, measureObservationCriteria)) {
                                measureObservation.put(resource.getId(), resource);
                            }
                        }
                    }

                    break;
                }
                case COHORT: {
                    // Only initial population matters for a cohort measure
                    break;
                }
            }

            // Add population reports for each group
            addPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria, initialPopulation != null ? initialPopulation.size() : 0, initialPopulation);
            addPopulationCriteriaReport(report, reportGroup, numeratorCriteria, numerator != null ? numerator.size() : 0, numeratorPatients != null ? numeratorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, numeratorExclusionCriteria, numeratorExclusion != null ? numeratorExclusion.size() : 0, numeratorExclusionPatients != null ? numeratorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorCriteria, denominator != null ? denominator.size() : 0, denominatorPatients != null ? denominatorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExclusionCriteria, denominatorExclusion != null ? denominatorExclusion.size() : 0, denominatorExclusionPatients != null ? denominatorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExceptionCriteria, denominatorException != null ? denominatorException.size() : 0, denominatorExceptionPatients != null ? denominatorExceptionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationCriteria,  measurePopulation != null ? measurePopulation.size() : 0, measurePopulationPatients != null ? measurePopulationPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationExclusionCriteria,  measurePopulationExclusion != null ? measurePopulationExclusion.size() : 0, measurePopulationExclusionPatients != null ? measurePopulationExclusionPatients.values() : null);
            // TODO: Measure Observations...
        }

        for (String key : codeToResourceMap.keySet()) {
            org.hl7.fhir.dstu3.model.ListResource list = new org.hl7.fhir.dstu3.model.ListResource();
            for (String element : codeToResourceMap.get(key)) {
                org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent comp = new org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent();
                comp.setItem(new Reference('#' + element));
                list.addEntry(comp);
            }

            if (!list.isEmpty()) {
                list.setId(UUID.randomUUID().toString());
                list.setTitle(key);
                resources.put(list.getId(), list);
            }
        }

        if (!resources.isEmpty()) {
            FhirMeasureBundler bundler = new FhirMeasureBundler();
            org.hl7.fhir.dstu3.model.Bundle evaluatedResources = bundler.bundle(resources.values());
            evaluatedResources.setId(UUID.randomUUID().toString());
            report.setEvaluatedResources(new Reference('#' + evaluatedResources.getId()));
            report.addContained(evaluatedResources);
        }

        return report;
    }

    private void populateResourceMap(Context context, MeasurePopulationType type, HashMap<String, Resource> resources,  HashMap<String,HashSet<String>> codeToResourceMap) {
        if (context.getEvaluatedResources().isEmpty()) {
            return;
        }

        if (!codeToResourceMap.containsKey(type.toCode())) {
            codeToResourceMap.put(type.toCode(), new HashSet<>());
        }

        HashSet<String> codeHashSet = codeToResourceMap.get((type.toCode()));

        for (Object o : context.getEvaluatedResources()) {
            if (o instanceof Resource){
                Resource r = (Resource)o;
                String id = r.getId();
                if (!codeHashSet.contains(id)) {
                    codeHashSet.add(id);
                }

                if (!resources.containsKey(id)) {
                    resources.put(id, r);
                }
            }
        }

        context.clearEvaluatedResources();
    }

    private List<Patient> getInitalPopulation(Measure measure, List<Patient> population, Context context) {
        // TODO: Needs to account for multiple population groups
        List<Patient> initalPop = new ArrayList<>();
        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                if (pop.getCode().getCodingFirstRep().getCode().equals("initial-population")) {
                    for (Patient patient : population) {
                        context.setContextValue("Patient", patient.getIdElement().getIdPart());
                        Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
                        if (result == null) {
                            continue;
                        }
                        if ((Boolean) result) {
                            initalPop.add(patient);
                        }
                    }
                }
            }
        }

        return initalPop;
    }
}