package org.opencds.cqf.common.exceptions;

public class InvalidFieldTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;

    public InvalidFieldTypeException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
