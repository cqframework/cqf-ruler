package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.*;
import org.junit.Assert;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.helpers.FhirMeasureEvaluator;
import org.opencds.cqf.providers.JpaTerminologyProvider;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

class MeasureEvaluationTests {

    private TestServer server;
    private final String measureEvalLocation = "measure-evaluation/";

    MeasureEvaluationTests(TestServer server) {
        this.server = server;

        this.server.putResource("general-practitioner.json", "Practitioner-12208");
        this.server.putResource("general-patient.json", "Patient-12214");
        this.server.putResource(measureEvalLocation + "population-measure-terminology-bundle.json", "");
        this.server.putResource(measureEvalLocation + "population-measure-network.json", "");
        this.server.putResource(measureEvalLocation + "population-measure-patients.json", "");
        this.server.putResource(measureEvalLocation + "population-measure-resources-bundle.json", "");
    }

    void TestMeasureEvaluator() throws IOException, JAXBException {
        server.putResource(measureEvalLocation + "measure-processing-bundle.json", "");

        File xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource(measureEvalLocation + "library-col.elm.xml").getFile(), "UTF-8"));
        Library library = CqlLibraryReader.read(xmlFile);

        Context context = new Context(library);

        JpaTerminologyProvider jpaTermSvc = new JpaTerminologyProvider(
                (ValueSetResourceProvider) server.dataProvider.resolveResourceProvider("ValueSet"),
                (CodeSystemResourceProvider) server.dataProvider.resolveResourceProvider("CodeSystem")
        );
        server. dataProvider.setTerminologyProvider(jpaTermSvc);
//        provider.setExpandValueSets(true);

        context.registerDataProvider("http://hl7.org/fhir", server.dataProvider);
        context.registerTerminologyProvider(jpaTermSvc);

        xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource(measureEvalLocation + "measure-col.xml").getFile(), "UTF-8"));
        Measure measure = FhirContext.forDstu3().newXmlParser().parseResource(Measure.class, new FileReader(xmlFile));

        String patientId = "Patient-12214";
        Patient patient = (Patient) server.dataProvider.resolveResourceProvider("Patient").getDao().read(new IdType(patientId));

        if (patient == null) {
            throw new RuntimeException("Patient is null");
        }

        context.setContextValue("Patient", patientId);

        FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();

        // Java's date support is _so_ bad.
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(2014, Calendar.JANUARY, 1, 0, 0, 0);
        Date periodStart = cal.getTime();
        cal.set(2014, Calendar.DECEMBER, 31, 11, 59, 59);
        Date periodEnd = cal.getTime();

        org.hl7.fhir.dstu3.model.MeasureReport report = evaluator.evaluate(context, measure, patient, periodStart, periodEnd);

        if (report == null) {
            throw new RuntimeException("MeasureReport is null");
        }

        if (report.getEvaluatedResources() == null) {
            throw new RuntimeException("EvaluatedResources is null");
        }

        // System.out.println(String.format("Bundle url: %s", report.getEvaluatedResources().getReference()));
    }

    void MeasureEvaluationTest() {
        server.putResource(measureEvalLocation + "measure-processing-bundle.json", "");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));
        inParams.addParameter().setName("startPeriod").setValue(new DateType("2001-01-01"));
        inParams.addParameter().setName("endPeriod").setValue(new DateType("2015-03-01"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", "col"))
                .named("$evaluate")
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
            if (group.getIdentifier().getValue().equals("history-of-colorectal-cancer")) {
                Assert.assertTrue(group.getPopulation().get(0).getCount() > 0);
            }

            if (group.getIdentifier().getValue().equals("history-of-total-colectomy")) {
                Assert.assertTrue(group.getPopulation().get(0).getCount() > 0);
            }
        }
    }

    private void validatePopulationMeasure(String startPeriod, String endPeriod, String measureId, String primaryLibraryName) {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("reportType").setValue(new StringType("population"));
        inParams.addParameter().setName("startPeriod").setValue(new DateType(startPeriod));
        inParams.addParameter().setName("endPeriod").setValue(new DateType(endPeriod));
        inParams.addParameter().setName("primaryLibraryName").setValue(new StringType(primaryLibraryName));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("Measure", measureId))
                .named("$evaluate")
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
        server.putResource(measureEvalLocation + "population-measure-bcs-bundle.json", "");
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-bcs", "library-bcs-logic");
    }

    void populationMeasureCCS() {
        server.putResource(measureEvalLocation + "population-measure-ccs-bundle.json", "");
        validatePopulationMeasure("2017-01-01", "2017-12-31", "measure-ccs", "library-ccs-logic");
    }

    void populationMeasureCOL() {
        server.putResource(measureEvalLocation + "population-measure-col-bundle.json", "");
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-col", "library-col-logic");
    }
}
