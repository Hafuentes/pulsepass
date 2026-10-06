package com.pulse.pass.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone un Clock inyectable para que las reglas dependientes del tiempo
 * (fecha futura, edad, evento ya ocurrido) sean deterministas en los unit tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
