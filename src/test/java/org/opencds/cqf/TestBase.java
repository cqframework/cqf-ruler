package org.opencds.cqf;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class TestBase {

    private static TestServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server = new TestServer();
        server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void runMeasureEvaluationTests() throws IOException, JAXBException {
        MeasureEvaluationTests measureTests = new MeasureEvaluationTests(server);
        measureTests.patientMeasureCCS_PatientNotInInitialPopulation();
        measureTests.patientListMeasureCCS();
        measureTests.populationMeasureBCS();
        measureTests.populationMeasureCCS();
        measureTests.populationMeasureCOL();
    }

    @Test
    public void runOpioidGuidanceTests() throws Exception {
        CdcOpioidGuidanceTests opioidTests = new CdcOpioidGuidanceTests(server);
        opioidTests.CdcOpioidGuidanceRecommendationFourTest_LongActingOpioid();
        opioidTests.CdcOpioidGuidanceRecommendationFourTest_NewPatient();
        opioidTests.CdcOpioidGuidanceRecommendationFourTest_NotLongActingOpioid();
        opioidTests.CdcOpioidGuidanceRecommendationFourTest_OpioidWithAbusePotential();

        opioidTests.CdcOpioidGuidanceRecommendationFiveTest_MMEGreaterThanFifty();
        opioidTests.CdcOpioidGuidanceRecommendationFiveTest_MMELessThanFifty();

        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_EndOfLifeExclusion();
        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_RiskAssessment();
        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_SevenOfPastTenDays();
        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_SixOfPastTenDays();
        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_SixtyThreeOfPastNinetyDays();
        opioidTests.CdcOpioidGuidanceRecommendationSevenTest_SixtyTwoOfPastNinetyDays();

        opioidTests.CdcOpioidGuidanceRecommendationEightTest_MMEGreaterThanFifty();
        opioidTests.CdcOpioidGuidanceRecommendationEightTest_MMELessThanFifty();
        opioidTests.CdcOpioidGuidanceRecommendationEightTest_OnBenzodiazepine();
        opioidTests.CdcOpioidGuidanceRecommendationEightTest_OnNaloxone();
        opioidTests.CdcOpioidGuidanceRecommendationEightTest_HistoryOfSubstanceAbuse();

        opioidTests.CdcOpioidGuidanceRecommendationTenTest_EndOfLifeExclusion();
        opioidTests.CdcOpioidGuidanceRecommendationTenTest_IllicitDrugs();
        opioidTests.CdcOpioidGuidanceRecommendationTenTest_MissingPrescribedOpioids();
        opioidTests.CdcOpioidGuidanceRecommendationTenTest_NoScreenings();
        opioidTests.CdcOpioidGuidanceRecommendationTenTest_NotMissingPrescribedOpioids();
        opioidTests.CdcOpioidGuidanceRecommendationTenTest_UnprescribedOpioidsAndMissingPrescribedOpioids();

        opioidTests.CdcOpioidGuidanceRecommendationElevenTest_BenzoTriggerWithOpioid();
        opioidTests.CdcOpioidGuidanceRecommendationElevenTest_BenzoTriggerWithoutOpioid();
        opioidTests.CdcOpioidGuidanceRecommendationElevenTest_OpioidTriggerWithBenzo();
        opioidTests.CdcOpioidGuidanceRecommendationElevenTest_OpioidTriggerWithoutBenzo();
    }

    @Test
    public void runHedisCdsTests() throws IOException {
        HedisCdsHooksTests hedisTests = new HedisCdsHooksTests(server);
        hedisTests.BCSCdsHooksPatientViewTest();
        hedisTests.BCSCdsHooksPatientViewTestError();
        hedisTests.CCSCdsHooksPatientViewTest();
        hedisTests.COLCdsHooksPatientViewTest();
    }

    @Test
    public void runDefinitionApplyTests() throws ClassNotFoundException {
        DefinitionApplyTests applyTests = new DefinitionApplyTests(server);
        applyTests.PlanDefinitionApplyTest();
        applyTests.ActivityDefinitionApplyTest();
    }
}
