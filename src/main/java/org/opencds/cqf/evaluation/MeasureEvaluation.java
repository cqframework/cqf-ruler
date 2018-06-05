package org.opencds.cqf.evaluation;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
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

        List<Patient> patients = new ArrayList<>();
        if (practitionerRef != null) {
            SearchParameterMap map = new SearchParameterMap();
            map.add(
                    "general-practitioner",
                    new ReferenceParam(
                            practitionerRef.startsWith("Practitioner/")
                                    ? practitionerRef
                                    : "Practitioner/" + practitionerRef
                    )
            );

            if (provider instanceof JpaDataProvider) {
                IBundleProvider patientProvider = ((JpaDataProvider) provider).resolveResourceProvider("Patient").getDao().search(map);
                List<IBaseResource> patientList = patientProvider.getResources(0, patientProvider.size());
                patientList.forEach(x -> patients.add((Patient) x));
            }
        }
        return evaluate(measure, context, patients, MeasureReport.MeasureReportType.PATIENTLIST);
    }

    public MeasureReport evaluatePopulationMeasure(Measure measure, Context context) {
        logger.info("Generating summary report");

        List<Patient> patients = new ArrayList<>();
        if (provider instanceof JpaDataProvider) {
            IBundleProvider patientProvider = ((JpaDataProvider) provider).resolveResourceProvider("Patient").getDao().search(new SearchParameterMap());
            List<IBaseResource> patientList = patientProvider.getResources(0, patientProvider.size());
            patientList.forEach(x -> patients.add((Patient) x));
        }
        return evaluate(measure, context, patients, MeasureReport.MeasureReportType.SUMMARY);
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

        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            MeasureReport.MeasureReportGroupComponent reportGroup = new MeasureReport.MeasureReportGroupComponent();
            reportGroup.setIdentifier(group.getIdentifier());
            report.getGroup().add(reportGroup);

            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                int count = 0;
                // Worried about performance here with big populations...
                for (Patient patient : initialPopulation) {
                    context.setContextValue("Patient", patient.getIdElement().getIdPart());
                    Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
                    if (result instanceof Boolean) {
                        count += (Boolean) result ? 1 : 0;
                    }
                    else if (result instanceof Iterable) {
                        for (Object item : (Iterable) result) {
                            count++;
                            if (item instanceof Resource) {
                                resources.put(((Resource) item).getId(), (Resource) item);
                            }
                        }
                    }
                }
                MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
                populationReport.setCount(count);
                populationReport.setCode(pop.getCode());
                populationReport.setIdentifier(pop.getIdentifier());
                reportGroup.getPopulation().add(populationReport);
            }
        }

        FhirMeasureBundler bundler = new FhirMeasureBundler();
        if (resources.isEmpty()) {
            for (Object element : context.getEvaluatedResourcesList()) {
                if (element instanceof Resource) {
                    resources.put(((Resource) element).getId(), (Resource) element);
                }
            }
        }
        org.hl7.fhir.dstu3.model.Bundle evaluatedResources = bundler.bundle(resources.values());
        evaluatedResources.setId(UUID.randomUUID().toString());
        report.setEvaluatedResources(new Reference('#' + evaluatedResources.getId()));
        report.addContained(evaluatedResources);
        return report;
    }

    private List<Patient> getInitalPopulation(Measure measure, List<Patient> population, Context context) {
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
