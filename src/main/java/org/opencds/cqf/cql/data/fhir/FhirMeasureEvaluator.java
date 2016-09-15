package org.opencds.cqf.cql.data.fhir;

import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.IGenericClient;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Bryn on 5/7/2016.
 */
public class FhirMeasureEvaluator {
    public MeasureReport evaluate(IGenericClient fhirClient, Context context, Measure measure, Patient patient, Date periodStart, Date periodEnd) {
        MeasureReport report = new MeasureReport();
        report.setMeasure(new Reference(measure));
        report.setPatient(new Reference(patient));
        Period reportPeriod = new Period();
        reportPeriod.setStart(periodStart);
        reportPeriod.setEnd(periodEnd);
        report.setPeriod(reportPeriod);
        report.setType(MeasureReport.MeasureReportType.INDIVIDUAL);

        Interval measurementPeriod = new Interval(DateTime.fromJavaDate(periodStart), true, DateTime.fromJavaDate(periodEnd), true);
        context.setParameter(null, "MeasurementPeriod", measurementPeriod);

        // for each measure group
        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            MeasureReport.MeasureReportGroupComponent reportGroup = new MeasureReport.MeasureReportGroupComponent();
            reportGroup.setIdentifier(group.getIdentifier().copy()); // TODO: Do I need to do this copy? Will HAPI FHIR do this automatically?
            report.getGroup().add(reportGroup);
            for (Measure.MeasureGroupPopulationComponent population : group.getPopulation()) {
                // evaluate the criteria expression, should return true/false, translate to 0/1 for report
                Boolean result = (Boolean)context.resolveExpressionRef(population.getCriteria()).evaluate(context);
                MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
                populationReport.setCount(result != null && result ? 1 : 0);
                populationReport.setType(population.getType().toCode()); // TODO: It's not clear why these properties are represented differently in the HAPI client, they're the same type in the FHIR spec...
                reportGroup.getPopulation().add(populationReport);
            }
        }

        ArrayList<String> expressionNames = new ArrayList<String>();
        // HACK: Hijacking Supplemental data to specify the evaluated resources
        // In reality, this should be specified explicitly, but I'm not sure what else to do here....
        for (Measure.MeasureSupplementalDataComponent supplementalData : measure.getSupplementalData()) {
            expressionNames.add(supplementalData.getCriteria());
        }

        FhirMeasureBundler bundler = new FhirMeasureBundler();
        String[] expressionNameArray = new String[expressionNames.size()];
        expressionNameArray = expressionNames.toArray(expressionNameArray);
        org.hl7.fhir.dstu3.model.Bundle evaluatedResources = bundler.Bundle(context, expressionNameArray);
        String jsonString = fhirClient.getFhirContext().newJsonParser().encodeResourceToString(evaluatedResources);
        ca.uhn.fhir.rest.api.MethodOutcome result = fhirClient.create().resource(evaluatedResources).execute();
        report.setEvaluatedResources(new Reference(result.getId()));

        //report.setEvaluatedResources(new Reference(bundleId));
        return report;
    }
}
