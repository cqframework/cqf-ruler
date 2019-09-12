package org.opencds.cqf.dstu3.evaluation;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.dstu3.builders.MeasureReportBuilder;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.dstu3.helpers.FhirMeasureBundler;
import org.opencds.cqf.dstu3.providers.JpaDataProvider;
import org.opencds.cqf.qdm.providers.Qdm54DataProvider;
import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientRepository;
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

    public MeasureReport evaluateQdmPatientMeasure(Measure measure, Context context, String patientId) {
        logger.info("Generating individual report");

        if (patientId == null) {
            return evaluateQdmPopulationMeasure(measure, context);
        }

        Iterable<Object> patientRetrieve = provider.retrieve("Patient", patientId, "Patient", null, null, null, null, null, null, null, null);
        org.opencds.cqf.qdm.fivepoint4.model.Patient patient = null;
        if (patientRetrieve.iterator().hasNext()) {
            patient = (org.opencds.cqf.qdm.fivepoint4.model.Patient) patientRetrieve.iterator().next();
        }

        return evaluateQdm(measure, context, patient == null ? Collections.emptyList() : Collections.singletonList(patient), MeasureReport.MeasureReportType.INDIVIDUAL);
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

    private List<org.opencds.cqf.qdm.fivepoint4.model.Patient> getAllQdmPatients() {
        List<org.opencds.cqf.qdm.fivepoint4.model.Patient> patients = new ArrayList<>();
        if (provider instanceof Qdm54DataProvider) {
            List<org.opencds.cqf.qdm.fivepoint4.model.Patient> patientList = QdmContext.getBean(PatientRepository.class).findAll();
            patients.addAll(patientList);
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

    public MeasureReport evaluateQdmPopulationMeasure(Measure measure, Context context) {
        logger.info("Generating summary report");

        return evaluateQdm(measure, context, getAllQdmPatients(), MeasureReport.MeasureReportType.SUMMARY);
    }

    public MeasureReport evaluatePopulationMeasure(Measure measure, Context context) {
        logger.info("Generating summary report");

        return evaluate(measure, context, getAllPatients(), MeasureReport.MeasureReportType.SUMMARY);
    }

    private Iterable<org.opencds.cqf.qdm.fivepoint4.model.Patient> evaluateQdmCriteria(Context context, org.opencds.cqf.qdm.fivepoint4.model.Patient patient, Measure.MeasureGroupPopulationComponent pop) {
        if (!pop.hasCriteria()) {
            return Collections.emptyList();
        }
        
        context.setContextValue("Patient", patient.getId().getValue());
        Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
        if (result == null) {
            return Collections.emptyList();
        }
        
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

    private Iterable<Resource> evaluateCriteria(Context context, Patient patient, Measure.MeasureGroupPopulationComponent pop) {
        if (!pop.hasCriteria()) {
            return Collections.emptyList();
        }

        context.setContextValue("Patient", patient.getIdElement().getIdPart());
        Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
        if (result == null) {
            return Collections.emptyList();
        }

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

    private boolean evaluateQdmPopulationCriteria(
            Context context, org.opencds.cqf.qdm.fivepoint4.model.Patient patient,
            Measure.MeasureGroupPopulationComponent criteria, HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> population,
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> populationPatients,
            Measure.MeasureGroupPopulationComponent exclusionCriteria, HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> exclusionPopulation,
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> exclusionPatients
    ) {
        boolean inPopulation = false;
        if (criteria != null) {
            for (org.opencds.cqf.qdm.fivepoint4.model.Patient resource : evaluateQdmCriteria(context, patient, criteria)) {
                inPopulation = true;
                population.put(resource.getId().getValue(), resource);
            }
        }

        if (inPopulation) {
            // Are they in the exclusion?
            if (exclusionCriteria != null) {
                for (org.opencds.cqf.qdm.fivepoint4.model.Patient resource : evaluateQdmCriteria(context, patient, exclusionCriteria)) {
                    inPopulation = false;
                    exclusionPopulation.put(resource.getId().getValue(), resource);
                    population.remove(resource.getId());
                }
            }
        }

        if (inPopulation && populationPatients != null) {
            populationPatients.put(patient.getId().getValue(), patient);
        }
        if (!inPopulation && exclusionPatients != null) {
            exclusionPatients.put(patient.getId().getValue(), patient);
        }

        return inPopulation;
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

    private void addQdmPopulationCriteriaReport(
            MeasureReport report, MeasureReport.MeasureReportGroupComponent reportGroup,
            Measure.MeasureGroupPopulationComponent populationCriteria, int populationCount,
            Iterable<org.opencds.cqf.qdm.fivepoint4.model.Patient> patientPopulation)
    {
        if (populationCriteria != null) {
            MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationReport.setCode(populationCriteria.getCode());
            populationReport.setIdentifier(populationCriteria.getIdentifier());
            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST && patientPopulation != null) {
                ListResource patientList = new ListResource();
                patientList.setId(UUID.randomUUID().toString());
                populationReport.setPatients(new Reference().setReference("#" + patientList.getId()));
                for (org.opencds.cqf.qdm.fivepoint4.model.Patient patient : patientPopulation) {
                    ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
                            .setItem(new Reference().setReference(
                                    patient.getId().getValue().startsWith("Patient/") ?
                                            patient.getId().getValue() :
                                            String.format("Patient/%s", patient.getId()))
                                    .setDisplay(patient.getId().getValue()));
                    patientList.addEntry(entry);
                }
                report.addContained(patientList);
            }
            populationReport.setCount(populationCount);
            reportGroup.addPopulation(populationReport);
        }
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

    private MeasureReport evaluateQdm(Measure measure, Context context, List<org.opencds.cqf.qdm.fivepoint4.model.Patient> patients, MeasureReport.MeasureReportType type)
    {
        MeasureReportBuilder reportBuilder = new MeasureReportBuilder();
        reportBuilder.buildStatus("complete");
        reportBuilder.buildType(type);
        reportBuilder.buildMeasureReference(measure.getIdElement().getValue());
        if (type == MeasureReport.MeasureReportType.INDIVIDUAL && !patients.isEmpty()) {
            reportBuilder.buildPatientReference(patients.get(0).getId().getValue());
        }
        reportBuilder.buildPeriod(measurementPeriod);

        MeasureReport report = reportBuilder.build();

        HashMap<String,BaseType> resources = new HashMap<>();
        HashMap<String,HashSet<String>> codeToResourceMap = new HashMap<>();

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

            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> initialPopulation = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> numerator = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> numeratorExclusion = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominator = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominatorExclusion = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominatorException = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> measurePopulation = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> measurePopulationExclusion = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> measureObservation = null;

            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> initialPopulationPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> numeratorPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> numeratorExclusionPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominatorPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominatorExclusionPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> denominatorExceptionPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> measurePopulationPatients = null;
            HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient> measurePopulationExclusionPatients = null;

            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());
                if (populationType != null) {
                    switch (populationType) {
                        case INITIALPOPULATION:
                            initialPopulationCriteria = pop;
                            initialPopulation = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                initialPopulationPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case NUMERATOR:
                            numeratorCriteria = pop;
                            numerator = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case NUMERATOREXCLUSION:
                            numeratorExclusionCriteria = pop;
                            numeratorExclusion = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorExclusionPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case DENOMINATOR:
                            denominatorCriteria = pop;
                            denominator = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case DENOMINATOREXCLUSION:
                            denominatorExclusionCriteria = pop;
                            denominatorExclusion = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExclusionPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case DENOMINATOREXCEPTION:
                            denominatorExceptionCriteria = pop;
                            denominatorException = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExceptionPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case MEASUREPOPULATION:
                            measurePopulationCriteria = pop;
                            measurePopulation = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            }
                            break;
                        case MEASUREPOPULATIONEXCLUSION:
                            measurePopulationExclusionCriteria = pop;
                            measurePopulationExclusion = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationExclusionPatients = new HashMap<String, org.opencds.cqf.qdm.fivepoint4.model.Patient>();
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
                    for (org.opencds.cqf.qdm.fivepoint4.model.Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluateQdmPopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateQdmResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the denominator?
                            boolean inDenominator = evaluateQdmPopulationCriteria(context, patient,
                                    denominatorCriteria, denominator, denominatorPatients,
                                    denominatorExclusionCriteria, denominatorExclusion, denominatorExclusionPatients);
                            populateQdmResourceMap(context, MeasurePopulationType.DENOMINATOR, resources, codeToResourceMap);

                            if (inDenominator) {
                                // Are they in the numerator?
                                boolean inNumerator = evaluateQdmPopulationCriteria(context, patient,
                                        numeratorCriteria, numerator, numeratorPatients,
                                        numeratorExclusionCriteria, numeratorExclusion, numeratorExclusionPatients);
                                populateQdmResourceMap(context, MeasurePopulationType.NUMERATOR, resources, codeToResourceMap);

                                if (!inNumerator && inDenominator && (denominatorExceptionCriteria != null)) {
                                    // Are they in the denominator exception?
                                    boolean inException = false;
                                    for (org.opencds.cqf.qdm.fivepoint4.model.Patient resource : evaluateQdmCriteria(context, patient, denominatorExceptionCriteria)) {
                                        inException = true;
                                        denominatorException.put(resource.getId().getValue(), resource);
                                        denominator.remove(resource.getId().getValue());
                                        populateQdmResourceMap(context, MeasurePopulationType.DENOMINATOREXCEPTION, resources, codeToResourceMap);
                                    }
                                    if (inException) {
                                        if (denominatorExceptionPatients != null) {
                                            denominatorExceptionPatients.put(patient.getId().getValue(), patient);
                                        }
                                        if (denominatorPatients != null) {
                                            denominatorPatients.remove(patient.getId().getValue());
                                        }
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
                    for (org.opencds.cqf.qdm.fivepoint4.model.Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluateQdmPopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateQdmResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the measure population?
                            boolean inMeasurePopulation = evaluateQdmPopulationCriteria(context, patient,
                                    measurePopulationCriteria, measurePopulation, measurePopulationPatients,
                                    measurePopulationExclusionCriteria, measurePopulationExclusion, measurePopulationExclusionPatients);

                            if (inMeasurePopulation) {
                                // TODO: Evaluate measure observations
                                for (org.opencds.cqf.qdm.fivepoint4.model.Patient resource : evaluateQdmCriteria(context, patient, measureObservationCriteria)) {
                                    measureObservation.put(resource.getId().getValue(), resource);
                                }
                            }
                        }
                    }

                    break;
                }
                case COHORT: {
                    // For each patient in the initial population
                    for (org.opencds.cqf.qdm.fivepoint4.model.Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluateQdmPopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateQdmResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);
                    }
                    break;
                }
            }

            // Add population reports for each group
            addQdmPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria, initialPopulation != null ? initialPopulation.size() : 0, initialPopulationPatients != null ? initialPopulationPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, numeratorCriteria, numerator != null ? numerator.size() : 0, numeratorPatients != null ? numeratorPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, numeratorExclusionCriteria, numeratorExclusion != null ? numeratorExclusion.size() : 0, numeratorExclusionPatients != null ? numeratorExclusionPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, denominatorCriteria, denominator != null ? denominator.size() : 0, denominatorPatients != null ? denominatorPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, denominatorExclusionCriteria, denominatorExclusion != null ? denominatorExclusion.size() : 0, denominatorExclusionPatients != null ? denominatorExclusionPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, denominatorExceptionCriteria, denominatorException != null ? denominatorException.size() : 0, denominatorExceptionPatients != null ? denominatorExceptionPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, measurePopulationCriteria,  measurePopulation != null ? measurePopulation.size() : 0, measurePopulationPatients != null ? measurePopulationPatients.values() : null);
            addQdmPopulationCriteriaReport(report, reportGroup, measurePopulationExclusionCriteria,  measurePopulationExclusion != null ? measurePopulationExclusion.size() : 0, measurePopulationExclusionPatients != null ? measurePopulationExclusionPatients.values() : null);
            // TODO: Measure Observations...
        }

//        for (String key : codeToResourceMap.keySet()) {
//            org.hl7.fhir.dstu3.model.ListResource list = new org.hl7.fhir.dstu3.model.ListResource();
//            for (String element : codeToResourceMap.get(key)) {
//                org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent comp = new org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent();
//                comp.setItem(new Reference('#' + element));
//                list.addEntry(comp);
//            }
//
//            if (!list.isEmpty()) {
//                list.setId(UUID.randomUUID().toString());
//                list.setTitle(key);
//                resources.put(list.getId(), list);
//            }
//        }

//        if (!resources.isEmpty()) {
//            FhirMeasureBundler bundler = new FhirMeasureBundler();
//            org.hl7.fhir.dstu3.model.Bundle evaluatedResources = bundler.bundle(resources.values());
//            evaluatedResources.setId(UUID.randomUUID().toString());
//            report.setEvaluatedResources(new Reference('#' + evaluatedResources.getId()));
//            report.addContained(evaluatedResources);
//        }

        return report;
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

        HashMap<String,Resource> resources = new HashMap<>();
        HashMap<String,HashSet<String>> codeToResourceMap = new HashMap<>();

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

            HashMap<String, Resource> initialPopulation = null;
            HashMap<String, Resource> numerator = null;
            HashMap<String, Resource> numeratorExclusion = null;
            HashMap<String, Resource> denominator = null;
            HashMap<String, Resource> denominatorExclusion = null;
            HashMap<String, Resource> denominatorException = null;
            HashMap<String, Resource> measurePopulation = null;
            HashMap<String, Resource> measurePopulationExclusion = null;
            HashMap<String, Resource> measureObservation = null;

            HashMap<String, Patient> initialPopulationPatients = null;
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
                            initialPopulation = new HashMap<String, Resource>();
                            if (type == MeasureReport.MeasureReportType.PATIENTLIST) {
                                initialPopulationPatients = new HashMap<String, Patient>();
                            }
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
                    for (Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
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
                    }

                    // Calculate actual measure score, Count(numerator) / Count(denominator)
                    if (denominator != null && numerator != null && denominator.size() > 0) {
                        reportGroup.setMeasureScore(numerator.size() / (double)denominator.size());
                    }

                    break;
                }
                case CONTINUOUSVARIABLE: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
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
                    }

                    break;
                }
                case COHORT: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {
                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);
                    }

                    break;
                }
            }

            // Add population reports for each group
            addPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria, initialPopulation != null ? initialPopulation.size() : 0, initialPopulationPatients != null ? initialPopulationPatients.values() : null);
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

    private void populateQdmResourceMap(
            Context context, MeasurePopulationType type, HashMap<String, BaseType> resources,
            HashMap<String,HashSet<String>> codeToResourceMap)
    {
        if (context.getEvaluatedResources().isEmpty()) {
            return;
        }

        if (!codeToResourceMap.containsKey(type.toCode())) {
            codeToResourceMap.put(type.toCode(), new HashSet<>());
        }

        HashSet<String> codeHashSet = codeToResourceMap.get((type.toCode()));

        for (Object o : context.getEvaluatedResources()) {
            if (o instanceof BaseType){
                BaseType r = (BaseType) o;
                String id = r.getId().getValue();
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

    private void populateResourceMap(
            Context context, MeasurePopulationType type, HashMap<String, Resource> resources,
            HashMap<String,HashSet<String>> codeToResourceMap)
    {
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
}