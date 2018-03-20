package org.opencds.cqf.exceptions;

public class MissingHookException extends RuntimeException {

    @Override
    public String getMessage() {
        return "CDS Hooks requests require a hook ID be specified.";
    }
}
