package org.opencds.cqf.common.exceptions;

public class MissingRequiredFieldException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;

    public MissingRequiredFieldException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
