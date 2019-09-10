package org.opencds.cqf.exceptions;

public class InvalidFieldTypeException extends RuntimeException {

    private String message;

    public InvalidFieldTypeException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
