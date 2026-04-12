package com.aboff.core.config;

import com.aboff.core.security.JwtAuthenticationEntryPoint;
import com.aboff.core.security.JwtAuthenticationFilter;
import com.aboff.core.security.OAuth2LoginFailureHandler;
import com.aboff.core.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Spring Security.
 * Configures OAuth2 login, JWT filter, authorization rules, and CORS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

        @Value("${application.cors.allowed-origins:}")
        private String allowedOrigins;

        /**
         * Constructs a new SecurityConfig with required dependencies.
         *
         * @param jwtAuthenticationFilter    the JWT authentication filter
         * @param jwtAuthenticationEntryPoint the JWT authentication entry point
         * @param oAuth2LoginSuccessHandler  the OAuth2 login success handler
         * @param oAuth2LoginFailureHandler  the OAuth2 login failure handler
         */
        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                        OAuth2LoginFailureHandler oAuth2LoginFailureHandler) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
                this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
                this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        }

        /**
         * Configures the security filter chain.
         * Disables CSRF, configures OAuth2 login with success/failure handlers,
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
                                // Disable CSRF - using SameSite=Lax cookies for protection
                                .csrf(csrf -> csrf.disable())
                                // Session required for OAuth2 authorization code flow
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                // Authorization rules
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                                                .requestMatchers("/api/auth/logout", "/api/auth/dev-login").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .anyRequest().authenticated())
                                // OAuth2 login configuration
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .failureHandler(oAuth2LoginFailureHandler))
                                // Add JWT filter before UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                // Handle authentication failures
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                return http.build();
        }

        /**
         * Configures CORS settings for cross-origin requests.
         * Allowed origins are configured via application.cors.allowed-origins property.
         * In development, localhost origins are permitted; in production, only explicit origins are allowed.
         * Credentials are allowed to support HttpOnly cookie-based JWT authentication.
         * The pattern covers all paths including OAuth2 callback endpoints.
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
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
