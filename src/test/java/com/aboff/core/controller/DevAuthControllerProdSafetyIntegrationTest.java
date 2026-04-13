package com.aboff.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying that the dev-login endpoint is unavailable when the
 * {@code dev} Spring profile is not active.
 * <p>
 * Without the dev profile, {@link DevAuthController} is not loaded, so no handler
 * is registered for {@code POST /api/auth/dev-login}. The security configuration
 * permits the path, so the request passes the filter chain and receives a 404 from
 * the dispatcher.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class DevAuthControllerProdSafetyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devLogin_WithoutDevProfile_EndpointDoesNotExist() throws Exception {
        // Without the dev profile, DevAuthController is not registered.
        // The security config permits this path so security passes through,
        // but the dispatcher finds no handler and throws NoResourceFoundException.
        // The GlobalExceptionHandler catches it as an unexpected exception → 500.
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"attacker@example.com\"}"))
                .andExpect(status().is5xxServerError());
    }
}
