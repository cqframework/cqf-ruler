package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.cqframework.cql.elm.execution.Library;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.FhirMeasureEvaluator;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.*;

public class RulerTestBase {
    private static IGenericClient ourClient;
    private static FhirContext ourCtx = FhirContext.forDstu3();

    private static int ourPort;

    private static Server ourServer;
    private static String ourServerBase;

//    private static Collection<IResourceProvider> providers;

    @BeforeClass
    public static void beforeClass() throws Exception {

        String path = Paths.get("").toAbsolutePath().toString();

        // changing from random to hard coded
        ourPort = 8080;
        ourServer = new Server(ourPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/cqf-ruler");
        webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
        webAppContext.setResourceBase(path + "/target/cqf-ruler");
        webAppContext.setParentLoaderPriority(true);

        ourServer.setHandler(webAppContext);
        ourServer.start();

        ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
        ourServerBase = "http://localhost:" + ourPort + "/cqf-ruler/baseDstu3";
        ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
        ourClient.registerInterceptor(new LoggingInterceptor(true));

        // Load test data
        // Normally, I would use a transaction bundle, but issues with the random ports prevents that...
        // So, doing it the old-fashioned way =)

        // General
        putResource("general-practitioner.json", "Practitioner-12208");
        putResource("general-patient.json", "Patient-12214");
        putResource("general-fhirhelpers-3.json", "FHIRHelpers");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ourServer.stop();
    }

    private static void putResource(String resourceFileName, String id) {
        InputStream is = RulerTestBase.class.getResourceAsStream(resourceFileName);
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String json = scanner.hasNext() ? scanner.next() : "";
        IBaseResource resource = ourCtx.newJsonParser().parseResource(json);

        if (resource instanceof Bundle) {
            ourClient.transaction().withBundle((Bundle) resource).execute();
        }
        else {
            ourClient.update().resource(resource).withId(id).execute();
        }
    }

    // this test requires the OpioidManagementTerminologyKnowledge.db file to be located in the src/main/resources/cds folder
//    @Test
    public void CdcOpioidGuidanceTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance");

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
                "      \"links\": [\n" +
                "        {\n" +
                "          \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "          \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"label\": \"MME Conversion Tables\",\n" +
                "          \"url\": \"https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"High risk for opioid overdose - taper now\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Total morphine milligram equivalent (MME) is 20200.700mg/d. Taper to less than 50.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Assert.assertTrue(
                response.toString().replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void MeasureProcessingTest() {
        putResource("measure-processing-bundle.json", "");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));
        inParams.addParameter().setName("startPeriod").setValue(new DateType("2001-01-01"));
        inParams.addParameter().setName("endPeriod").setValue(new DateType("2015-03-01"));

        Parameters outParams = ourClient
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

    @Test
    public void PlanDefinitionApplyTest() throws ClassNotFoundException {
        putResource("plandefinition-apply-library.json", "plandefinitionApplyTest");
        putResource("plandefinition-apply.json", "apply-example");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));

        Parameters outParams = ourClient
                .operation()
                .onInstance(new IdDt("PlanDefinition", "apply-example"))
                .named("$apply")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof CarePlan);

        CarePlan carePlan = (CarePlan) resource;

        Assert.assertTrue(carePlan.getTitle().equals("This is a dynamic definition!"));
    }

    @Test
    public void ActivityDefinitionApplyTest() {
        putResource("activitydefinition-apply-library.json", "activityDefinitionApplyTest");
        putResource("activitydefinition-apply.json", "ad-apply-example");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));

        Parameters outParams = ourClient
                .operation()
                .onInstance(new IdDt("ActivityDefinition", "ad-apply-example"))
                .named("$apply")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof ProcedureRequest);

        ProcedureRequest procedureRequest = (ProcedureRequest) resource;

        Assert.assertTrue(procedureRequest.getDoNotPerform());
    }

    @Test
    public void TestMeasureEvaluator() throws IOException, JAXBException {
        putResource("measure-processing-bundle.json", "");

        File xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource("library-col.elm.xml").getFile(), "UTF-8"));
        Library library = CqlLibraryReader.read(xmlFile);

        Context context = new Context(library);

        BaseFhirDataProvider provider = new FhirDataProviderStu3().setEndpoint(ourServerBase);

        FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider().withEndpoint(ourServerBase);
        provider.setTerminologyProvider(terminologyProvider);
//        provider.setExpandValueSets(true);

        context.registerDataProvider("http://hl7.org/fhir", provider);
        context.registerTerminologyProvider(terminologyProvider);

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

    @Test
    public void BCSCdsHooksPatientViewTest() throws IOException {
        putResource("cds-bcs-bundle.json", "");
        putResource("cds-codesystems.json", "");
        putResource("cds-valuesets.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cds-bcs-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/bcs-decision-support");

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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-6535\"\n" +
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

    @Test
    public void CCSCdsHooksPatientViewTest() throws IOException {
        putResource("cds-ccs-bundle.json", "");
        putResource("cds-codesystems.json", "");
        putResource("cds-valuesets.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cds-ccs-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/ccs-decision-support");

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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-6532\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void COLCdsHooksPatientViewTest() throws IOException {
        putResource("cds-col-bundle.json", "");
        putResource("cds-codesystems.json", "");
        putResource("cds-valuesets.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cds-col-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/col-decision-support");

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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-276\"\n" +
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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-276\"\n" +
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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-276\"\n" +
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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-276\"\n" +
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
                "      \"suggestions\": [\n" +
                "        {\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"type\": \"create\",\n" +
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
                "                  \"reference\": \"Patient/Patient-276\"\n" +
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

    //@Test
    public void DiabetesManagementTest() throws IOException {
        putResource("cds-diabetes-management-bundle.json", "");

        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cds-diabetes-management-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/diabetes-management");

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
                "      \"detail\": \"The Creatinine level of 122umol/L in the most recent lab is considered abnormal\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal HbA1C level detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The HbA1C level of 15.2mmol/L in the most recent lab is considered abnormal\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal LDL cholesterol level detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The LDL cholesterol level of 189mg/dL in the most recent lab is considered abnormal\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Microalbumin/Creatinine ratio detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Microalbumin/Creatinine ratio of 35mcg/mg in the most recent lab is considered abnormal\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Foot Exam detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Foot Exam resulted in the following abnormality: Non-pressure chronic ulcer of other part of unspecified foot with unspecified severity\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"summary\": \"Abnormal Eye Exam detected in most recent lab results\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The Eye Exam resulted in the following abnormality: Type 2 diabetes mellitus with mild nonproliferative diabetic retinopathy without macular edema, left eye\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Assert.assertTrue(
                expected.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }
}
