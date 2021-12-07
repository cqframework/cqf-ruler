package org.opencds.cqf.ruler.plugin.cpg.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cpg.CpgConfig;
import org.opencds.cqf.ruler.plugin.cpg.IServerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.IRestfulServerDefaults;
import ca.uhn.fhir.rest.server.RestfulServer;
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CpgConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3", "hapi.fhir.cpg.enabled=true" })
public class CqlExecutionProviderIT implements IServerSupport {

        @Autowired
        CqlExecutionProvider cqlExecutionProvider;

        @Autowired
        private FhirContext ourCtx;

        @Autowired
        private DaoRegistry myDaoRegistry;
    
        @LocalServerPort
        private int port;

        @Test
        public void testCqlExecutionProviderTranslationErrors() throws Exception {    
                RequestDetails theRequest = Mockito.mock(RequestDetails.class);
                theRequest.setFhirServerBase(getLocalServer());
                BufferedReader reader = new BufferedReader(new InputStreamReader(CqlExecutionProviderIT.class.getResourceAsStream("cql/EXM104_FHIR3-8.1.000.cql")));
                String cqlCode = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                // Patient First
                uploadTests("test/denom-EXM104-FHIR3/Patient");
                Map<String, IBaseResource> resources = uploadTests("test/denom-EXM104-FHIR3");
                IBaseResource patient = resources.get("denom-EXM104-FHIR3");
                Bundle resultingBundle = cqlExecutionProvider.evaluate(theRequest, cqlCode, patient.getIdElement().getIdPart(), "2019-01-01", "2020-01-01", null, getLocalServer(), null, null, "Patient", null);
                for (BundleEntryComponent entry : resultingBundle.getEntry()) {//.forEach(entry -> {
                        assertTrue(entry.getResource().getId().toLowerCase().equals("error"));
                } //);
                System.out.println("Made it here.");
        }

        @Test
        public void testCqlExecutionProviderMultipleResultsInParameters() throws Exception {    
                RequestDetails theRequest = Mockito.mock(RequestDetails.class);
                theRequest.setFhirServerBase(getLocalServer());
                BufferedReader reader = new BufferedReader(new InputStreamReader(CqlExecutionProviderIT.class.getResourceAsStream("cql/SimpleArithmetic.cql")));
                String cqlCode = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                Bundle resultingBundle = cqlExecutionProvider.evaluate(theRequest, cqlCode, null, null, null, null, null, null, null, null, null);
                assertTrue(resultingBundle.getEntry().size() == 5);
                Parameters param = (Parameters) resultingBundle.getEntry().get(0).getResource();
                assertTrue(param.getIdElement().getIdPart().equals("Sum of 1 and 5"));
                // assertTrue(validateValueParameter(param, new StringType("6")));
                Parameters param2 = (Parameters) resultingBundle.getEntry().get(1).getResource();
                assertTrue(param2.getIdElement().getIdPart().equals("String Value"));
                // assertTrue(validateValueParameter(param, new StringType("This is a String Value")));
                Parameters param3 = (Parameters) resultingBundle.getEntry().get(2).getResource();
                assertTrue(param3.getIdElement().getIdPart().equals("5"));
                // assertTrue(validateValueParameter(param, new StringType("5")));
                Parameters param4 = (Parameters) resultingBundle.getEntry().get(3).getResource();
                assertTrue(param4.getIdElement().getIdPart().equals("Six"));
                // assertTrue(validateValueParameter(param, new StringType("6")));
                Parameters param5 = (Parameters) resultingBundle.getEntry().get(4).getResource();
                assertTrue(param5.getIdElement().getIdPart().equals("Six Times Five"));
                // assertTrue(validateValueParameter(param, new StringType("30")));
        }

        @Test
        public void testCqlExecutionProviderFhirResourceResultParameters() throws Exception {    
                RequestDetails theRequest = Mockito.mock(RequestDetails.class);
                theRequest.setFhirServerBase(getLocalServer());
                when(theRequest.getServer()).thenReturn(Mockito.mock(IRestfulServerDefaults.class));
                BufferedReader reader = new BufferedReader(new InputStreamReader(CqlExecutionProviderIT.class.getResourceAsStream("cql/FhirEvaluation.cql")));
                String cqlCode = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                // Patient First
                uploadTests("test/FhirEvaluation/Patient");
                Map<String, IBaseResource> resources = uploadTests("test/FhirEvaluation");
                IBaseResource patient = resources.get("FhirEvaluation");
                Bundle resultingBundle = cqlExecutionProvider.evaluate(theRequest, cqlCode, patient.getIdElement().getIdPart(), "2019-01-01", "2020-01-01", null, getLocalServer(), null, null, "Patient", null);
                assertTrue(resultingBundle.getEntry().size() == 2);
                Parameters param = (Parameters) resultingBundle.getEntry().get(0).getResource();
                assertTrue(param.getIdElement().getIdPart().equals("MedicationRequest"));
                // assertTrue(validateValueParameter(param, new StringType("6")));
        }

        // private Boolean validateValueParameter(Parameters param, Type type) {
        //         for(ParametersParameterComponent parameter : param.getParameter()) {
        //                 if (parameter.getName().equals("value")) {
        //                         return parameter.getValue().equals(type);
        //                 }
        //         }
        //         return false;
        // }

        private Map<String, IBaseResource> uploadTests(String testDirectory) throws URISyntaxException, IOException {
                URL url = CqlExecutionProviderIT.class.getResource(testDirectory);
                File testDir = new File(url.toURI());
                assertTrue(testDir.isDirectory());
                return uploadTests(testDir.listFiles());
        }

        private Map<String, IBaseResource>  uploadTests(File[] files) throws IOException {
                Map<String, IBaseResource> resources = new HashMap<String, IBaseResource>();
                for(File file : files) {
                        if (file.isDirectory()) {
                                resources.putAll(uploadTests(file.listFiles()));
                        } else if (file.isFile()) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
                                String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                                reader.close();
                                IBaseResource resource = loadResource(FilenameUtils.getExtension(file.getAbsolutePath()), resourceString, ourCtx, myDaoRegistry);
                                resources.put(resource.getIdElement().getIdPart(), resource);
                        }
                }
                return resources;
        }

        private String getLocalServer() {
                return "http://localhost:" + port + "/fhir/";
        }

        private IBaseResource uploadResource(String location) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(CqlExecutionProviderIT.class.getResourceAsStream(location)));
            String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            reader.close();
            return this.loadResource("json", resourceString, ourCtx, myDaoRegistry);
        }

}
