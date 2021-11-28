package org.opencds.cqf.ruler.plugin.dev.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.dev.IServerSupport;
import org.opencds.cqf.ruler.plugin.dev.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.dev.DevToolsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.rest.client.api.IGenericClient;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class}, properties ="hapi.fhir.fhir_version=r4")
public class CodeSystemProviderIT implements IServerSupport {
    private Logger log = LoggerFactory.getLogger(CodeSystemProviderIT.class);

    @Autowired
    private IGenericClient ourClient;

    @Autowired
    private DevToolsProperties myDevToolsProperties;

    @Autowired
    CodeSystemUpdateProvider codeSystemUpdateProvider;

    @Test
    public void testR4CodeSystemUpdate() throws IOException {
        log.info("Beginning Test R4 CodeSystemUpdate");


        log.info("Finished Test R4 CodeSystemUpdate");
    }
}
