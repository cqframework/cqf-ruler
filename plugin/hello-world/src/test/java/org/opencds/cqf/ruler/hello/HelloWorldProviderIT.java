package org.opencds.cqf.ruler.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { HelloWorldProviderIT.class,
        HelloWorldConfig.class }, properties = {
            "hapi.fhir.fhir_version=r4",
            "hello.world.message=Howdy"

})
public class HelloWorldProviderIT  extends RestIntegrationTest {
    @Test
    public void testHelloWorldConfig() {

        OperationOutcome outcome = getClient().operation().onServer().named("$hello-world")
                .withNoParameters(Parameters.class).returnResourceType(OperationOutcome.class).execute();

        assertNotNull(outcome);
        assertEquals("Howdy", outcome.getIssueFirstRep().getDiagnostics());
    }
}
