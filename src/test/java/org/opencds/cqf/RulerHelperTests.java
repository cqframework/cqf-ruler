package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.FhirMeasureEvaluator;
import org.opencds.cqf.helpers.XlsxToValueSet;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.*;

public class RulerHelperTests {

    // These tests were used for hedis terminology that we do not have permission to share yet.
    // Uncomment and add spreadsheets when permission is given, otherwise construct new tests.
    @Test
    public void XlsxToValueSetTest() throws IOException {
//        String[] args = { "src/test/resources/org/opencds/cqf/test.xlsx", "-b=1", "-s=3", "-v=4", "-c=5", "-d=6"};
//        Bundle bundle = XlsxToValueSet.convertVs(args);
//        XlsxToValueSet.main(args);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] csArgs = { "src/test/resources/org/opencds/cqf/test.xlsx", "-b=1", "-o=8", "-u=7", "-v=4", "-c=5", "-d=6", "-cs", "-outDir=src/main/resources/codesystems/"};
//        bundle = XlsxToValueSet.convertCs(csArgs);
//        XlsxToValueSet.main(csArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaAffectedAreasArgs = { "src/test/resources/org/opencds/cqf/zika-affected-areas.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaAffectedAreasArgs);
//        XlsxToValueSet.main(zikaAffectedAreasArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaSignsSymptomsArgs = { "src/test/resources/org/opencds/cqf/zika-virus-signs-symptoms.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaSignsSymptomsArgs);
//        XlsxToValueSet.main(zikaSignsSymptomsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaArboSignsSymptomsArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-signs-symptoms.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaArboSignsSymptomsArgs);
//        XlsxToValueSet.main(zikaArboSignsSymptomsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaVirusTestArgs = { "src/test/resources/org/opencds/cqf/zika-virus-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaVirusTestArgs);
//        XlsxToValueSet.main(zikaVirusTestArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaArbovirusTestArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaArbovirusTestArgs);
//        XlsxToValueSet.main(zikaArbovirusTestArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaChikungunyaTestsArgs = { "src/test/resources/org/opencds/cqf/zika-chikungunya-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaChikungunyaTestsArgs);
//        XlsxToValueSet.main(zikaChikungunyaTestsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaDengueTestsArgs = { "src/test/resources/org/opencds/cqf/zika-dengue-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaDengueTestsArgs);
//        XlsxToValueSet.main(zikaDengueTestsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaIgmELISAResultsArgs = { "src/test/resources/org/opencds/cqf/zika-igm-elisa-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaIgmELISAResultsArgs);
//        XlsxToValueSet.main(zikaIgmELISAResultsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaNeutralizingAntibodyResultsArgs = { "src/test/resources/org/opencds/cqf/zika-neutralizing-antibody-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaNeutralizingAntibodyResultsArgs);
//        XlsxToValueSet.main(zikaNeutralizingAntibodyResultsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaArbovirusTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaArbovirusTestResultsArgs);
//        XlsxToValueSet.main(zikaArbovirusTestResultsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaChikungunyaTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-chikungunya-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaChikungunyaTestResultsArgs);
//        XlsxToValueSet.main(zikaChikungunyaTestResultsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//
//        String[] zikaDengueTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-dengue-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
//        bundle = XlsxToValueSet.convertVs(zikaDengueTestResultsArgs);
//        XlsxToValueSet.main(zikaDengueTestResultsArgs);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] csArgs = { "src/test/resources/org/opencds/cqf/zika-codesystem.xlsx", "-b=1", "-cs", "-outDir=src/main/resources/codesystems/"};
        Bundle bundle = XlsxToValueSet.convertCs(csArgs);
        XlsxToValueSet.main(csArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());
    }

    @Test
    public void TestCOL() throws IOException, JAXBException {
        InputStream is = this.getClass().getResourceAsStream("library-col.elm.xml");
        File xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource("library-col.elm.xml").getFile(), "UTF-8"));
        Library library = CqlLibraryReader.read(xmlFile);

        Context context = new Context(library);

//        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint("http://fhirtest.uhn.ca/baseDstu3");
//        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint("http://fhir3.healthintersections.com.au/open");
//        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint("http://wildfhir.aegis.net/fhir");
//        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint("http://open-api2.hspconsortium.org/payerextract/data");
        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");

        FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider().withEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");
//        FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider().withBasicAuth("brhodes", "apelon123!").withEndpoint("http://fhir.ext.apelon.com/dtsserverws/fhir");
        provider.setTerminologyProvider(terminologyProvider);
//        provider.setExpandValueSets(true);

        context.registerDataProvider("http://hl7.org/fhir", provider);

        xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource("measure-col.xml").getFile(), "UTF-8"));
        Measure measure = provider.getFhirClient().getFhirContext().newXmlParser().parseResource(Measure.class, new FileReader(xmlFile));

        String patientId = "Patient-12214";
        Patient patient = provider.getFhirClient().read().resource(Patient.class).withId(patientId).execute();
        // TODO: Couldn't figure out what matcher to use here, gave up.
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

        System.out.println(String.format("Bundle url: %s", report.getEvaluatedResources().getReference()));
    }
}
