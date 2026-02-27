package com.aboff.core.config;

import com.aboff.core.security.JwtAuthenticationEntryPoint;
import com.aboff.core.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Spring Security.
 * Configures authentication, authorization, token filters, and password
 * encoding.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Value("${application.cors.allowed-origins:}")
        private String allowedOrigins;

        @Value("${application.security.bcrypt-strength:12}")
        private int bcryptStrength;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        }

        /**
         * Configures the security filter chain.
         * Disables CSRF, sets session management to stateless, configures public
         * endpoints,
         * adds the JWT filter, and sets the exception handler.
         *
         * @param http the HttpSecurity to modify
         * @return the SecurityFilterChain
         * @throws Exception if an error occurs during configuration
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Enable CORS with custom configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // Disable CSRF - using SameSite=Strict cookies for protection
                                .csrf(csrf -> csrf.disable())
                                // Stateless session management (JWT-based)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Authorization rules
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .anyRequest().authenticated())
                                // Add JWT filter before UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                // Handle authentication failures
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                return http.build();
        }

        /**
         * Provides the password encoder bean.
         * Uses BCrypt with configurable strength (default 12).
         * Strength can be overridden via {@code application.security.bcrypt-strength} property.
         *
         * @return the BCryptPasswordEncoder
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(bcryptStrength);
        }

        /**
         * Provides the authentication manager bean.
         *
         * @param authConfig the authentication configuration
         * @return the AuthenticationManager
         * @throws Exception if an error occurs
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        /**
         * Configures CORS settings for cross-origin requests.
         * Allowed origins are configured via application.cors.allowed-origins property.
         * In development, localhost origins are permitted; in production, only explicit origins are allowed.
         * Credentials are allowed to support HttpOnly cookie-based JWT authentication.
         *
         * @return the CorsConfigurationSource
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parse allowed origins from configuration
                if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .toList();
                        configuration.setAllowedOrigins(origins);
                }

                // Allow standard HTTP methods
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                // Allow common headers
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With"));

                // Required for cookie-based authentication
                configuration.setAllowCredentials(true);

                // Cache preflight response for 1 hour
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }
}
