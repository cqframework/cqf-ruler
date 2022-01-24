package org.opencds.cqf.ruler.devtools.r4;

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
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.api.server.IBundleProvider;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class }, properties ={"hapi.fhir.fhir_version=r4", "spring.batch.job.enabled=false", "spring.main.allow-bean-definition-overriding=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CodeSystemProviderIT extends RestIntegrationTest  {
    private Logger log = LoggerFactory.getLogger(CodeSystemProviderIT.class);

    @Autowired
    CodeSystemUpdateProvider codeSystemUpdateProvider;

    private String loincUrl = "http://loinc.org";
    private String snomedSctUrl = "http://snomed.info/sct";
    private String cptUrl = "http://www.ama-assn.org/go/cpt";

    @AfterEach
    void tearDown() {
        // These are not actually doing anything right now
        getClient().delete().resourceConditionalByType(CodeSystem.class);
        getClient().delete().resourceConditionalByType(ValueSet.class);
    }

    @Test
    @Order(1)
    public void testCodeSystemUpdateValueSetDNE() throws IOException {
        ValueSet vs = (ValueSet) readResource("org/opencds/cqf/ruler/devtools/r4/valueset/valueset-pain-treatment-plan.json");
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        assertEquals(1, outcome.getIssue().size());
        OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertTrue(issue.getDetails().getText().startsWith("Unable to find Resource: " + vs.getIdElement().getIdPart()));
    }

    @Test
    @Order(2)
    public void testCodeSystemUpdateValueSetIdNull() {
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(new ValueSet().getIdElement());
        assertEquals(1, outcome.getIssue().size());
        OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
        assertEquals(OperationOutcome.IssueSeverity.ERROR,issue.getSeverity());
        assertTrue(issue.getDetails().getText().startsWith("Unable to find Resource: null"));
    }

    @Test
    @Order(3)
    public void testR4RxNormCodeSystemUpdateById() throws IOException {
        log.info("Beginning Test R4 LOINC CodeSystemUpdate");
        ValueSet vs = (ValueSet) loadResource("org/opencds/cqf/ruler/devtools/r4/valueset/valueset-pain-treatment-plan.json");

        assertEquals(0, performCodeSystemSearchByUrl(loincUrl).size());
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertEquals(OperationOutcome.IssueSeverity.INFORMATION, issue.getSeverity());
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("loinc"));
        }
        assertEquals(1, performCodeSystemSearchByUrl(loincUrl).size());

        log.info("Finished Test R4 LOINC CodeSystemUpdate");
    }

    @Test
    @Order(4)
    public void testR4ICD10PerformCodeSystemUpdateByList() throws IOException {
        log.info("Beginning Test R4 SNOMED CodeSystemUpdate");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(CodeSystemProviderIT.class.getResourceAsStream("valueset" + "/" + "valueset-pdmp-review-procedure.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        ValueSet vs = (ValueSet) loadResource("json", resourceString);

        assertEquals(0, performCodeSystemSearchByUrl(snomedSctUrl).size());
        codeSystemUpdateProvider.performCodeSystemUpdate(Arrays.asList(vs));
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems(vs.getIdElement());
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
				assertEquals(OperationOutcome.IssueSeverity.INFORMATION, issue.getSeverity());
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("sct"));
        }
        assertEquals(1,performCodeSystemSearchByUrl(snomedSctUrl).size());

        log.info("Finished Test R4 SNOMED CodeSystemUpdate");
    }

    @Test
    @Order(5)
    public void testR4UpdateCodeSystems() throws IOException {
        log.info("Beginning Test R4 Update Code Systems");

        assertEquals(0, performCodeSystemSearchByUrl(cptUrl).size());
        
        File[] valuesets = new File(CodeSystemProviderIT.class.getResource("valueset").getPath()).listFiles();
        for (File file : valuesets) {
            if (file.isFile() && FilenameUtils.getExtension(file.getPath()).equals("json")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                loadResource("json", resourceString);    
            } else if (file.isFile() && FilenameUtils.getExtension(file.getPath()).equals("xml")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                reader.close();
                loadResource("xml", resourceString);  
            }
        }
        OperationOutcome outcome = codeSystemUpdateProvider.updateCodeSystems();
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertEquals(OperationOutcome.IssueSeverity.INFORMATION,issue.getSeverity());
            assertTrue(issue.getDetails().getText().startsWith("Successfully updated the following CodeSystems: "));
            assertTrue(issue.getDetails().getText().contains("cpt"));
            assertTrue(issue.getDetails().getText().contains("sct"));
            assertTrue(issue.getDetails().getText().contains("loinc"));
        }
        assertEquals(1, performCodeSystemSearchByUrl(loincUrl).size());
        assertEquals(1, performCodeSystemSearchByUrl(snomedSctUrl).size());
        assertEquals(1, performCodeSystemSearchByUrl(cptUrl).size());

        log.info("Finished Test R4 Update Code Systems");
    }

    private IBundleProvider performCodeSystemSearchByUrl(String rxNormUrl) {
		 return search(CodeSystem.class, Searches.byUrl(rxNormUrl));
    }
}
