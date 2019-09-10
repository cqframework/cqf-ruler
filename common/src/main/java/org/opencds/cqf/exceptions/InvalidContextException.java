package org.opencds.cqf.exceptions;

public class InvalidContextException extends RuntimeException {

    private String message = "CDS Hooks requests require a context object be specified and populated with the appropriate fields for the specific hook.";

    public InvalidContextException() {}

    public InvalidContextException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
