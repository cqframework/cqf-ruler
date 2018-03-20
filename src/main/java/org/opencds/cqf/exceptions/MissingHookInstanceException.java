package org.opencds.cqf.exceptions;

public class MissingHookInstanceException extends RuntimeException {

    @Override
    public String getMessage() {
        return "CDS Hooks requests require a hookInstance UUID be specified.";
    }
}
