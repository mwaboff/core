package com.aboff.core.security;

import com.aboff.core.model.entity.User;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.OAuth2UserProvisioningService;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OAuth2LoginSuccessHandler}.
 * Verifies redirect URLs for the popup-based OAuth flow.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    private static final String FRONTEND_BASE_URL = "http://localhost:4200";

    @Mock
    private OAuth2UserProvisioningService provisioningService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2AuthenticationToken authToken;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(
                provisioningService, authenticationService, cookieUtil, FRONTEND_BASE_URL);
        handler.setRedirectStrategy(redirectStrategy);

        when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authToken.getPrincipal()).thenReturn(oAuth2User);
    }

    @Test
    void onAuthenticationSuccess_UsernameChosen_RedirectsWithNeedsUsernameFalse() throws Exception {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .usernameChosen(true)
                .build();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);

        AuthenticationService.LoginResult loginResult = mock(AuthenticationService.LoginResult.class);
        when(loginResult.getToken()).thenReturn("jwt-token");
        when(authenticationService.issueToken(any(), any(), any(), any())).thenReturn(loginResult);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?needsUsername=false");
        verify(cookieUtil).setAuthCookie(response, "jwt-token");
    }

    @Test
    void onAuthenticationSuccess_UsernameNotChosen_RedirectsWithNeedsUsernameTrue() throws Exception {
        // Arrange
        User user = User.builder()
                .username("user-temp-123")
                .email("new@example.com")
                .usernameChosen(false)
                .build();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);

        AuthenticationService.LoginResult loginResult = mock(AuthenticationService.LoginResult.class);
        when(loginResult.getToken()).thenReturn("jwt-token");
        when(authenticationService.issueToken(any(), any(), any(), any())).thenReturn(loginResult);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?needsUsername=true");
    }

    @Test
    void onAuthenticationSuccess_UsernameChosenNull_RedirectsWithNeedsUsernameTrue() throws Exception {
        // Arrange - usernameChosen is null (not set), should be treated as needing username
        User user = User.builder()
                .username("user-temp-456")
                .email("null@example.com")
                .build();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);

        AuthenticationService.LoginResult loginResult = mock(AuthenticationService.LoginResult.class);
        when(loginResult.getToken()).thenReturn("jwt-token");
        when(authenticationService.issueToken(any(), any(), any(), any())).thenReturn(loginResult);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?needsUsername=true");
    }

    @Test
    void onAuthenticationSuccess_BannedUser_RedirectsToCallbackWithBannedError() throws Exception {
        // Arrange
        User user = User.builder()
                .username("banneduser")
                .email("banned@example.com")
                .build();
        user.ban();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?error=banned");
        verify(authenticationService, never()).issueToken(any(), any(), any(), any());
    }

    @Test
    void onAuthenticationSuccess_DeletedUser_RedirectsToCallbackWithBannedError() throws Exception {
        // Arrange
        User user = User.builder()
                .username("deleteduser")
                .email("deleted@example.com")
                .build();
        user.softDelete();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(FRONTEND_BASE_URL + "/auth/callback?error=banned");
        verify(authenticationService, never()).issueToken(any(), any(), any(), any());
    }

    @Test
    void onAuthenticationSuccess_WithXForwardedFor_UsesFirstIpFromHeader() throws Exception {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .usernameChosen(true)
                .build();
        when(provisioningService.findOrCreateUserFromOAuth2("google", oAuth2User)).thenReturn(user);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");

        AuthenticationService.LoginResult loginResult = mock(AuthenticationService.LoginResult.class);
        when(loginResult.getToken()).thenReturn("jwt-token");
        when(authenticationService.issueToken(any(), any(), any(), anyString())).thenReturn(loginResult);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert - IP extracted from X-Forwarded-For
        verify(authenticationService).issueToken(eq(user), eq("google"), any(), eq("1.2.3.4"));
    }
}
