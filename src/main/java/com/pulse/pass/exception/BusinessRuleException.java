package com.pulse.pass.exception;

/** Se lanza cuando el recurso existe pero la operacion viola una regla de negocio. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
