package org.opencds.cqf.ruler.plugin.hello;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.Operation;

public class HelloWorldProvider implements OperationProvider {

    @Autowired
    HelloWorldProperties helloWorldProperties;


    @Operation(idempotent=true, name = "$hello-world")
    public OperationOutcome message() {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setDiagnostics(helloWorldProperties.getMessage());
        return outcome;
    }
}
