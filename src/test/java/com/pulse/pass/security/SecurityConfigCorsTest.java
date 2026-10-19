package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

/** NFR-SEC008: la politica CORS se parametriza y exige origenes explicitos. */
class SecurityConfigCorsTest {

    private final SecurityConfig config = new SecurityConfig();

    private CorsProperties props(String... origins) {
        return new CorsProperties(List.of(origins), List.of("GET", "POST"), List.of("Authorization"),
                Duration.ofHours(1));
    }

    @Test
    void wildcardOrigin_failsAtStartup() {
        assertThatThrownBy(() -> config.corsConfigurationSource(props("*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit origins");
        assertThatThrownBy(() -> config.corsConfigurationSource(props("https://*.example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noOrigins_disablesCorsWithoutFailing() {
        assertThat(config.corsConfigurationSource(props())).isNotNull();
        assertThat(config.corsConfigurationSource(props("  ", ""))).isNotNull();
    }

    @Test
    void explicitOrigins_areAccepted() {
        assertThat(config.corsConfigurationSource(props("https://app.example.com"))).isNotNull();
    }
}
