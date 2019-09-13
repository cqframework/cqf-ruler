package org.opencds.cqf.qdm.fivepoint4.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QdmError
{
    private String errorCode;
    private String message;

    public QdmError(String errorCode, String message) {
        super();
        this.errorCode = errorCode;
        this.message = message;
    }
}
