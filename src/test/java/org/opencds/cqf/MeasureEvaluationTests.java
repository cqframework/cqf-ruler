package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.*;
import org.junit.Assert;

import java.io.InputStream;
import java.io.InputStreamReader;
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
        this.server.putResource(measureEvalLocation + "hedis-asf-bundle.json", "");
        this.server.putResource(measureEvalLocation + "hedis-bcs-bundle.json", "");
        this.server.putResource(measureEvalLocation + "hedis-ccs-bundle.json", "");
        this.server.putResource(measureEvalLocation + "hedis-col-bundle.json", "");
        this.server.putResource(measureEvalLocation + "hedis-dms-bundle.json", "");
    }

    void patientMeasureASF_IIP_AllNumerator_AllDenominator_True() {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient/Patient-6529"));
        inParams.addParameter().setName("periodStart").setValue(new DateType("2003-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("2003-12-31"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-asf"))
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
                Assert.assertTrue(population.getCount() > 0);
            }
        }
    }

    void patientMeasureDMS_IIP_Numerator1_Denominator1_True() {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient/Patient-6498"));
        inParams.addParameter().setName("periodStart").setValue(new DateType("2017-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("2017-12-31"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-dms"))
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
                if (population.getIdentifier().getValue().equals("initial-population")) {
                    Assert.assertTrue(population.getCount() > 0);
                }
                else if (population.getIdentifier().getValue().equals("numerator 1")) {
                    Assert.assertTrue(population.getCount() > 0);
                }
                else if (population.getIdentifier().getValue().equals("denominator 1")) {
                    Assert.assertTrue(population.getCount() > 0);
                }
            }
        }
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

    void bundleSourceDataMeasure_COL() {
        InputStream is = MeasureEvaluationTests.class.getResourceAsStream("measure-evaluation/col-source-data-bundle.json");
        Bundle bundle = (Bundle) FhirContext.forDstu3().newJsonParser().parseResource(new InputStreamReader(is));

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("periodStart").setValue(new DateType("2014-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("2014-12-31"));
        inParams.addParameter().setName("sourceData").setResource(bundle);

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-col"))
                .named("$evaluate-measure-with-source")
                .withParameters(inParams)
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof MeasureReport);

        MeasureReport report = (MeasureReport) component.getResource();

        for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
            for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                Assert.assertTrue(population.getCount() == 1);
            }
        }
    }

    void careGapTestBCS() {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient/Patient-6523"));
        inParams.addParameter().setName("topic").setValue(new StringType("Preventive Care and Screening"));
        inParams.addParameter().setName("periodStart").setValue(new DateType("1997-01-01"));
        inParams.addParameter().setName("periodEnd").setValue(new DateType("1997-12-31"));

        Parameters outParams = server.ourClient
                .operation()
                .onType(Measure.class)
                .named("$care-gaps")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof Bundle);

        Bundle bundle = (Bundle) component.getResource();

        Assert.assertTrue(bundle.hasEntry() && bundle.getEntry().size() == 2);
    }

    // TODO - Need a test that is expected to fail and tests with multiple resource params
    void submitDataTest_NonTransaction() {
        runSubmitDataOperationWithBundle("asf-submit-data-bundle.json");
    }

    void submitDataTest_Transaction() {
        runSubmitDataOperationWithBundle("asf-submit-data-transaction-bundle.json");
    }

    private void runSubmitDataOperationWithBundle(String bundleName) {
        InputStream is = MeasureEvaluationTests.class.getResourceAsStream(measureEvalLocation + "asf-measure-report.json");
        MeasureReport report = (MeasureReport) server.dataProvider.getFhirContext().newJsonParser().parseResource(new InputStreamReader(is));
        is = MeasureEvaluationTests.class.getResourceAsStream(measureEvalLocation + bundleName);
        Bundle bundle = (Bundle) server.dataProvider.getFhirContext().newJsonParser().parseResource(new InputStreamReader(is));

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("measure-report").setResource(report);
        inParams.addParameter().setName("resource").setResource(bundle);

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "measure-asf"))
                .named("$submit-data")
                .withParameters(inParams)
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof Bundle);

        Bundle transactionResponse = (Bundle) component.getResource();

        Assert.assertTrue(transactionResponse.hasType() && transactionResponse.getType() == Bundle.BundleType.TRANSACTIONRESPONSE);
    }
}
