package com.aboff.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles authentication errors during unauthorized access requests.
 * Returns a 401 Unauthorized response with a JSON error body.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Commences an authentication scheme.
         * Called when an unauthenticated user attempts to access a protected resource.
         *
         * @param request       that resulted in an AuthenticationException
         * @param response      so that the user agent can begin authentication
         * @param authException that caused the invocation
         * @throws IOException      if an input or output exception occurs
         * @throws ServletException if a servlet exception occurs
         */
        @Override
        public void commence(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {

                log.warn("Authentication failed for request {} {}: {}",
                                request.getMethod(), request.getServletPath(), authException.getMessage());
                log.debug("Authentication failure details - Remote addr: {}, User-Agent: {}",
                                request.getRemoteAddr(), request.getHeader("User-Agent"));

                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                Map<String, Object> body = new HashMap<>();
                body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                body.put("error", "Unauthorized");
                body.put("message", authException.getMessage());
                body.put("path", request.getServletPath());
                body.put("timestamp", LocalDateTime.now().toString());

                objectMapper.writeValue(response.getOutputStream(), body);
        }
}
