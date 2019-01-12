package org.opencds.cqf;

import org.junit.Assert;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

class DiabetesManagementTests {

    private TestServer server;
    private final String diabetesManagementLocation = "diabetes-management/";

    DiabetesManagementTests(TestServer server) {
        this.server = server;
        this.server.putResource(diabetesManagementLocation + "cds-diabetes-management-bundle.json", "");
    }

    void diabetesManagementTest() throws IOException {
        InputStream is = this.getClass().getResourceAsStream(diabetesManagementLocation + "cds-diabetes-management-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + server.ourPort + "/cqf-ruler/dstu3/cds-services/diabetes-management");

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
                "      \"summary\": \"Abnormal Creatinine level detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Creatinine level of 122 \\u0027umol/L\\u0027 in the most recent lab is considered abnormal\",\n" +
                "      \"source\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal HbA1C level detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The HbA1C level of 15.2 \\u0027mmol/L\\u0027 in the most recent lab is considered abnormal\",\n" +
                "      \"source\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal LDL cholesterol level detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The LDL cholesterol level of 189 \\u0027mg/dL\\u0027 in the most recent lab is considered abnormal\",\n" +
                "      \"source\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Microalbumin/Creatinine ratio detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Microalbumin/Creatinine ratio of 35 \\u0027mcg/mg\\u0027 in the most recent lab is considered abnormal\",\n" +
                "      \"source\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Foot Exam detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Foot Exam resulted in the following abnormality: Non-pressure chronic ulcer of other part of unspecified foot with unspecified severity\",\n" +
                "      \"source\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Eye Exam detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Eye Exam resulted in the following abnormality: Type 2 diabetes mellitus with mild nonproliferative diabetic retinopathy without macular edema, left eye\",\n" +
                "      \"source\": {}\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(
                response.toString().replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }
}
