package org.opencds.cqf.ruler.plugin.cdshooks.exceptions;

@SuppressWarnings("serial")
public class InvalidHookException extends RuntimeException {

    public InvalidHookException(String message) {
        super(message);
    }

}
