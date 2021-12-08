package org.opencds.cqf.ruler.plugin.cr.dstu3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cr.CrConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CrConfig.class, CqlConfig.class }, properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "hapi.fhir.fhir_version=dstu3",
            "hapi.fhir.cr.enabled=true",
            "hapi.fhir.cql.enabled=true"
})
public class ExpressionEvaluationIT {

    @Autowired
    private ExpressionEvaluation expressionEvaluation;
    @Autowired
    private FhirContext context;

    @Test
    private void testExpressionEvaluation() throws Exception {
        RequestDetails theRequest = new SystemRequestDetails();
        expressionEvaluation.evaluateInContext(instance, cql, patientId, theRequest);
    }
    
}
