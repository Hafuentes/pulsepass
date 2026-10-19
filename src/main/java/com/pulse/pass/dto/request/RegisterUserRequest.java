package com.pulse.pass.dto.request;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email") String email,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        String phone,
        String city,
        @NotNull(message = "Birth date is required") LocalDate birthDate,
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters") String password
) {

    /** BCrypt solo procesa 72 bytes: se rechaza (400) en lugar de truncar silenciosamente. */
    @AssertTrue(message = "Password must not exceed 72 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    /** Evita que la contrasena aparezca en logs o trazas. */
    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
