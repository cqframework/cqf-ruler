package com.converter;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;

/**
 * This is an example OperationProvider that returns a simple greeting. This is
 * meant to be a demonstration of how to implement an OperationProvider,
 * and not an actual implementation of anything. It also shows hows to use the
 * {@link Description} and {@link Operation}
 * annotations.
 * <p>
 * When implementing the operations it's important to capture the specific IG
 * the operation is defined in. Additional, release versions should be used
 * whenever possible.
 * Please add both the appropriate Javadoc comments so that implementors have
 * documentation when writing Java code, and also use the {@link Description}
 * annotation so that the relevant information is surfaced via the Tester UI and
 * Swagger UI.
 */
public class ConverterProvider implements OperationProvider {

	@Autowired
	ConverterProperties converterProperties;

	/**
	 * Implements the $hello-world operation found in the
	 * <a href="https://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR CR
	 * Module</a>
	 *
	 * @return a greeting
	 */
	@Description(shortDefinition = "returns a greeting", value = "Implements the $hello-world operation found in the <a href=\"https://www.hl7.org/fhir/clinicalreasoning-module.html\">FHIR CR Module</a>")
	@Operation(idempotent = true, name = "$convert-v1")
	public OperationOutcome convert_v1() {
		OperationOutcome outcome = new OperationOutcome();
		// outcome.addIssue().setDiagnostics(converterProperties.getMessage());
		return outcome;
	}
}