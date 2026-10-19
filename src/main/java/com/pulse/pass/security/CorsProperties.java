package com.pulse.pass.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * pulsepass.security.cors.* (NFR-SEC008). Por defecto NO se permite ningun origen (CORS desactivado).
 * Los origenes deben ser explicitos: no se admite "*".
 */
@ConfigurationProperties(prefix = "pulsepass.security.cors")
public record CorsProperties(
        @DefaultValue List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PATCH", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue({"Authorization", "Content-Type", "X-Request-Id"}) List<String> allowedHeaders,
        @DefaultValue("PT1H") Duration maxAge
) {
}
