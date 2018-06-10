package org.opencds.cqf;

import org.hl7.fhir.dstu3.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.opencds.cqf.helpers.XlsxToValueSet;

import java.io.*;

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

//    @Test
//    public void converterTest() throws FHIRException {
//        NullVersionConverterAdvisor30 advisor = new NullVersionConverterAdvisor30();
//        VersionConvertor_10_30 converter = new VersionConvertor_10_30(advisor);
//
//        InputStream is = RulerHelperTests.class.getResourceAsStream("Dstu2Observation.json");
//        Observation dstu2Obs = (Observation) FhirContext.forDstu2Hl7Org().newJsonParser().parseResource(new InputStreamReader(is));
//
//        org.hl7.fhir.dstu3.model.Observation dstu3Obs = converter.convertObservation(dstu2Obs);
//        String s = "";
//    }

//    @Test
//    public void validationTest() {
//        Condition condition = new Condition();
//        condition.setId("condition-1");
//        condition.setCode(
//                new CodeableConcept().addCoding(
//                        new Coding().setSystem("ICD-9-CM").setCode("428.21")
//                )
//        )
//                .setClinicalStatus(Condition.ConditionClinicalStatus.ACTIVE)
//                .setVerificationStatus(Condition.ConditionVerificationStatus.CONFIRMED)
//                .setSubject(new Reference().setReference("Patient/pneumo-true-2"))
//                .setOnset(new DateTimeType().setValue(new Date()));
//
//        Bundle bundle = new Bundle();
//        bundle.setType(Bundle.BundleType.TRANSACTION);
//        bundle.addEntry(
//                new Bundle.BundleEntryComponent().setResource(condition)
//                        .setRequest(new Bundle.BundleEntryRequestComponent()
//                                .setMethod(Bundle.HTTPVerb.PUT)
//                                .setUrl("Condition/condition-1"))
//        );
//
//        FhirContext ctx = FhirContext.forDstu3();
//        FhirValidator validator = ctx.newValidator();
//        IValidatorModule module1 = new SchemaBaseValidator(ctx);
//        validator.registerValidatorModule(module1);
//        ValidationResult result = validator.validateWithResult(bundle);
//
//        if (result.isSuccessful()) {
//            System.out.println("Validation Successful");
//        }
//        else {
//            System.out.println("Validation failed");
//        }
//
//        List<SingleValidationMessage> messages = result.getMessages();
//        for (SingleValidationMessage next : messages) {
//            System.out.println("Message:");
//            System.out.println(" * Location: " + next.getLocationString());
//            System.out.println(" * Severity: " + next.getSeverity());
//            System.out.println(" * Message : " + next.getMessage());
//        }
//    }
}
