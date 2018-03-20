package org.opencds.cqf.exceptions;

public class MissingContextException extends RuntimeException {

    @Override
    public String getMessage() {
        return "CDS Hooks requests require a context object be specified and populated.";
    }
}
