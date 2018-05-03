package org.opencds.cqf;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.*;
import org.junit.Assert;
import java.util.List;

class MeasureEvaluationTests {

    private TestServer server;
    private final String measureEvalLocation = "measure-evaluation/";

    MeasureEvaluationTests(TestServer server) {
        this.server = server;

        this.server.putResource("general-practitioner.json", "Practitioner-12208");
        this.server.putResource("general-patient.json", "Patient-12214");
        this.server.putResource(measureEvalLocation + "hedis-terminology-bundle.json", "");
        this.server.putResource(measureEvalLocation + "hedis-measure-network.json", "");
        this.server.putResource(measureEvalLocation + "hedis-patients.json", "");
        this.server.putResource(measureEvalLocation + "hedis-resources-bundle.json", "");
        server.putResource(measureEvalLocation + "hedis-bcs-bundle.json", "");
        server.putResource(measureEvalLocation + "hedis-ccs-bundle.json", "");
        server.putResource(measureEvalLocation + "hedis-col-bundle.json", "");
    }

    void patientMeasureCCS_PatientNotInInitialPopulation() {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient/Patient-12214"));
        inParams.addParameter().setName("periodStart").setValue(new DateType("2017-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("2017-12-31"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-ccs"))
                .named("$evaluate-measure")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof MeasureReport);

        MeasureReport report = (MeasureReport) component.getResource();

        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                Assert.assertTrue(population.getCount() == 0);
            }
        }
    }

    void patientListMeasureCCS() {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("reportType").setValue(new StringType("patient-list"));
        inParams.addParameter().setName("practitioner").setValue(new StringType("Practitioner/Practitioner-2520"));
        inParams.addParameter().setName("periodStart").setValue(new DateType("1997-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("1997-12-31"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-ccs"))
                .named("$evaluate-measure")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof MeasureReport);

        MeasureReport report = (MeasureReport) component.getResource();

        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                if (population.getCode().getCodingFirstRep().getCode().equals("initial-population")) {
                    Assert.assertTrue(population.getCount() == 1);
                }
            }
        }
    }

    private void validatePopulationMeasure(String startPeriod, String endPeriod, String measureId)
    {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("reportType").setValue(new StringType("population"));
        inParams.addParameter().setName("periodStart").setValue(new DateType(startPeriod));
        inParams.addParameter().setName("periodEnd").setValue(new DateType(endPeriod));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", measureId))
                .named("$evaluate-measure")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof MeasureReport);

        MeasureReport report = (MeasureReport) component.getResource();

        Assert.assertTrue(report.getEvaluatedResources() != null);

        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            for (MeasureReport.MeasureReportGroupPopulationComponent pop : group.getPopulation()) {
                if (pop.getCode().getCodingFirstRep().getCode().equals("initial-population")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }

                if (pop.getCode().getCodingFirstRep().getCode().equals("numerator")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }

                if (pop.getCode().getCodingFirstRep().getCode().equals("denominator")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }
            }
        }
    }

    void populationMeasureBCS() {
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-bcs");
    }

    void populationMeasureCCS() {
        validatePopulationMeasure("2017-01-01", "2017-12-31", "measure-ccs");
    }

    void populationMeasureCOL() {
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-col");
    }
}
