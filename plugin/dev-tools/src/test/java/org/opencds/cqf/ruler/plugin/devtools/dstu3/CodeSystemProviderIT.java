package org.opencds.cqf.ruler.plugin.devtools.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.testutility.IServerSupport;
import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
import org.opencds.cqf.ruler.plugin.utility.IdUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.UriParam;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class }, properties ={"hapi.fhir.fhir_version=dstu3", "spring.batch.job.enabled=false", "spring.main.allow-bean-definition-overriding=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CodeSystemProviderIT implements IServerSupport, IdUtilities, ClientUtilities {
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

    private String icd10 = "http://hl7.org/fhir/sid/icd-10";
    private String rxNormUrl = "http://www.nlm.nih.gov/research/umls/rxnorm";
    private String snomedSctUrl = "http://snomed.info/sct";
    private String cptUrl = "http://www.ama-assn.org/go/cpt";

    @BeforeEach
	void beforeEach() throws IOException {
        ourClient = createClient(FhirVersionEnum.DSTU3, "http://localhost:" + port + "/fhir/");
	}

    @AfterEach
    void tearDown() {
        // These are not actually doing anything right now
        ourClient.delete().resourceConditionalByType(CodeSystem.class);
        ourClient.delete().resourceConditionalByType(ValueSet.class);
    }

    @Test
    @Order(1)
    public void testCodeSystemUpdateValueSetDNE() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(CodeSystemProviderIT.class.getResourceAsStream("valueset" + "/" + "AntithromboticTherapy.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        ValueSet vs = (ValueSet) loadResource("json", resourceString, ourCtx, null);
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        assertTrue(outcome.getIssue().size() == 1);
        OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
        assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issue.getDetails().getText().startsWith("Unable to find Resource: " + vs.getIdElement().getIdPart()));
    }

    @Test
    @Order(2)
    public void testCodeSystemUpdateValueSetIdNull() {
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(new ValueSet().getIdElement());
        assertTrue(outcome.getIssue().size() == 1);
        OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
        assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issue.getDetails().getText().startsWith("Unable to find Resource: null"));
    }

    @Test
    @Order(3)
    public void testDSTU3RxNormCodeSystemUpdateById() throws IOException {
        log.info("Beginning Test DSTU3 RxNorm CodeSystemUpdate");

        BufferedReader reader = new BufferedReader(new InputStreamReader(CodeSystemProviderIT.class.getResourceAsStream("valueset" + "/" + "AntithromboticTherapy.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        ValueSet vs = (ValueSet) loadResource("json", resourceString, ourCtx, myDaoRegistry);

        assertEquals(0, performCodeSystemSearchByUrl(rxNormUrl).size());
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.INFORMATION));
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("rxnorm"));
        }
        assertEquals(1, performCodeSystemSearchByUrl(rxNormUrl).size());

        log.info("Finished Test DSTU3 RxNorm CodeSystemUpdate");
    }

    @Test
    @Order(4)
    public void testDSTU3ICD10PerformCodeSystemUpdateByList() throws IOException {
        log.info("Beginning Test DSTU3 ICD10 CodeSystemUpdate");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(CodeSystemProviderIT.class.getResourceAsStream("valueset" + "/" + "AllPrimaryandSecondaryCancer.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        ValueSet vs = (ValueSet) loadResource("json", resourceString, ourCtx, myDaoRegistry);

        assertEquals(0, performCodeSystemSearchByUrl(icd10).size());
        codeSystemUpdateProvider.performCodeSystemUpdate(Arrays.asList(vs));
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.INFORMATION));
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("icd-10"));
        }
        assertEquals(1,performCodeSystemSearchByUrl(icd10).size());

        log.info("Finished Test DSTU3 ICD10 CodeSystemUpdate");
    }

    @Test
    @Order(5)
    public void testDSTU3UpdateCodeSystems() throws IOException {
        log.info("Beginning Test DSTU3 Update Code Systems");

        assertEquals(0, performCodeSystemSearchByUrl(cptUrl).size());
        
        File[] valuesets = new File(CodeSystemProviderIT.class.getResource("valueset").getPath()).listFiles();
        for (File file : valuesets) {
            if (file.isFile() && FilenameUtils.getExtension(file.getPath()).equals("json")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                loadResource("json", resourceString, ourCtx, myDaoRegistry);    
            } else if (file.isFile() && FilenameUtils.getExtension(file.getPath()).equals("xml")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                loadResource("xml", resourceString, ourCtx, myDaoRegistry);  
            }
        }
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems();
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.INFORMATION));
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("cpt"));
            assertTrue(issue.getDetails().getText().contains("icd-10"));
            assertTrue(issue.getDetails().getText().contains("sct"));
            assertTrue(issue.getDetails().getText().contains("rxnorm"));
        }
        assertEquals(1, performCodeSystemSearchByUrl(icd10).size());
        assertEquals(1, performCodeSystemSearchByUrl(rxNormUrl).size());
        assertEquals(1, performCodeSystemSearchByUrl(snomedSctUrl).size());
        assertEquals(1, performCodeSystemSearchByUrl(cptUrl).size());

        log.info("Finished Test DSTU3 Update Code Systems");
    }

    private IBundleProvider performCodeSystemSearchByUrl(String rxNormUrl) {
        return this.myDaoRegistry.getResourceDao(CodeSystem.class)
                .search(SearchParameterMap.newSynchronous().add(CodeSystem.SP_URL, new UriParam(rxNormUrl)));
    }
}
