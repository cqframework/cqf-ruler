package org.opencds.cqf.exceptions;

public class MissingUserException extends RuntimeException {

    @Override
    public String getMessage() {
        return "CDS Hooks requests require a user reference (e.g. Practitioner/123) be specified.";
    }
}
