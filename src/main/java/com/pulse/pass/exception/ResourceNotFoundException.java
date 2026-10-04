package com.pulse.pass.exception;

/** Se lanza cuando el recurso solicitado no existe (ej. "Event not found: CMF-2026"). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
