package org.opencds.cqf.r4.evaluation;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.common.evaluation.MeasurePopulationType;
import org.opencds.cqf.common.evaluation.MeasureScoring;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.r4.builders.MeasureReportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;

public class MeasureEvaluation {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluation.class);

    private DataProvider provider;
    private Interval measurementPeriod;
    private DaoRegistry registry;

    public MeasureEvaluation(DataProvider provider, DaoRegistry registry, Interval measurementPeriod) {
        this.provider = provider;
        this.registry = registry;
        this.measurementPeriod = measurementPeriod;
    }

    public MeasureReport evaluatePatientMeasure(Measure measure, Context context, String patientId) {
        logger.info("Generating individual report");

        if (patientId == null) {
            return evaluatePopulationMeasure(measure, context);
        }

        Iterable<Object> patientRetrieve = provider.retrieve("Patient", "id", patientId, "Patient", null, null, null,
                null, null, null, null, null);
        Patient patient = null;
        if (patientRetrieve.iterator().hasNext()) {
            patient = (Patient) patientRetrieve.iterator().next();
        }

        boolean isSingle = true;
        return evaluate(measure, context,
                patient == null ? Collections.emptyList() : Collections.singletonList(patient),
                MeasureReport.MeasureReportType.INDIVIDUAL, isSingle);
    }

    public MeasureReport evaluateSubjectListMeasure(Measure measure, Context context, String practitionerRef) {
        logger.info("Generating patient-list report");

        List<Patient> patients = practitionerRef == null ? getAllPatients() : getPractitionerPatients(practitionerRef);
        boolean isSingle = false;
        return evaluate(measure, context, patients, MeasureReport.MeasureReportType.SUBJECTLIST, isSingle);
    }

    private List<Patient> getPractitionerPatients(String practitionerRef) {
        SearchParameterMap map = SearchParameterMap.newSynchronous();
        map.add("general-practitioner", new ReferenceParam(
                practitionerRef.startsWith("Practitioner/") ? practitionerRef : "Practitioner/" + practitionerRef));

        List<Patient> patients = new ArrayList<>();
        IBundleProvider patientProvider = registry.getResourceDao("Patient").search(map);
        List<IBaseResource> patientList = patientProvider.getAllResources();
        patientList.forEach(x -> patients.add((Patient) x));
        return patients;
    }

    private List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        IBundleProvider patientProvider = registry.getResourceDao("Patient").search(SearchParameterMap.newSynchronous());
        List<IBaseResource> patientList = patientProvider.getAllResources();
        patientList.forEach(x -> patients.add((Patient) x));
        return patients;
    }

    private List<Coverage> getAllCoverages() {
        List<Coverage> coverages = new ArrayList<>();
        IBundleProvider coverageProvider = registry.getResourceDao("Coverage").search(SearchParameterMap.newSynchronous());
        List<IBaseResource> coverageList = coverageProvider.getAllResources();
        coverageList.forEach(x -> coverages.add((Coverage) x));
        return coverages;
    }

    public MeasureReport evaluatePopulationMeasure(Measure measure, Context context) {
        logger.info("Generating summary report");

        boolean isSingle = false;
        return evaluate(measure, context, getAllPatients(), MeasureReport.MeasureReportType.SUMMARY, isSingle);
    }

    @SuppressWarnings("unchecked")
    private void clearExpressionCache(Context context) {
        // Hack to clear expression cache
        // See cqf-ruler github issue #153
        try {
            Field privateField = Context.class.getDeclaredField("expressions");
            privateField.setAccessible(true);
            LinkedHashMap<String, Object> expressions = (LinkedHashMap<String, Object>) privateField.get(context);
            expressions.clear();

        } catch (Exception e) {
            logger.warn("Error resetting expression cache", e);
        }
    }

    private Resource evaluateObservationCriteria(Context context, Patient patient, Resource resource, Measure.MeasureGroupPopulationComponent pop, MeasureReport report) {
        if (pop == null || !pop.hasCriteria()) {
            return null;
        }

        context.setContextValue("Patient", patient.getIdElement().getIdPart());

        clearExpressionCache(context);

        String observationName = pop.getCriteria().getExpression();
        ExpressionDef ed = context.resolveExpressionRef(observationName);
        if (!(ed instanceof FunctionDef)) {
            throw new IllegalArgumentException(String.format("Measure observation %s does not reference a function definition", observationName));
        }

        Object result = null;
        context.pushWindow();
        try {
            context.push(new Variable().withName(((FunctionDef)ed).getOperand().get(0).getName()).withValue(resource));
            result = ed.getExpression().evaluate(context);
        }
        finally {
            context.popWindow();
        }

        if (result instanceof Resource) {
            return (Resource)result;
        }

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(UUID.randomUUID().toString());
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        Extension obsExtension = new Extension().setUrl("http://hl7.org/fhir/StructureDefinition/cqf-measureInfo");
        Extension extExtMeasure = new Extension()
                .setUrl("measure")
                .setValue(new CanonicalType(report.getMeasure()));
        obsExtension.addExtension(extExtMeasure);
        Extension extExtPop = new Extension()
                .setUrl("populationId")
                .setValue(new StringType(observationName));
        obsExtension.addExtension(extExtPop);
        obs.addExtension(obsExtension);
        return obs;
    }

    @SuppressWarnings("unchecked")
    private Iterable<Resource> evaluateCriteria(Context context, Patient patient,
            Measure.MeasureGroupPopulationComponent pop) {
        if (pop == null || !pop.hasCriteria()) {
            return Collections.emptyList();
        }

        context.setContextValue("Patient", patient.getIdElement().getIdPart());

        clearExpressionCache(context);

        Object result = context.resolveExpressionRef(pop.getCriteria().getExpression()).evaluate(context);
        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof Boolean) {
            if (((Boolean) result)) {
                return Collections.singletonList(patient);
            } else {
                return Collections.emptyList();
            }
        }

        return (Iterable<Resource>) result;
    }

    private boolean evaluatePopulationCriteria(Context context, Patient patient,
            Measure.MeasureGroupPopulationComponent criteria, HashMap<String, Resource> population,
            HashMap<String, Patient> populationPatients, Measure.MeasureGroupPopulationComponent exclusionCriteria,
            HashMap<String, Resource> exclusionPopulation, HashMap<String, Patient> exclusionPatients) {
        boolean inPopulation = false;
        if (criteria != null) {
            for (Resource resource : evaluateCriteria(context, patient, criteria)) {
                inPopulation = true;
                population.put(resource.getIdElement().getIdPart(), resource);
            }
        }

        if (inPopulation) {
            // Are they in the exclusion?
            if (exclusionCriteria != null) {
                for (Resource resource : evaluateCriteria(context, patient, exclusionCriteria)) {
                    inPopulation = false;
                    exclusionPopulation.put(resource.getIdElement().getIdPart(), resource);
                    population.remove(resource.getIdElement().getIdPart());
                }
            }
        }

        if (inPopulation && populationPatients != null) {
            populationPatients.put(patient.getIdElement().getIdPart(), patient);
        }
        if (!inPopulation && exclusionPatients != null) {
            exclusionPatients.put(patient.getIdElement().getIdPart(), patient);
        }

        return inPopulation;
    }

    private void addPopulationCriteriaReport(MeasureReport report,
            MeasureReport.MeasureReportGroupComponent reportGroup,
            Measure.MeasureGroupPopulationComponent populationCriteria, int populationCount,
            Iterable<Patient> patientPopulation) {
        if (populationCriteria != null) {
            MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationReport.setCode(populationCriteria.getCode());
            if (report.getType() == MeasureReport.MeasureReportType.SUBJECTLIST && patientPopulation != null) {
                ListResource SUBJECTLIST = new ListResource();
                SUBJECTLIST.setId(UUID.randomUUID().toString());
                Reference subjectListReference = new Reference().setReferenceElement(new IdType("ListResource", "#" + SUBJECTLIST.getId()));
                populationReport.setSubjectResults(subjectListReference);
                for (Patient patient : patientPopulation) {
                    ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
                            .setItem(new Reference()
                                    .setReference(patient.getIdElement().getIdPart().startsWith("Patient/")
                                            ? patient.getIdElement().getIdPart()
                                            : String.format("Patient/%s", patient.getIdElement().getIdPart()))
                                    .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
                    SUBJECTLIST.addEntry(entry);
                }
                report.addContained(SUBJECTLIST);
            }
            populationReport.setCount(populationCount);
            reportGroup.addPopulation(populationReport);
        }
    }

    private MeasureReport evaluate(Measure measure, Context context, List<Patient> patients,
            MeasureReport.MeasureReportType type, boolean isSingle) {
        MeasureReportBuilder reportBuilder = new MeasureReportBuilder();
        reportBuilder.buildStatus("complete");
        reportBuilder.buildType(type);
        try {
            reportBuilder.buildMeasureReference(measure.getUrl());
        } catch (Exception e) {
            logger.error("Measure must have a canonical url.");
        }
        if (type == MeasureReport.MeasureReportType.INDIVIDUAL && !patients.isEmpty()) {
            IdType patientId = patients.get(0).getIdElement();
            reportBuilder.buildPatientReference(patientId.getResourceType() + "/" + patientId.getIdPart());
        }
        reportBuilder.buildPeriod(measurementPeriod);

        MeasureReport report = reportBuilder.build();

        HashMap<String, Resource> resources = new HashMap<>();
        HashMap<Pair<String, String>, HashSet<String>> codeToResourceMap = new HashMap<>();

        MeasureScoring measureScoring = MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        if (measureScoring == null) {
            throw new RuntimeException("Measure scoring is required in order to calculate.");
        }

        List<Measure.MeasureSupplementalDataComponent> sde = new ArrayList<>();
        HashMap<String, HashMap<String, Integer>> sdeAccumulators = null;
        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            MeasureReport.MeasureReportGroupComponent reportGroup = new MeasureReport.MeasureReportGroupComponent();
            reportGroup.setId(group.getId());
            report.getGroup().add(reportGroup);

            // Declare variables to avoid a hash lookup on every patient
            // TODO: Isn't quite right, there may be multiple initial populations for a
            // ratio measure...
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

            sdeAccumulators = new HashMap<>();
             sde = measure.getSupplementalData();
            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType
                        .fromCode(pop.getCode().getCodingFirstRep().getCode());
                if (populationType != null) {
                    switch (populationType) {
                        case INITIALPOPULATION:
                            initialPopulationCriteria = pop;
                            initialPopulation = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                initialPopulationPatients = new HashMap<>();
                            }
                            break;
                        case NUMERATOR:
                            numeratorCriteria = pop;
                            numerator = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                numeratorPatients = new HashMap<>();
                            }
                            break;
                        case NUMERATOREXCLUSION:
                            numeratorExclusionCriteria = pop;
                            numeratorExclusion = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                numeratorExclusionPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOR:
                            denominatorCriteria = pop;
                            denominator = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                denominatorPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOREXCLUSION:
                            denominatorExclusionCriteria = pop;
                            denominatorExclusion = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                denominatorExclusionPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOREXCEPTION:
                            denominatorExceptionCriteria = pop;
                            denominatorException = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                denominatorExceptionPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREPOPULATION:
                            measurePopulationCriteria = pop;
                            measurePopulation = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                measurePopulationPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREPOPULATIONEXCLUSION:
                            measurePopulationExclusionCriteria = pop;
                            measurePopulationExclusion = new HashMap<>();
                            if (type == MeasureReport.MeasureReportType.SUBJECTLIST) {
                                measurePopulationExclusionPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREOBSERVATION:
                            measureObservationCriteria = pop;
                            measureObservation = new HashMap<>();
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
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient,
                                initialPopulationCriteria, initialPopulation, initialPopulationPatients, null, null,
                                null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources,
                                codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the denominator?
                            boolean inDenominator = evaluatePopulationCriteria(context, patient, denominatorCriteria,
                                    denominator, denominatorPatients, denominatorExclusionCriteria,
                                    denominatorExclusion, denominatorExclusionPatients);
                            populateResourceMap(context, MeasurePopulationType.DENOMINATOR, resources,
                                    codeToResourceMap);

                            if (inDenominator) {
                                // Are they in the numerator?
                                boolean inNumerator = evaluatePopulationCriteria(context, patient, numeratorCriteria,
                                        numerator, numeratorPatients, numeratorExclusionCriteria, numeratorExclusion,
                                        numeratorExclusionPatients);
                                populateResourceMap(context, MeasurePopulationType.NUMERATOR, resources,
                                        codeToResourceMap);

                                if (!inNumerator && inDenominator && (denominatorExceptionCriteria != null)) {
                                    // Are they in the denominator exception?
                                    boolean inException = false;
                                    for (Resource resource : evaluateCriteria(context, patient,
                                            denominatorExceptionCriteria)) {
                                        inException = true;
                                        denominatorException.put(resource.getIdElement().getIdPart(), resource);
                                        denominator.remove(resource.getIdElement().getIdPart());
                                        populateResourceMap(context, MeasurePopulationType.DENOMINATOREXCEPTION,
                                                resources, codeToResourceMap);
                                    }
                                    if (inException) {
                                        if (denominatorExceptionPatients != null) {
                                            denominatorExceptionPatients.put(patient.getIdElement().getIdPart(),
                                                    patient);
                                        }
                                        if (denominatorPatients != null) {
                                            denominatorPatients.remove(patient.getIdElement().getIdPart());
                                        }
                                    }
                                }
                            }
                        }
                        populateSDEAccumulators(measure, context, patient, sdeAccumulators, sde);
                    }

                    // Calculate actual measure score, Count(numerator) / Count(denominator)
                    if (denominator != null && numerator != null && denominator.size() > 0) {
                        reportGroup.setMeasureScore(new Quantity(numerator.size() / (double) denominator.size()));
                    }

                    break;
                }
                case CONTINUOUSVARIABLE: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient,
                                initialPopulationCriteria, initialPopulation, initialPopulationPatients, null, null,
                                null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources,
                                codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the measure population?
                            boolean inMeasurePopulation = evaluatePopulationCriteria(context, patient,
                                    measurePopulationCriteria, measurePopulation, measurePopulationPatients,
                                    measurePopulationExclusionCriteria, measurePopulationExclusion,
                                    measurePopulationExclusionPatients);

                            if (inMeasurePopulation) {
                                for (Resource resource : measurePopulation.values()) {
                                    Resource observation = evaluateObservationCriteria(context, patient, resource, measureObservationCriteria, report);
                                    measureObservation.put(resource.getIdElement().getIdPart(), observation);
                                    report.addContained(observation);
                                    report.getEvaluatedResource().add(new Reference("#" + observation.getId()));
                                }
                            }
                        }
                        populateSDEAccumulators(measure, context, patient, sdeAccumulators,sde);
                    }

                    break;
                }
                case COHORT: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {
                        evaluatePopulationCriteria(context, patient,
                                initialPopulationCriteria, initialPopulation, initialPopulationPatients, null, null,
                                null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources,
                                codeToResourceMap);
                        populateSDEAccumulators(measure, context, patient, sdeAccumulators, sde);
                    }

                    break;
                }
            }

            // Add population reports for each group
            addPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria,
                    initialPopulation != null ? initialPopulation.size() : 0,
                    initialPopulationPatients != null ? initialPopulationPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, numeratorCriteria,
                    numerator != null ? numerator.size() : 0,
                    numeratorPatients != null ? numeratorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, numeratorExclusionCriteria,
                    numeratorExclusion != null ? numeratorExclusion.size() : 0,
                    numeratorExclusionPatients != null ? numeratorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorCriteria,
                    denominator != null ? denominator.size() : 0,
                    denominatorPatients != null ? denominatorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExclusionCriteria,
                    denominatorExclusion != null ? denominatorExclusion.size() : 0,
                    denominatorExclusionPatients != null ? denominatorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExceptionCriteria,
                    denominatorException != null ? denominatorException.size() : 0,
                    denominatorExceptionPatients != null ? denominatorExceptionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationCriteria,
                    measurePopulation != null ? measurePopulation.size() : 0,
                    measurePopulationPatients != null ? measurePopulationPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationExclusionCriteria,
                    measurePopulationExclusion != null ? measurePopulationExclusion.size() : 0,
                    measurePopulationExclusionPatients != null ? measurePopulationExclusionPatients.values() : null);
            // TODO: Measure Observations...
        }

        List<Reference> evaluatedResourceIds = new ArrayList<>();
        Map<String, Reference> referenceMap = new HashMap<String, Reference>();
        for (Pair<String, String> key : codeToResourceMap.keySet()) {
            org.hl7.fhir.r4.model.ListResource list = new org.hl7.fhir.r4.model.ListResource();
            for (String element : codeToResourceMap.get(key)) {
                if (referenceMap.containsKey(element)) {
                    referenceMap.get(element).addExtension("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-populationReference",
                        new StringType(key.getLeft()));
                    if (!evaluatedResourceIds.contains(referenceMap.get(element))) {
                        evaluatedResourceIds.add(referenceMap.get(element));
                    }
                } else {
                    org.hl7.fhir.r4.model.ListResource.ListEntryComponent comp = new org.hl7.fhir.r4.model.ListResource.ListEntryComponent();
                    Reference reference = new Reference(element);
                    comp.setItem(reference);
                    list.addEntry(comp);
                    // Do not want to add extension to ListEntryReference
                    reference.addExtension("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-populationReference",
                            new StringType(key.getLeft()));
                    if (!evaluatedResourceIds.contains(reference)) {
                        evaluatedResourceIds.add(reference);
                    }
                    referenceMap.put(element, reference);
                }
            }

            if (!list.isEmpty()) {
                list.setId("List/" + UUID.randomUUID().toString());
                list.setTitle(key.getRight());
                resources.put(list.getId(), list);
            }
        }
        report.setEvaluatedResource(evaluatedResourceIds);

        if (sdeAccumulators.size() > 0) {
            report = processAccumulators(report, sdeAccumulators, sde, isSingle, patients);
        }

        return report;
    }

    private void populateSDEAccumulators(Measure measure, Context context, Patient patient,HashMap<String, HashMap<String, Integer>> sdeAccumulators,
                                         List<Measure.MeasureSupplementalDataComponent> sde){
        context.setContextValue("Patient", patient.getIdElement().getIdPart());
        List<Object> sdeList = sde.stream().map(sdeItem -> context.resolveExpressionRef(sdeItem.getCriteria().getExpression()).evaluate(context)).collect(Collectors.toList());
        if(!sdeList.isEmpty()) {
            for (int i = 0; i < sdeList.size(); i++) {
                Object sdeListItem = sdeList.get(i);
                if(null != sdeListItem) {
                    String sdeAccumulatorKey = sde.get(i).getCode().getText();
                    if((null == sdeAccumulatorKey || sdeAccumulatorKey.length() < 1) && (null != sde.get(i).getCriteria() && null != sde.get(i).getCriteria().getExpression())){
                        sdeAccumulatorKey = sde.get(i).getCriteria().getExpression().toLowerCase(Locale.ROOT).replace(" ", "-");
                    }
                    HashMap<String, Integer> sdeItemMap = sdeAccumulators.get(sdeAccumulatorKey);
                    String code = "";

                    switch (sdeListItem.getClass().getSimpleName()) {
                        case "Code":
                            code = ((Code) sdeListItem).getCode();
                            break;
                        case "ArrayList":
                            if (((ArrayList<?>) sdeListItem).size() > 0) {
                                if (((ArrayList<?>) sdeListItem).get(0).getClass().getSimpleName().equals("Coding")) {
                                    code  = ((Coding) ((ArrayList<?>) sdeListItem).get(0)).getCode();
                                } else if (((ArrayList<?>) sdeListItem).get(0).getClass().getSimpleName().equals("Code")) {
                                    code = ((Code) ((ArrayList<?>) sdeListItem).get(0)).getCode();
                                } else {
                                    continue;
                                }
                            }else{
                                continue;
                            }
                            break;
                    }
                    if(null == code){
                        continue;
                    }
                    if (null != sdeItemMap && null != sdeItemMap.get(code)) {
                        Integer sdeItemValue = sdeItemMap.get(code);
                        sdeItemValue++;
                        sdeItemMap.put(code, sdeItemValue);
                        sdeAccumulators.get(sdeAccumulatorKey).put(code, sdeItemValue);
                    } else {
                        if (null == sdeAccumulators.get(sdeAccumulatorKey)) {
                            HashMap<String, Integer> newSDEItem = new HashMap<>();
                            newSDEItem.put(code, 1);
                            sdeAccumulators.put(sdeAccumulatorKey, newSDEItem);
                        } else {
                            sdeAccumulators.get(sdeAccumulatorKey).put(code, 1);
                        }
                    }
                }
            }
        }
    }

    private MeasureReport processAccumulators(MeasureReport report, HashMap<String, HashMap<String, Integer>> sdeAccumulators,
                                              List<Measure.MeasureSupplementalDataComponent> sde, boolean isSingle, List<Patient> patients){
        List<Reference> newRefList = new ArrayList<>();
        //sdeAccumulators.forEach((sdeKey, sdeAccumulator) -> {
        //sdeAccumulator.forEach((sdeAccumulatorKey, sdeAccumulatorValue)->{
        for (Map.Entry<String, HashMap<String, Integer>> sdeAccumulator : sdeAccumulators.entrySet()){
            for (Map.Entry<String, Integer> sdeAccumulatorValue : sdeAccumulator.getValue().entrySet()){
                Observation obs = new Observation();
                obs.setStatus(Observation.ObservationStatus.FINAL);
                obs.setId(UUID.randomUUID().toString());
                Coding valueCoding = new Coding();
                if (sdeAccumulator.getKey().equalsIgnoreCase("sde-sex")) {
                    valueCoding.setCode(sdeAccumulatorValue.getKey());
                } else if (sdeAccumulator.getKey().equalsIgnoreCase("sde-payer")) {
                    for (Patient pt : patients){
                        List<Coverage> coverages = getAllCoverages();
                        String patientId = pt.getId().split("/")[1];
                        if (null != coverages) {
                            Coverage coverage = coverages.stream().filter(coverageItem -> coverageItem.getBeneficiary().getReference().contains(patientId)).findFirst().orElse(null);
                            if (null != coverage) {
                                Coding coverageCoding = coverage.getType().getCoding().stream().filter(coding -> coding.getCode().contains(sdeAccumulatorValue.getKey())).findFirst().orElse(null);
                                if (null != coverageCoding) {
                                    valueCoding.setSystem(coverageCoding.getSystem());
                                    valueCoding.setCode(coverageCoding.getCode());
                                    valueCoding.setDisplay(coverageCoding.getDisplay());
                                }
                            }
                        }
                    }
                } else {
                    String coreCategory = sdeAccumulator.getKey().substring(sdeAccumulator.getKey().lastIndexOf('-') >= 0 ? sdeAccumulator.getKey().lastIndexOf('-') : 0);
                    for (Patient pt : patients){
                        for (Extension ptExt : pt.getExtension()){
                            if (ptExt.getUrl().contains(coreCategory)) {
                                String code = ((Coding) ptExt.getExtension().get(0).getValue()).getCode();
                                if(code.equalsIgnoreCase(sdeAccumulatorValue.getKey())) {
                                    valueCoding.setSystem(((Coding) ptExt.getExtension().get(0).getValue()).getSystem());
                                    valueCoding.setCode(code);
                                    valueCoding.setDisplay(((Coding) ptExt.getExtension().get(0).getValue()).getDisplay());
                                }
                            }
                        }
                    }
                }
                CodeableConcept obsCodeableConcept = new CodeableConcept();
                Extension obsExtension = new Extension().setUrl("http://hl7.org/fhir/StructureDefinition/cqf-measureInfo");
                Extension extExtMeasure = new Extension()
                        .setUrl("measure")
                        .setValue(new CanonicalType(report.getMeasure()));
                obsExtension.addExtension(extExtMeasure);
                Extension extExtPop = new Extension()
                        .setUrl("populationId")
                        .setValue(new StringType(sdeAccumulator.getKey()));
                obsExtension.addExtension(extExtPop);
                obs.addExtension(obsExtension);
                obs.setValue(new IntegerType(sdeAccumulatorValue.getValue()));
                if(!isSingle) {
                    valueCoding.setCode(sdeAccumulatorValue.getKey());
                    obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));
                    obs.setCode(obsCodeableConcept);
                }else{
                    obs.setCode(new CodeableConcept().setText(sdeAccumulator.getKey()));
                    obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));
                    obs.setValue(obsCodeableConcept);
                }
                newRefList.add(new Reference("#" + obs.getId()));
                report.addContained(obs);
            }
        }
        newRefList.addAll(report.getEvaluatedResource());
        report.setEvaluatedResource(newRefList);
        return report;
    }

    private void populateResourceMap(Context context, MeasurePopulationType type, HashMap<String, Resource> resources,
            HashMap<Pair<String, String>, HashSet<String>> codeToResourceMap) {
        if (context.getEvaluatedResources().isEmpty()) {
            return;
        }

        if (!codeToResourceMap.containsKey(Pair.of(type.toCode(), type.getDisplay()))) {
            codeToResourceMap.put(Pair.of(type.toCode(), type.getDisplay()), new HashSet<>());
        }

        HashSet<String> codeHashSet = codeToResourceMap.get((Pair.of(type.toCode(), type.getDisplay())));

        for (Object o : context.getEvaluatedResources()) {
            if (o instanceof Resource) {
                Resource r = (Resource) o;
                String id = (r.getIdElement().getResourceType() != null ? (r.getIdElement().getResourceType() + "/")
                        : "") + r.getIdElement().getIdPart();
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