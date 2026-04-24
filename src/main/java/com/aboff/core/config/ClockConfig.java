package com.aboff.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a {@link Clock} bean so time-sensitive components (throttles,
 * scheduled work, audit timestamps) can be unit-tested with a fixed or
 * stepping clock. Production uses the system default zone.
 */
@Configuration
public class ClockConfig {

    /**
     * Returns the application-wide default {@link Clock}.
     *
     * @return a system-zone clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
