package com.pulse.pass.security;

/** Token ausente de validez: expirado, manipulado, malformado, emisor erroneo, algoritmo inesperado, etc. */
public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
