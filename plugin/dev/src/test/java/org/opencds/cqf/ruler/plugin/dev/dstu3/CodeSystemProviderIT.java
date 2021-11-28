package org.opencds.cqf.ruler.plugin.dev.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.dev.DevToolsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class }, properties ={"hapi.fhir.fhir_version=dstu3", "hapi.fhir.dev.enabled=true"})
public class CodeSystemProviderIT {
    private Logger log = LoggerFactory.getLogger(CodeSystemProviderIT.class);

    private IGenericClient ourClient;

    private FhirContext ourCtx = FhirContext.forCached(FhirVersionEnum.DSTU3);
    
    // @Autowired
    // private DaoRegistry ourRegistry;

    // @LocalServerPort
    // private int port;

    // @Autowired
    // CodeSystemUpdateProvider codeSystemUpdateProvider;

    private List<ValueSet> valuesets = new ArrayList<ValueSet>();

    private IParser parser;

    @BeforeEach
	void beforeEach() {
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		//String ourServerBase = "http://localhost:" + port + "/fhir/";
		//ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
	}

    @Test
    public void testDSTU3CodeSystemUpdate() throws IOException {
        parser = ourCtx.newJsonParser();
        log.info("Beginning Test DSTU3 CodeSystemUpdate");
        URL url = CodeSystemProviderIT.class.getResource("valueset");
        String path = url.getPath();
        File resourceDirectory = new File(path);
        File[] resourceFiles = resourceDirectory.listFiles();
        for (File resourceFile : resourceFiles) {
            try {
                IBaseResource resource = parser.parseResource(CodeSystemProviderIT.class.getResourceAsStream("valueset/" + resourceFile.getName()));
                if (resource instanceof ValueSet) {
                    valuesets.add((ValueSet) resource);
                }  
            } catch (Exception e) {
                log.error(String.format("Failure parsing file, %s.  Expected Json ValueSet", resourceFile.getName()), e);
            }
        }
        //codeSystemUpdateProvider.updateCodeSystems();

        log.info("Finished Test DSTU3 CodeSystemUpdate");
    }
}
