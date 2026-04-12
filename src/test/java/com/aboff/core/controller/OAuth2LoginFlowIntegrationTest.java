package com.aboff.core.controller;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIdentity;
import com.aboff.core.repository.UserIdentityRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.service.OAuth2UserProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the OAuth2 login flow, covering user provisioning logic
 * and security configuration of OAuth2 endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class OAuth2LoginFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2UserProvisioningService provisioningService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    // ==================== PROVISIONING SERVICE TESTS ====================

    @Test
    void findOrCreateUser_NewGoogleUser_CreatesUserAndIdentity() {
        // Arrange
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("sub")).thenReturn("google-12345");
        when(mockOAuth2User.getAttribute("email")).thenReturn("newuser@gmail.com");
        when(mockOAuth2User.getAttribute("name")).thenReturn("New User");
        when(mockOAuth2User.getAttribute("picture")).thenReturn("https://photo.url/pic.jpg");

        // Act
        User user = provisioningService.findOrCreateUserFromOAuth2("google", mockOAuth2User);

        // Assert - user created with correct details
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("newuser@gmail.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://photo.url/pic.jpg");

        // Assert - identity created with correct provider coordinates
        Optional<UserIdentity> identity = userIdentityRepository.findByProviderAndProviderSub("google", "google-12345");
        assertThat(identity).isPresent();
        assertThat(identity.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(identity.get().getDisplayName()).isEqualTo("New User");
        assertThat(identity.get().getLastUsedAt()).isNotNull();
    }

    @Test
    void findOrCreateUser_ExistingGoogleUser_ReturnsExistingUserAndUpdatesLastUsedAt() {
        // Arrange - pre-create a user and identity
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@gmail.com")
                .avatarUrl("https://old.avatar.url")
                .timezone("UTC")
                .build();
        existingUser = userRepository.save(existingUser);

        UserIdentity existingIdentity = UserIdentity.builder()
                .user(existingUser)
                .provider("google")
                .providerSub("google-existing-sub")
                .email("existing@gmail.com")
                .displayName("Existing User")
                .build();
        userIdentityRepository.save(existingIdentity);

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("sub")).thenReturn("google-existing-sub");
        when(mockOAuth2User.getAttribute("email")).thenReturn("existing@gmail.com");
        when(mockOAuth2User.getAttribute("name")).thenReturn("Existing User");
        when(mockOAuth2User.getAttribute("picture")).thenReturn("https://photo.url/pic.jpg");

        // Act
        User returnedUser = provisioningService.findOrCreateUserFromOAuth2("google", mockOAuth2User);

        // Assert - same user returned, not a new one
        assertThat(returnedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(returnedUser.getEmail()).isEqualTo("existing@gmail.com");

        // Assert - lastUsedAt was updated
        UserIdentity updatedIdentity = userIdentityRepository
                .findByProviderAndProviderSub("google", "google-existing-sub")
                .orElseThrow();
        assertThat(updatedIdentity.getLastUsedAt()).isNotNull();
    }

    @Test
    void findOrCreateUser_UsernameCollision_AppendsSubSuffix() {
        // Arrange - pre-create a user with the username that would be generated from the email
        User existingUser = User.builder()
                .username("collision")
                .email("other@example.com")
                .avatarUrl("https://avatar.url")
                .timezone("UTC")
                .build();
        userRepository.save(existingUser);

        // The email local-part "collision" is already taken, so a suffix should be appended
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("sub")).thenReturn("sub-abc123xyz");
        when(mockOAuth2User.getAttribute("email")).thenReturn("collision@gmail.com");
        when(mockOAuth2User.getAttribute("name")).thenReturn("Collision User");
        when(mockOAuth2User.getAttribute("picture")).thenReturn(null);

        // Act
        User newUser = provisioningService.findOrCreateUserFromOAuth2("google", mockOAuth2User);

        // Assert - username differs from the taken one and contains the sub suffix
        assertThat(newUser.getUsername()).isNotEqualTo("collision");
        assertThat(newUser.getUsername()).startsWith("collision-");
    }

    // ==================== SECURITY CONFIG TESTS ====================

    @Test
    void oauth2AuthorizationEndpoint_IsAccessible_NotBlocked() throws Exception {
        // The /oauth2/authorization/* path is permitted by SecurityConfig.
        // Spring Security will redirect (302) toward the OAuth provider's auth URL
        // rather than rejecting with 401.
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }
}
