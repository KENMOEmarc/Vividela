package com.vivid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expose un {@link Clock} système comme bean Spring.
 *
 * Voir revue de code — "Couplage fort avec Instant.now() non testable" :
 * les services qui appellent directement Instant.now() sont difficiles à
 * tester unitairement (impossible de figer/contrôler le temps). En
 * injectant un Clock, les tests peuvent fournir un Clock.fixed(...) à la
 * place, sans changer le code de production.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
