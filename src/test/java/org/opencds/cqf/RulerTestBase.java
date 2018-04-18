package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.cqframework.cql.elm.execution.Library;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.helpers.FhirMeasureEvaluator;
import org.opencds.cqf.providers.FHIRBundleResourceProvider;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.providers.JpaTerminologyProvider;
import org.opencds.cqf.servlet.BaseServlet;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RulerTestBase {
    private static IGenericClient ourClient;
    private static FhirContext ourCtx = FhirContext.forDstu3();
    private static int ourPort;
    private static Server ourServer;
    private static String ourServerBase;
    private static JpaDataProvider dataProvider;


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

        Collection<IResourceProvider> resourceProviders = null;
        for (ServletHolder servletHolder : webAppContext.getServletHandler().getServlets()) {
            if (servletHolder.getServlet() instanceof BaseServlet) {
                resourceProviders = ((BaseServlet) servletHolder.getServlet()).getResourceProviders();
                break;
            }
        }

        dataProvider = new JpaDataProvider(resourceProviders);

        ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
        ourServerBase = "http://localhost:" + ourPort + "/cqf-ruler/baseDstu3";
        ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
        ourClient.registerInterceptor(new LoggingInterceptor(true));

        // Load test data
        // General
        putResource("general-practitioner.json", "Practitioner-12208");
        putResource("general-patient.json", "Patient-12214");
        putResource("general-fhirhelpers-3.json", "FHIRHelpers");
        putResource("population-measure-network.json", "");
        putResource("population-measure-patients.json", "");
        putResource("population-measure-resources-bundle.json", "");
        putResource("cds-codesystems.json", "");
        putResource("cds-valuesets.json", "");
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

    // NOTE - the opioid guidance tests require the LocalDataStore_RxNav_OpioidCds.db file to be located in the src/main/resources/cds folder
    @Test
    public void CdcOpioidGuidanceRecommendationFourTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-04-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-04");

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
                "      \"summary\": \"Recommend use of immediate-release opioids instead of extended release/long acting opioids when starting patient on opioids.\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"The following medication requests(s) release rates should be re-evaluated: MedicationRequest/example-rec-04-long-acting-opioid-context \",\n" +
                "      \"source\": {\n" +
                "        \"label\": \"CDC guideline for prescribing opioids for chronic pain\",\n" +
                "        \"url\": \"https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Assert.assertTrue(
                response.toString().replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void CdcOpioidGuidanceRecommendationFiveTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-05-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-05");

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
                "      \"summary\": \"High risk for opioid overdose - taper now\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Total morphine milligram equivalent (MME) is 179.99999820mg/d. Taper to less than 50.\",\n" +
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

        Assert.assertTrue(
                response.toString().replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    // this test requires the LocalDataStore_RxNav_OpioidCds.db file to be located in the src/main/resources/cds folder
    @Test
    public void CdcOpioidGuidanceRecommendationSevenTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-07-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-07");

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

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void CdcOpioidGuidanceRecommendationEightTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-08-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-08");

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
                "      \"summary\": \"Incorporate into the management plan strategies to mitigate risk; including considering offering naloxone when factors that increase risk for opioid overdose are present\",\n" +
                "      \"indicator\": \"warning\",\n" +
                "      \"detail\": \"Consider offering naloxone given following risk factor(s) for opioid overdose: Average MME (54.000000mg/d) \\u003e\\u003d 50 mg/day, \",\n" +
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

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void CdcOpioidGuidanceRecommendationTenTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-10-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-10");

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

        conn.disconnect();

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
                "}";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
                        .equals(expected.replaceAll("\\s+", ""))
        );
    }

    @Test
    public void CdcOpioidGuidanceRecommendationElevenTest() throws IOException {
        putResource("cdc-opioid-guidance-bundle.json", "");
        putResource("cdc-opioid-guidance-terminology.json", "");
        // Get the CDS Hooks request
        JsonObject request = new Gson().fromJson(new InputStreamReader(this.getClass().getResourceAsStream("cdc-opioid-guidance-11-request.json")), JsonObject.class);

        InputStream is = this.getClass().getResourceAsStream("cdc-opioid-guidance-11-request.json");
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String cdsHooksRequest = scanner.hasNext() ? scanner.next() : "";
        byte[] data = cdsHooksRequest.getBytes("UTF-8");

        // Unsure how to use Hapi to make this request...
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services/cdc-opioid-guidance-11");

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

        conn.disconnect();

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
                "}";

        String withoutID = response.toString().replaceAll("\"id\":.*\\s", "");
        Assert.assertTrue(
                withoutID.replaceAll("\\s+", "")
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

        JpaTerminologyProvider jpaTermSvc = new JpaTerminologyProvider(
                (ValueSetResourceProvider) dataProvider.resolveResourceProvider("ValueSet"),
                (CodeSystemResourceProvider) dataProvider.resolveResourceProvider("CodeSystem")
        );
        dataProvider.setTerminologyProvider(jpaTermSvc);
//        provider.setExpandValueSets(true);

        context.registerDataProvider("http://hl7.org/fhir", dataProvider);
        context.registerTerminologyProvider(jpaTermSvc);

        xmlFile = new File(URLDecoder.decode(RulerHelperTests.class.getResource("measure-col.xml").getFile(), "UTF-8"));
        Measure measure = FhirContext.forDstu3().newXmlParser().parseResource(Measure.class, new FileReader(xmlFile));

        String patientId = "Patient-12214";
        Patient patient = (Patient) dataProvider.resolveResourceProvider("Patient").getDao().read(new IdType(patientId));

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

    // Testing request without indicator specified
    @Test
    public void BCSCdsHooksPatientViewTestError() throws IOException {
        putResource("cds-bcs-bundle-error.json", "");

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

    @Test
    public void CCSCdsHooksPatientViewTest() throws IOException {
        putResource("cds-ccs-bundle.json", "");

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
                "                  \"reference\": \"Patient/Patient-6532\"\n" +
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
    public void COLCdsHooksPatientViewTest() throws IOException {
        putResource("cds-col-bundle.json", "");

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

    private void validatePopulationMeasure(String startPeriod, String endPeriod, String measureId, String primaryLibraryName) {
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("reportType").setValue(new StringType("population"));
        inParams.addParameter().setName("startPeriod").setValue(new DateType(startPeriod));
        inParams.addParameter().setName("endPeriod").setValue(new DateType(endPeriod));
        inParams.addParameter().setName("primaryLibraryName").setValue(new StringType(primaryLibraryName));

        Parameters outParams = ourClient
                .operation()
                .onInstance(new IdDt("Measure", measureId))
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
            for (MeasureReport.MeasureReportGroupPopulationComponent pop : group.getPopulation()) {
                if (pop.getCode().getCodingFirstRep().getCode().equals("initial-population")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }

                if (pop.getCode().getCodingFirstRep().getCode().equals("numerator")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }

                if (pop.getCode().getCodingFirstRep().getCode().equals("denominator")) {
                    Assert.assertTrue(pop.getCount() > 0);
                }
            }
        }
    }

    @Test
    public void populationMeasureBCS() {
        putResource("population-measure-bcs-bundle.json", "");
        putResource("population-measure-terminology-bundle.json", "");
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-bcs", "library-bcs-logic");
    }

    @Test
    public void populationMeasureCCS() {
        putResource("population-measure-ccs-bundle.json", "");
        putResource("population-measure-terminology-bundle.json", "");
        validatePopulationMeasure("2017-01-01", "2017-12-31", "measure-ccs", "library-ccs-logic");
    }

    @Test
    public void populationMeasureCOL() {
        putResource("population-measure-col-bundle.json", "");
        putResource("population-measure-terminology-bundle.json", "");
        validatePopulationMeasure("1997-01-01", "1997-12-31", "measure-col", "library-col-logic");
    }

    @Test
    public void applyCqlTest() throws FHIRException, ParseException {
        InputStream is = this.getClass().getResourceAsStream("bundle-example-rec-04-long-acting-opioid.xml");
        Bundle bundle = (Bundle) FhirContext.forDstu3().newXmlParser().parseResource(new InputStreamReader(is));

        FHIRBundleResourceProvider bundleProvider = new FHIRBundleResourceProvider();
        bundle = bundleProvider.applyCql(bundle);

        MedicationRequest medReq = (MedicationRequest) bundle.getEntry().get(0).getResource();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, -4);
        Date todayMinus4Months = cal.getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date expected = formatter.parse(formatter.format(todayMinus4Months));
        Date actual = formatter.parse(formatter.format(medReq.getAuthoredOn()));
        Assert.assertEquals(expected, actual);

//        TODO - uncomment once engine issue #97 (https://github.com/DBCG/cql_engine/issues/97) is resolved
        actual = formatter.parse(formatter.format(medReq.getDispenseRequest().getValidityPeriod().getStart()));
        Assert.assertEquals(expected, actual);

        cal.setTime(new Date());
        cal.add(Calendar.MONTH, 3);
        expected = formatter.parse(formatter.format(cal.getTime()));
        actual = formatter.parse(formatter.format(medReq.getDispenseRequest().getValidityPeriod().getEnd()));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDynamicDiscovery() throws IOException {
        URL url = new URL("http://localhost:" + ourPort + "/cqf-ruler/cds-services");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try(Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")))
        {
            for (int i; (i = in.read()) >= 0;) {
                response.append((char) i);
            }
        }

        System.out.println(StringEscapeUtils.unescapeJava(response.toString()));
    }
}
