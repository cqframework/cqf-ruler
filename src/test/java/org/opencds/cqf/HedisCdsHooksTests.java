package org.opencds.cqf;

import org.junit.Assert;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

class HedisCdsHooksTests {

    private TestServer server;
    private final String hedisCdsLocation = "hedis-cds/";

    HedisCdsHooksTests(TestServer server) {
        this.server = server;
        this.server.putResource(hedisCdsLocation + "cds-valuesets.json", "");
    }

    void BCSCdsHooksPatientViewTest() throws IOException {
        server.putResource(hedisCdsLocation + "cds-bcs-bundle.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream(hedisCdsLocation + "cds-bcs-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + server.ourPort + "/cqf-ruler/dstu3/cds-services/bcs-decision-support");

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

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"A Mammogram procedure for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Mammogram procedure in the last 39 months\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Mammogram request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Mammogram procedure in the last 39 months\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://www.ama-assn.org/go/cpt\",\n" +
                "                      \"code\": \"77056\",\n" +
                "                      \"display\": \"Mammography; bilateral\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-66535\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    // Testing request without indicator specified
    void BCSCdsHooksPatientViewTestError() throws IOException {
        this.server.putResource(hedisCdsLocation + "cds-bcs-bundle-error.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream(hedisCdsLocation + "cds-bcs-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + this.server.ourPort + "/cqf-ruler/dstu3/cds-services/bcs-decision-support");

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

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"MissingRequiredFieldException encountered during execution\",\n" +
                "      \"indicator\": \"hard-stop\",\n" +
                "      \"detail\": \"The indicator field must be specified in the action.dynamicValue field in the PlanDefinition\",\n" +
                "      \"source\": {}\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Assert.assertTrue(
                response.toString().replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    void CCSCdsHooksPatientViewTest() throws IOException {
        server.putResource(hedisCdsLocation + "cds-ccs-bundle.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream(hedisCdsLocation + "cds-ccs-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + server.ourPort + "/cqf-ruler/dstu3/cds-services/ccs-decision-support");

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

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"A Cervical Cytology procedure for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Cervical Cytology procedure in the last 3 years\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Cervical Cytology request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Cervical Cytology procedure in the last 3 years\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://loinc.org\",\n" +
                "                      \"code\": \"33717-0\",\n" +
                "                      \"display\": \"Cytology Cervical or vaginal smear or scraping study\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-66532\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    void COLCdsHooksPatientViewTest() throws IOException {
        server.putResource(hedisCdsLocation + "cds-col-bundle.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream(hedisCdsLocation + "cds-col-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + server.ourPort + "/cqf-ruler/dstu3/cds-services/col-decision-support");

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

        String expected = "{\n" +
                "  \"cards\": [\n" +
                "    {\n" +
                "      \"summary\": \"A Fecal Occult Blood test for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Fecal Occult Blood test in the last year\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"FOBT request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Fecal Occult Blood test in the last year\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://www.ama-assn.org/go/cpt\",\n" +
                "                      \"code\": \"82272\",\n" +
                "                      \"display\": \"Fecal Occult Blood Test\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-2276\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"A Flexible Sigmoidoscopy procedure for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Flexible Sigmoidoscopy procedure in the last 5 years\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Flexible Sigmoidoscopy request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Flexible Sigmoidoscopy procedure in the last 5 years\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://www.ama-assn.org/go/cpt\",\n" +
                "                      \"code\": \"45530\",\n" +
                "                      \"display\": \"Sigmoidoscopy, flexible; diagnostic, including collection of specimen(s) by brushing or washing, when performed (separate procedure)\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-2276\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"A Colonoscopy procedure for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Colonoscopy procedure in the last 10 years\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"Colonoscopy request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Colonoscopy procedure in the last 10 years\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://hl7.org/fhir/sid/icd-9-cm\",\n" +
                "                      \"code\": \"45.23\",\n" +
                "                      \"display\": \"Colonoscopy\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-2276\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"A Colonography procedure for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a Colonography procedure in the last 5 years\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"CT Colonography request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a Colonography procedure in the last 5 years\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://www.ama-assn.org/go/cpt\",\n" +
                "                      \"code\": \"74261\",\n" +
                "                      \"display\": \"Computed tomographic (CT) colonography, diagnostic, including image postprocessing, without contract material\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-2276\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"A FIT-DNA test for the patient is recommended\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The patient has not had a FIT-DNA test in the last 3 years\",\n" +
                "      \"source\": {},\n" +
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"label\": \"FIT-DNA request\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
                "              \"description\": \"The patient has not had a FIT-DNA test in the last 3 years\",\n" +
                "              \"resource\": {\n" +
                "                \"resourceType\": \"ProcedureRequest\",\n" +
                "                \"status\": \"draft\",\n" +
                "                \"intent\": \"order\",\n" +
                "                \"code\": {\n" +
                "                  \"coding\": [\n" +
                "                    {\n" +
                "                      \"system\": \"http://www.ama-assn.org/go/cpt\",\n" +
                "                      \"code\": \"81528\",\n" +
                "                      \"display\": \"Oncology (colorectal) screening, quantitative real-time target and signal amplification of 10 DNA markers (KRAS mutations, promoter methylation of NDRG4 and BMP3) and fecal hemoglobin, utilizing stool, algorithm reported as a positive or negative result\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"subject\": {\n" +
                "                  \"reference\": \"Patient/Patient-2276\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }
}