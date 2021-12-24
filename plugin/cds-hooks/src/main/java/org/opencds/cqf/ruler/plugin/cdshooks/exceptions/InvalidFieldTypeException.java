package org.opencds.cqf.ruler.plugin.cdshooks.exceptions;

@SuppressWarnings("serial")
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
