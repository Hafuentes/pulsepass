package com.pulse.pass.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida JWT firmados con HS256 (JJWT 0.13). Solo conoce firma, issuer, fechas y sujeto:
 * no consulta tickets ni ejecuta negocio.
 */
@Service
public class JwtService {

    static final int MIN_KEY_BYTES = 32;
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.key = buildKey(properties.secretBase64());
        this.issuer = properties.issuer();
        this.ttlSeconds = properties.accessTokenTtl().toSeconds();
        this.clock = clock;
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("pulsepass.security.jwt.issuer must not be blank.");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl must be positive.");
        }
    }

    /** La aplicacion falla al arrancar si la clave falta o es insuficiente (SEC004, 12.2). */
    private static SecretKey buildKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing. Set the PULSEPASS_JWT_SECRET_BASE64 environment variable "
                            + "(Base64 of at least 32 random bytes).");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretBase64.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT secret is not valid Base64.");
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short: at least %d bytes are required.".formatted(MIN_KEY_BYTES));
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /** Claims: sub (ID interno), iss, iat, exp, jti. Sin roles ni datos personales. */
    public String generateToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpiresInSeconds() {
        return ttlSeconds;
    }

    /**
     * Verifica firma, algoritmo, issuer, expiracion, sub, iat y jti y devuelve el ID del usuario.
     *
     * @throws InvalidTokenException ante cualquier irregularidad (nunca otra excepcion).
     */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Empty token");
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token);

            if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
                throw new InvalidTokenException("Unexpected signing algorithm");
            }
            Claims claims = jws.getPayload();
            String subject = claims.getSubject();
            Date issuedAt = claims.getIssuedAt();
            if (subject == null || subject.isBlank()) {
                throw new InvalidTokenException("Missing subject");
            }
            if (claims.getExpiration() == null) {
                throw new InvalidTokenException("Missing expiration");
            }
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw new InvalidTokenException("Missing jti");
            }
            if (issuedAt == null || issuedAt.toInstant().isAfter(clock.instant().plusSeconds(CLOCK_SKEW_SECONDS))) {
                throw new InvalidTokenException("Unacceptable issued-at");
            }
            return Long.valueOf(subject);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // JwtException (expirado, firma, malformado, unsupported...), IllegalArgumentException,
            // NumberFormatException, etc. Se traducen a una unica excepcion sin detalles internos.
            throw new InvalidTokenException("Invalid token", ex);
        }
    }
}
