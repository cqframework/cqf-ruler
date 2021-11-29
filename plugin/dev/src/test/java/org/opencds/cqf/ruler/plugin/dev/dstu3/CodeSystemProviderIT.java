package org.opencds.cqf.ruler.plugin.dev.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;

import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.dev.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.dev.TesterUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.UriParam;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class }, properties ={"hapi.fhir.fhir_version=dstu3", "hapi.fhir.dev.enabled=true"})
public class CodeSystemProviderIT implements TesterUtilities {
    private Logger log = LoggerFactory.getLogger(CodeSystemProviderIT.class);

    @Autowired
    private FhirContext ourCtx;

    @LocalServerPort
    private int port;

    @Autowired
    CodeSystemUpdateProvider codeSystemUpdateProvider;
    
    @Autowired
    private DaoRegistry myDaoRegistry;

    private IGenericClient ourClient;

    @BeforeEach
	void beforeEach() throws IOException {
        ourClient = createClient(FhirVersionEnum.DSTU3, "http://localhost:" + port + "/fhir/");
	}

    @AfterEach
    void tearDown() {
        ourClient.delete().resourceConditionalByType(CodeSystem.class);
        ourClient.delete().resourceConditionalByType(ValueSet.class);
    }

    @Test
    @Order(0)
    public void testDSTU3RxNormCodeSystemUpdateById() throws IOException {       
        log.info("Beginning Test DSTU3 RxNorm CodeSystemUpdate");

        ValueSet vs = (ValueSet) loadResource(CodeSystemProviderIT.class.getResource("AntithromboticTherapy.json").getPath(), ourCtx, myDaoRegistry);
        myDaoRegistry.getResourceDao(ValueSet.class).update(vs);

        String rxNormUrl = "http://www.nlm.nih.gov/research/umls/rxnorm";
        assertEquals(performCodeSystemSearchByUrl(rxNormUrl).size(), 0);
        codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        assertEquals(performCodeSystemSearchByUrl(rxNormUrl).size(), 1);

        log.info("Finished Test DSTU3 RxNorm CodeSystemUpdate");
    }

    @Test
    @Order(1)
    public void testDSTU3ICD10PerformCodeSystemUpdateByList() throws IOException {
        log.info("Beginning Test DSTU3 RxNorm CodeSystemUpdate");
        
        ValueSet vs = (ValueSet) loadResource(CodeSystemProviderIT.class.getResource("AllPrimaryandSecondaryCancer.json").getPath(), ourCtx, myDaoRegistry);
        myDaoRegistry.getResourceDao(ValueSet.class).update(vs);

        String icd10 = "http://hl7.org/fhir/sid/icd-10";
        assertEquals(performCodeSystemSearchByUrl(icd10).size(), 0);
        codeSystemUpdateProvider.performCodeSystemUpdate(Arrays.asList(vs));
        assertEquals(performCodeSystemSearchByUrl(icd10).size(), 1);

        log.info("Finished Test DSTU3 RxNorm CodeSystemUpdate");
    }

    private IBundleProvider performCodeSystemSearchByUrl(String rxNormUrl) {
        return this.myDaoRegistry.getResourceDao(CodeSystem.class)
                .search(SearchParameterMap.newSynchronous().add(CodeSystem.SP_URL, new UriParam(rxNormUrl)));
    }
}
