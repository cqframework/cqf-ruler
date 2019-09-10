package org.opencds.cqf.exceptions;

public class MissingRequiredFieldException extends RuntimeException {

    private String message;

    public MissingRequiredFieldException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
