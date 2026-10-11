package com.pulse.pass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/** Contrato uniforme de errores HTTP (seccion 13 del PRD). */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
