package org.opencds.cqf.exceptions;

public class MissingFhirServerException extends RuntimeException {

    @Override
    public String getMessage() {
        return "CDS Hooks requests require a fhir server endpoint be specified if the prefetch is not supplied.";
    }
}
