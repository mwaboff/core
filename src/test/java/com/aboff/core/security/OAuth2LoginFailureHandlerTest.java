package com.aboff.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OAuth2LoginFailureHandler}.
 * Verifies the redirect URL for the popup-based OAuth flow.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2LoginFailureHandlerTest {

    private static final String FRONTEND_BASE_URL = "http://localhost:4200";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OAuth2LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginFailureHandler(FRONTEND_BASE_URL);
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationFailure_RedirectsToAuthCallbackWithError() throws Exception {
        // Arrange
        AuthenticationException exception = new AuthenticationException("Bad credentials") {};

        // Act
        handler.onAuthenticationFailure(request, response, exception);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?error=auth_failed");
    }
}
