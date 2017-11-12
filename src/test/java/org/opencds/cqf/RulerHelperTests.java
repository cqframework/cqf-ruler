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

    @Test
    public void XlsxToValueSetTest() throws IOException {
        String[] args = { "src/test/resources/org/opencds/cqf/test.xlsx", "-b=1", "-s=3", "-v=4", "-c=5", "-d=6"};
        Bundle bundle = XlsxToValueSet.convertVs(args);
        XlsxToValueSet.main(args);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaAffectedAreasArgs = { "src/test/resources/org/opencds/cqf/zika-affected-areas.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaAffectedAreasArgs);
        XlsxToValueSet.main(zikaAffectedAreasArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaSignsSymptomsArgs = { "src/test/resources/org/opencds/cqf/zika-virus-signs-symptoms.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaSignsSymptomsArgs);
        XlsxToValueSet.main(zikaSignsSymptomsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaArboSignsSymptomsArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-signs-symptoms.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaArboSignsSymptomsArgs);
        XlsxToValueSet.main(zikaArboSignsSymptomsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaVirusTestArgs = { "src/test/resources/org/opencds/cqf/zika-virus-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaVirusTestArgs);
        XlsxToValueSet.main(zikaVirusTestArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaArbovirusTestArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaArbovirusTestArgs);
        XlsxToValueSet.main(zikaArbovirusTestArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaChikungunyaTestsArgs = { "src/test/resources/org/opencds/cqf/zika-chikungunya-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaChikungunyaTestsArgs);
        XlsxToValueSet.main(zikaChikungunyaTestsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaDengueTestsArgs = { "src/test/resources/org/opencds/cqf/zika-dengue-tests.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaDengueTestsArgs);
        XlsxToValueSet.main(zikaDengueTestsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaIgmELISAResultsArgs = { "src/test/resources/org/opencds/cqf/zika-igm-elisa-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaIgmELISAResultsArgs);
        XlsxToValueSet.main(zikaIgmELISAResultsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaNeutralizingAntibodyResultsArgs = { "src/test/resources/org/opencds/cqf/zika-neutralizing-antibody-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaNeutralizingAntibodyResultsArgs);
        XlsxToValueSet.main(zikaNeutralizingAntibodyResultsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaArbovirusTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-arbovirus-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaArbovirusTestResultsArgs);
        XlsxToValueSet.main(zikaArbovirusTestResultsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaChikungunyaTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-chikungunya-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaChikungunyaTestResultsArgs);
        XlsxToValueSet.main(zikaChikungunyaTestResultsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] zikaDengueTestResultsArgs = { "src/test/resources/org/opencds/cqf/zika-dengue-test-results.xlsx", "-b=1", "-o=9", "-s=10", "-v=7", "-c=0", "-d=2" };
        bundle = XlsxToValueSet.convertVs(zikaDengueTestResultsArgs);
        XlsxToValueSet.main(zikaDengueTestResultsArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        String[] csArgs = { "src/test/resources/org/opencds/cqf/test.xlsx", "-b=1", "-o=8", "-u=7", "-v=4", "-c=5", "-d=6", "-cs", "-outDir=src/main/resources/codesystems/"};
        bundle = XlsxToValueSet.convertCs(csArgs);
        XlsxToValueSet.main(csArgs);
        Assert.assertTrue(!bundle.getEntry().isEmpty());

        // TODO - fix this
//        String[] csArgsZika = { "src/test/resources/org/opencds/cqf/zika-codesystem.xlsx", "-b=1", "-cs", "-outDir=src/main/resources/codesystems/"};
//        bundle = XlsxToValueSet.convertCs(csArgsZika);
//        XlsxToValueSet.main(csArgsZika);
//        Assert.assertTrue(!bundle.getEntry().isEmpty());
    }
}
