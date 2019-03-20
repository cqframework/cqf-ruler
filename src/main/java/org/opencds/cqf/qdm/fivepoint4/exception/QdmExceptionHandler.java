package org.opencds.cqf.qdm.fivepoint4.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class QdmExceptionHandler
{
    @ExceptionHandler(value = { MethodArgumentNotValidException.class })
    protected ResponseEntity<String> handleException(MethodArgumentNotValidException ex) {
        return new ResponseEntity<>(ex.getBindingResult().toString(), HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST);
    }
}
