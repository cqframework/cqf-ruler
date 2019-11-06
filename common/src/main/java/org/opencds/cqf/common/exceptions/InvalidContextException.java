package org.opencds.cqf.common.exceptions;

public class InvalidContextException extends RuntimeException {
    private static final long serialVersionUID = 1L;

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
