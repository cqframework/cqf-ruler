package org.opencds.cqf;

import org.junit.Assert;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

class CdcOpioidGuidanceTests {

    private TestServer server;
    private final String recFourLocation = "cdc-opioid-recommendations/recommendation4/";
    private final String recFiveLocation = "cdc-opioid-recommendations/recommendation5/";
    private final String recSevenLocation = "cdc-opioid-recommendations/recommendation7/";
    private final String recEightLocation = "cdc-opioid-recommendations/recommendation8/";
    private final String recTenLocation = "cdc-opioid-recommendations/recommendation10/";
    private final String recElevenLocation = "cdc-opioid-recommendations/recommendation11/";

    CdcOpioidGuidanceTests(TestServer server) {
        this.server = server;
        server.putResource("cdc-opioid-recommendations/terminology/valuesets.xml", "");
        server.putResource("cdc-opioid-recommendations/library-omtk-data-0.0.0.xml", "OMTKData");
        server.putResource("cdc-opioid-recommendations/library-omtk-logic-0.0.1.xml", "OMTKLogic");
        server.putResource("cdc-opioid-recommendations/library-opioidcds-common.xml", "OpioidCDSCommonSTU3");

        // Recommendation 4 artifacts
        server.putResource(recFourLocation + "library-opioidcds-recommendation-04.xml", "opioidcds-recommendation-04");
        server.putResource(recFourLocation + "plandefinition-opioidcds-04.xml", "cdc-opioid-guidance-04");

        // Recommendation 5 artifacts
        server.putResource(recFiveLocation + "library-opioidcds-recommendation-05.xml", "opioidcds-recommendation-05");
        server.putResource(recFiveLocation + "plandefinition-opioidcds-05.xml", "cdc-opioid-guidance-05");

        // Recommendation 7 artifacts
        server.putResource(recSevenLocation + "library-opioidcds-recommendation-07.xml", "opioidcds-recommendation-07");
        server.putResource(recSevenLocation + "plandefinition-opioidcds-07.xml", "cdc-opioid-guidance-07");
        server.putResource(recSevenLocation + "activitydefinition-opioidcds-risk-assessment-request.xml", "opioidcds-risk-assessment-request");

        // Recommendation 8 artifacts
        server.putResource(recEightLocation + "library-opioidcds-recommendation-08.xml", "opioidcds-recommendation-08");
        server.putResource(recEightLocation + "plandefinition-opioidcds-08.xml", "cdc-opioid-guidance-08");

        // Recommendation 10 artifacts
        server.putResource(recTenLocation + "library-opioidcds-recommendation-10.xml", "opioidcds-recommendation-10");
        server.putResource(recTenLocation + "plandefinition-opioidcds-10.xml", "cdc-opioid-guidance-10");

        // Recommendation 11 artifacts
        server.putResource(recElevenLocation + "library-opioidcds-recommendation-11.xml", "opioidcds-recommendation-11");
        server.putResource(recElevenLocation + "plandefinition-opioidcds-11.xml", "cdc-opioid-guidance-11");
    }

    private String makeRequest(String responsePath, String hookName) throws IOException {
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream(responsePath);
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + server.ourPort + "/cqf-ruler/dstu3/cds-services/" + hookName);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(data);

        StringBuilder response = new StringBuilder();
        try(Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")))
        {
            for (int i; (i = in.read()) >= 0;) {
                response.append((char) i);
            }
        }

        return response.toString();
    }

    void CdcOpioidGuidanceRecommendationFourTest_LongActingOpioid() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-long-acting-opioid.xml", "example-rec-04-long-acting-opioid");
        server.putResource(recFourLocation + "encounter-example-rec-04-long-acting-opioid-context.xml", "example-rec-04-long-acting-opioid-context");

        String response = makeRequest(recFourLocation + "request-example-rec-04-long-acting-opioid.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Recommend use of immediate-release opioids instead of extended release/long acting opioids when starting patient on opioids.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following medication requests(s) release rates should be re-evaluated: 12 HR Oxycodone Hydrochloride 10 MG Extended Release Oral Tablet\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFourTest_LongActingOpioid_NoPrefetch() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-long-acting-opioid-no-prefetch.xml", "example-rec-04-long-acting-opioid-no-prefetch");
        server.putResource(recFourLocation + "encounter-example-rec-04-long-acting-opioid-context-no-prefetch.xml", "example-rec-04-long-acting-opioid-context-no-prefetch");
        server.putResource(recFourLocation + "encounter-example-rec-04-long-acting-opioid-no-prefetch.json", "example-rec-04-long-acting-opioid-no-prefetch");
        server.putResource(recFourLocation + "medicationrequest-example-rec-04-long-acting-opioid-no-prefetch.json", "example-rec-04-long-acting-opioid-no-prefetch");

        String response = makeRequest(recFourLocation + "request-example-rec-04-long-acting-opioid-no-prefetch.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Recommend use of immediate-release opioids instead of extended release/long acting opioids when starting patient on opioids.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following medication requests(s) release rates should be re-evaluated: 12 HR Oxycodone Hydrochloride 10 MG Extended Release Oral Tablet\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFourTest_LongActingOpioid_PartialPrefetch() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-long-acting-opioid-partial-prefetch.xml", "example-rec-04-long-acting-opioid-partial-prefetch");
        server.putResource(recFourLocation + "encounter-example-rec-04-long-acting-opioid-context-partial-prefetch.xml", "example-rec-04-long-acting-opioid-context-partial-prefetch");
        server.putResource(recFourLocation + "encounter-example-rec-04-long-acting-opioid-partial-prefetch.json", "example-rec-04-long-acting-opioid-partial-prefetch");
        server.putResource(recFourLocation + "medicationrequest-example-rec-04-long-acting-opioid-partial-prefetch.json", "example-rec-04-long-acting-opioid-partial-prefetch");

        String response = makeRequest(recFourLocation + "request-example-rec-04-long-acting-opioid-partial-prefetch.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Recommend use of immediate-release opioids instead of extended release/long acting opioids when starting patient on opioids.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following medication requests(s) release rates should be re-evaluated: 12 HR Oxycodone Hydrochloride 10 MG Extended Release Oral Tablet\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFourTest_NewPatient() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-new-patient.xml", "example-rec-04-new-patient");
        server.putResource(recFourLocation + "encounter-example-rec-04-new-patient-context.xml", "example-rec-04-new-patient-context");

        String response = makeRequest(recFourLocation + "request-example-rec-04-new-patient.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFourTest_NotLongActingOpioid() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-not-long-acting-opioid.xml", "example-rec-04-not-long-acting-opioid");
        server.putResource(recFourLocation + "encounter-example-rec-04-not-long-acting-opioid-context.xml", "example-rec-04-not-long-acting-opioid-context");

        String response = makeRequest(recFourLocation + "request-example-rec-04-not-long-acting-opioid.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFourTest_OpioidWithAbusePotential() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFourLocation + "patient-example-rec-04-opioid-with-abuse-potential.xml", "example-rec-04-opioid-with-abuse-potential");
        server.putResource(recFourLocation + "encounter-example-rec-04-opioid-with-abuse-potential-context.xml", "example-rec-04-opioid-with-abuse-potential-context");

        String response = makeRequest(recFourLocation + "request-example-rec-04-opioid-with-abuse-potential.json", "cdc-opioid-guidance-04");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFiveTest_MMEGreaterThanFifty() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFiveLocation + "patient-example-rec-05-mme-greater-than-fifty.xml", "example-rec-05-mme-greater-than-fifty");
        server.putResource(recFiveLocation + "encounter-example-rec-05-mme-greater-than-fifty-context.xml", "example-rec-05-mme-greater-than-fifty-context");

        String response = makeRequest(recFiveLocation + "request-example-rec-05-mme-greater-than-fifty.json", "cdc-opioid-guidance-05");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"High risk for opioid overdose - taper now\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Total morphine milligram equivalent (MME) is 179.99999820 \\u0027mg/d\\u0027. Taper to less than 50.\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"label\": \"MME Conversion Tables\",\n" +
                "          \"url\": \"https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationFiveTest_MMELessThanFifty() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recFiveLocation + "patient-example-rec-05-mme-less-than-fifty.xml", "example-rec-05-mme-less-than-fifty");
        server.putResource(recFiveLocation + "encounter-example-rec-05-mme-less-than-fifty-context.xml", "example-rec-05-mme-less-than-fifty-context");

        String response = makeRequest(recFiveLocation + "request-example-rec-05-mme-less-than-fifty.json", "cdc-opioid-guidance-05");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_EndOfLifeExclusion() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-end-of-life-exclusion.xml", "example-rec-07-end-of-life-exclusion");
        server.putResource(recSevenLocation + "encounter-example-rec-07-end-of-life-exclusion-context.xml", "example-rec-07-end-of-life-exclusion-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-end-of-life-exclusion.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_RiskAssessment() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-risk-assessment.xml", "example-rec-07-risk-assessment");
        server.putResource(recSevenLocation + "encounter-example-rec-07-risk-assessment-context.xml", "example-rec-07-risk-assessment-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-risk-assessment.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_SevenOfPastTenDays() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-seven-of-past-ten-days.xml", "example-rec-07-seven-of-past-ten-days");
        server.putResource(recSevenLocation + "encounter-example-rec-07-seven-of-past-ten-days-context.xml", "example-rec-07-seven-of-past-ten-days-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-seven-of-past-ten-days.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Patients on opioid therapy should be evaluated for benefits and harms within 1 to 4 weeks of starting opioid therapy and every 3 months or more subsequently.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"No evaluation for benefits and harms has been performed for the patient starting opioid therapy\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Assessment of risk for opioid use procedure\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"No evaluation for benefits and harms has been performed for the patient starting opioid therapy\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://snomed.info/sct\",\n" +
                "                      \"code\": \"454281000124100\",\n" +
                "                      \"display\": \"Assessment of risk for opioid abuse (procedure)\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/example-rec-07-seven-of-past-ten-days\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        String withoutID = response.replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(withoutID.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_SixOfPastTenDays() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-six-of-past-ten-days.xml", "example-rec-07-six-of-past-ten-days");
        server.putResource(recSevenLocation + "encounter-example-rec-07-six-of-past-ten-days-context.xml", "example-rec-07-six-of-past-ten-days-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-six-of-past-ten-days.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_SixtyThreeOfPastNinetyDays() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-sixtythree-of-past-ninety-days.xml", "example-rec-07-sixtythree-of-past-ninety-days");
        server.putResource(recSevenLocation + "encounter-example-rec-07-sixtythree-of-past-ninety-days-context.xml", "example-rec-07-sixtythree-of-past-ninety-days-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-sixtythree-of-past-ninety-days.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Patients on opioid therapy should be evaluated for benefits and harms within 1 to 4 weeks of starting opioid therapy and every 3 months or more subsequently.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"No evaluation for benefits and harms associated with opioid therapy has been performed for the patient in the past 3 months\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Assessment of risk for opioid use procedure\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"No evaluation for benefits and harms associated with opioid therapy has been performed for the patient in the past 3 months\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://snomed.info/sct\",\n" +
                "                      \"code\": \"454281000124100\",\n" +
                "                      \"display\": \"Assessment of risk for opioid abuse (procedure)\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/example-rec-07-sixtythree-of-past-ninety-days\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        String withoutID = response.replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(withoutID.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationSevenTest_SixtyTwoOfPastNinetyDays() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recSevenLocation + "patient-example-rec-07-sixtytwo-of-past-ninety-days.xml", "example-rec-07-sixtytwo-of-past-ninety-days");
        server.putResource(recSevenLocation + "encounter-example-rec-07-sixtytwo-of-past-ninety-days-context.xml", "example-rec-07-sixtytwo-of-past-ninety-days-context");

        String response = makeRequest(recSevenLocation + "request-example-rec-07-sixtytwo-of-past-ninety-days.json", "cdc-opioid-guidance-07");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationEightTest_MMEGreaterThanFifty() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recEightLocation + "patient-example-rec-08-mme-greater-than-fifty.xml", "example-rec-08-mme-greater-than-fifty");
        server.putResource(recEightLocation + "encounter-example-rec-08-mme-greater-than-fifty-context.xml", "example-rec-08-mme-greater-than-fifty-context");

        String response = makeRequest(recEightLocation + "request-example-rec-08-mme-greater-than-fifty.json", "cdc-opioid-guidance-08");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Incorporate into the management plan strategies to mitigate risk; including considering offering naloxone when factors that increase risk for opioid overdose are present\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Consider offering naloxone given following risk factor(s) for opioid overdose: Average MME (54.000000 \\u0027mg/d\\u0027) \\u003e\\u003d 50 mg/day, \",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"label\": \"MME Conversion Tables\",\n" +
                "          \"url\": \"https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationEightTest_MMELessThanFifty() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recEightLocation + "patient-example-rec-08-mme-less-than-fifty.xml", "example-rec-08-mme-less-than-fifty");
        server.putResource(recEightLocation + "encounter-example-rec-08-mme-less-than-fifty-context.xml", "example-rec-08-mme-less-than-fifty-context");

        String response = makeRequest(recEightLocation + "request-example-rec-08-mme-less-than-fifty.json", "cdc-opioid-guidance-08");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationEightTest_OnBenzodiazepine() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recEightLocation + "patient-example-rec-08-on-benzodiazepine.xml", "example-rec-08-on-benzodiazepine");
        server.putResource(recEightLocation + "encounter-example-rec-08-on-benzodiazepine-context.xml", "example-rec-08-on-benzodiazepine-context");

        String response = makeRequest(recEightLocation + "request-example-rec-08-on-benzodiazepine.json", "cdc-opioid-guidance-08");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Incorporate into the management plan strategies to mitigate risk; including considering offering naloxone when factors that increase risk for opioid overdose are present\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Consider offering naloxone given following risk factor(s) for opioid overdose: concurrent use of benzodiazepine, \",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"label\": \"MME Conversion Tables\",\n" +
                "          \"url\": \"https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationEightTest_OnNaloxone() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recEightLocation + "patient-example-rec-08-on-naloxone.xml", "example-rec-08-on-naloxone");
        server.putResource(recEightLocation + "encounter-example-rec-08-on-naloxone-context.xml", "example-rec-08-on-naloxone-context");

        String response = makeRequest(recEightLocation + "request-example-rec-08-on-naloxone.json", "cdc-opioid-guidance-08");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationEightTest_HistoryOfSubstanceAbuse() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recEightLocation + "patient-example-rec-08-substance-abuse.xml", "example-rec-08-substance-abuse");
        server.putResource(recEightLocation + "encounter-example-rec-08-substance-abuse-context.xml", "example-rec-08-substance-abuse-context");

        String response = makeRequest(recEightLocation + "request-example-rec-08-substance-abuse.json", "cdc-opioid-guidance-08");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Incorporate into the management plan strategies to mitigate risk; including considering offering naloxone when factors that increase risk for opioid overdose are present\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Consider offering naloxone given following risk factor(s) for opioid overdose: history of alcohol or drug abuse.\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      },\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"label\": \"MME Conversion Tables\",\n" +
                "          \"url\": \"https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_EndOfLifeExclusion() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-end-of-life-med-exclusion.xml", "example-rec-10-end-of-life-med-exclusion");
        server.putResource(recTenLocation + "encounter-example-rec-10-end-of-life-med-exclusion-context.xml", "example-rec-10-end-of-life-med-exclusion-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-end-of-life-med-exclusion.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_IllicitDrugs() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-illicit-drugs.xml", "example-rec-10-illicit-drugs");
        server.putResource(recTenLocation + "encounter-example-rec-10-illicit-drugs-context.xml", "example-rec-10-illicit-drugs-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-illicit-drugs.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Illicit Drugs Found In Urine Screening\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Found the following illicit drug(s) in urine drug screen: Tetrahydrocannabinol\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_MissingPrescribedOpioids() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-missing-prescribed-opioids.xml", "example-rec-10-missing-prescribed-opioids");
        server.putResource(recTenLocation + "encounter-example-rec-10-missing-prescribed-opioids-context.xml", "example-rec-10-missing-prescribed-opioids-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-missing-prescribed-opioids.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Prescribed Opioids Not Found In Urine Screening\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following opioids are missing from the screening: fentanyl\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_NoScreenings() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-no-screenings.xml", "example-rec-10-no-screenings");
        server.putResource(recTenLocation + "encounter-example-rec-10-no-screenings-context.xml", "example-rec-10-no-screenings-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-no-screenings.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Annual Urine Screening Check\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Patient has not had a urine screening in the past 12 months\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_NotMissingPrescribedOpioids() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-not-missing-prescribed-opioids.xml", "example-rec-10-not-missing-prescribed-opioids");
        server.putResource(recTenLocation + "encounter-example-rec-10-not-missing-prescribed-opioids-context.xml", "example-rec-10-not-missing-prescribed-opioids-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-not-missing-prescribed-opioids.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationTenTest_UnprescribedOpioidsAndMissingPrescribedOpioids() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recTenLocation + "patient-example-rec-10-unprescribed-opioids.xml", "example-rec-10-unprescribed-opioids");
        server.putResource(recTenLocation + "encounter-example-rec-10-unprescribed-opioids-context.xml", "example-rec-10-unprescribed-opioids-context");

        String response = makeRequest(recTenLocation + "request-example-rec-10-unprescribed-opioids.json", "cdc-opioid-guidance-10");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Unprescribed Opioids Found In Urine Screening\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Found the following unprescribed opioid(s): codeine\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Prescribed Opioids Not Found In Urine Screening\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following opioids are missing from the screening: oxycodone\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationElevenTest_BenzoTriggerWithOpioid() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recElevenLocation + "patient-example-rec-11-benzo-trigger-with-opioid.xml", "example-rec-11-benzo-trigger-with-opioid");
        server.putResource(recElevenLocation + "encounter-example-rec-11-benzo-trigger-with-opioid-context.xml", "example-rec-11-benzo-trigger-with-opioid-context");

        String response = makeRequest(recElevenLocation + "request-example-rec-11-benzo-trigger-with-opioid.json", "cdc-opioid-guidance-11");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Avoid prescribing opioid pain medication and benzodiazepine concurrently whenever possible.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The benzodiazepine prescription request is concurrent with an active opioid prescription\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationElevenTest_BenzoTriggerWithoutOpioid() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recElevenLocation + "patient-example-rec-11-benzo-trigger-without-opioid.xml", "example-rec-11-benzo-trigger-without-opioid");
        server.putResource(recElevenLocation + "encounter-example-rec-11-benzo-trigger-without-opioid-context.xml", "example-rec-11-benzo-trigger-without-opioid-context");

        String response = makeRequest(recElevenLocation + "request-example-rec-11-benzo-trigger-without-opioid.json", "cdc-opioid-guidance-11");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationElevenTest_OpioidTriggerWithBenzo() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recElevenLocation + "patient-example-rec-11-opioid-trigger-with-benzo.xml", "example-rec-11-opioid-trigger-with-benzo");
        server.putResource(recElevenLocation + "encounter-example-rec-11-opioid-trigger-with-benzo-context.xml", "example-rec-11-opioid-trigger-with-benzo-context");

        String response = makeRequest(recElevenLocation + "request-example-rec-11-opioid-trigger-with-benzo.json", "cdc-opioid-guidance-11");

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"Avoid prescribing opioid pain medication and benzodiazepine concurrently whenever possible.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The opioid prescription request is concurrent with an active benzodiazepine prescription\",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }

    void CdcOpioidGuidanceRecommendationElevenTest_OpioidTriggerWithoutBenzo() throws IOException {

        // load necessary resources and artifacts
        server.putResource(recElevenLocation + "patient-example-rec-11-opioid-trigger-without-benzo.xml", "example-rec-11-opioid-trigger-without-benzo");
        server.putResource(recElevenLocation + "encounter-example-rec-11-opioid-trigger-without-benzo-context.xml", "example-rec-11-opioid-trigger-without-benzo-context");

        String response = makeRequest(recElevenLocation + "request-example-rec-11-opioid-trigger-without-benzo.json", "cdc-opioid-guidance-11");

        String expected = "{\n" +
                "  \"cards\": []\n" +
                "}\n";

        Assert.assertTrue(response.replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", "")));
    }
}
