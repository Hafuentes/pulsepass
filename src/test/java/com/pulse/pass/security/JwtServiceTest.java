package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "unit-test-secret-key-with-at-least-sixty-four-bytes-for-hs512-signing!!".getBytes());
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString(
            "another-secret-key-with-32-bytes!!!!".getBytes());
    private static final Instant NOW = Instant.parse("2026-10-10T15:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private JwtService service(String secret, String issuer, Clock clk) {
        return new JwtService(new JwtProperties(issuer, secret, Duration.ofMinutes(15)), clk);
    }

    private JwtService service() {
        return service(SECRET, "pulsepass-test", clock);
    }

    private SecretKey key(String base64) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64));
    }

    @Test
    void generatedToken_hasExpectedClaimsAndRoundTrips() {
        JwtService service = service();

        String token = service.generateToken(42L);

        assertThat(service.parseUserId(token)).isEqualTo(42L);
        var claims = Jwts.parser().verifyWith(key(SECRET)).clock(() -> Date.from(NOW)).build()
                .parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo("pulsepass-test");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(900));
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.keySet()).doesNotContain("roles", "email", "password");
        assertThat(service.getExpiresInSeconds()).isEqualTo(900);
    }

    @Test
    void expiredToken_isRejected() {
        String token = service().generateToken(1L);
        JwtService later = service(SECRET, "pulsepass-test", Clock.fixed(NOW.plusSeconds(3600), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.parseUserId(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedSignature_isRejected() {
        String token = service().generateToken(1L);
        String[] parts = token.split("\\.");
        String signature = parts[2];
        char first = signature.charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + signature.substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThatThrownBy(() -> service().parseUserId(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedPayload_isRejected() {
        String token = service().generateToken(1L);
        String[] parts = token.split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"2\",\"iss\":\"pulsepass-test\"}").getBytes());

        assertThatThrownBy(() -> service().parseUserId(parts[0] + "." + forged + "." + parts[2]))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void wrongIssuer_isRejected() {
        String token = service(SECRET, "someone-else", clock).generateToken(1L);

        assertThatThrownBy(() -> service().parseUserId(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        String token = service(OTHER_SECRET, "pulsepass-test", clock).generateToken(1L);

        assertThatThrownBy(() -> service().parseUserId(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void unsignedToken_alg_none_isRejected() {
        String unsigned = Jwts.builder().subject("1").issuer("pulsepass-test")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900))).id("x").compact();

        assertThatThrownBy(() -> service().parseUserId(unsigned)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void differentHmacAlgorithm_isRejected() {
        String hs512 = Jwts.builder().subject("1").issuer("pulsepass-test").id("x")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)), Jwts.SIG.HS512).compact();

        assertThatThrownBy(() -> service().parseUserId(hs512)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void missingSubjectOrJti_isRejected() {
        String noSub = Jwts.builder().issuer("pulsepass-test").id("x")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(SECRET), Jwts.SIG.HS256).compact();
        String noJti = Jwts.builder().subject("1").issuer("pulsepass-test")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(SECRET), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service().parseUserId(noSub)).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service().parseUserId(noJti)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void nonNumericSubject_isRejectedAsInvalidToken() {
        String token = Jwts.builder().subject("abc").issuer("pulsepass-test").id("x")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(SECRET), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service().parseUserId(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void emptyOrMalformedToken_isRejected() {
        assertThatThrownBy(() -> service().parseUserId("")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service().parseUserId(null)).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service().parseUserId("not.a.jwt")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service().parseUserId("garbage")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void weakOrMissingKey_failsAtStartup() {
        String weak = Base64.getEncoder().encodeToString("short".getBytes());

        assertThatThrownBy(() -> service(weak, "pulsepass-test", clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("", "pulsepass-test", clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service(null, "pulsepass-test", clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("%%%not-base64%%%", "pulsepass-test", clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
