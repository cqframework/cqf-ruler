package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.Assert;
import org.junit.Test;
import org.opencds.cqf.helpers.XlsxToValueSet;

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

}
