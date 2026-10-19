package com.pulse.pass.dto.response;

import java.util.List;

/** Informacion minima del principal; nunca secretos. */
public record AuthenticatedUserResponse(Long id, String username, String email, List<String> roles) {
}
