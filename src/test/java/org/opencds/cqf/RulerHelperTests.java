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
import java.util.Calendar;
import java.util.Date;

public class RulerHelperTests {

    // These tests were used for hedis terminology that we do not have permission to share yet.
    // Uncomment and add spreadsheets when permission is given, otherwise construct new tests.
//    @Test
//    public void XlsxToValueSetTest() {
//        Bundle bundle = XlsxToValueSet.convertVs("src/test/resources/org/opencds/cqf/test_asf.xlsx");
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//        printBundleEntry(bundle);
//
//        bundle = XlsxToValueSet.convertVs("src/test/resources/org/opencds/cqf/test_dms-drr.xlsx");
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//        printBundleEntry(bundle);
//
//        bundle = XlsxToValueSet.convertVs("src/test/resources/org/opencds/cqf/test_dsf.xlsx");
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//        printBundleEntry(bundle);
//
//        bundle = XlsxToValueSet.convertVs("src/test/resources/org/opencds/cqf/test_pvc.xlsx");
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//        printBundleEntry(bundle);
//
//        bundle = XlsxToValueSet.convertCs("src/test/resources/org/opencds/cqf/test_all.xlsx");
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
//        printBundleEntry(bundle);
//    }
//
//    private void printBundleEntry(Bundle bundle) {
//        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//            System.out.println(FhirContext.forDstu3().newJsonParser().encodeResourceToString(entry.getResource()));
//        }
//    }

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
