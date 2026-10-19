package com.pulse.pass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** identifier = username o correo. */
public record LoginRequest(
        @NotBlank(message = "Identifier is required")
        @Size(max = 180, message = "Identifier must not exceed 180 characters") String identifier,
        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must not exceed 128 characters") String password
) {

    /** Evita que la contrasena aparezca en logs o trazas. */
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=***]";
    }
}
