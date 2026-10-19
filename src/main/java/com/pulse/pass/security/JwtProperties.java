package com.pulse.pass.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** pulsepass.security.jwt.* (NFR-SEC008). El secreto se lee del entorno y se valida en JwtService. */
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        @DefaultValue("pulsepass-api") String issuer,
        @DefaultValue("") String secretBase64,
        @DefaultValue("PT15M") Duration accessTokenTtl
) {
}
