package com.pulse.pass.exception;

/** Se lanza cuando existe un conflicto de unicidad (ej. "Username already exists."). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
