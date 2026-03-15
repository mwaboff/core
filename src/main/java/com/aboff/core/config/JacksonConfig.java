package com.aboff.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Jackson ObjectMapper.
 * <p>
 * Provides a shared ObjectMapper bean for JSON serialization and deserialization
 * throughout the application.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates and configures the application-wide ObjectMapper bean.
     *
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
