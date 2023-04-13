package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
        PlanDefinitionApplyProviderIT.class,
        CrConfig.class, CqlConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
class PlanDefinitionApplyProviderIT extends RestIntegrationTest {

    @Autowired
    private PlanDefinitionOperationsProvider planDefinitionApplyProvider;

    private Map<String, IBaseResource> planDefinitions;

    @BeforeEach
    public void setup() throws Exception {
        uploadTests("valueset");
        uploadTests("library");
        planDefinitions = uploadTests("plandefinition");
    }

    @Test
    void testPlanDefinitionApplyRec10NoScreenings() throws Exception {
        DomainResource plandefinition = (DomainResource) planDefinitions.get("opioidcds-10");
        // Patient First
        uploadTests("test/plandefinition/Rec10/Patient");
        Map<String, IBaseResource> resources = uploadTests("test/plandefinition/Rec10");
        IBaseResource patient = resources.get("example-rec-10-no-screenings");
        Object recommendation = planDefinitionApplyProvider.applyPlanDefinition(new SystemRequestDetails(),
                plandefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null,
                null,
                null);
        assertNotNull(recommendation);
    }
}
