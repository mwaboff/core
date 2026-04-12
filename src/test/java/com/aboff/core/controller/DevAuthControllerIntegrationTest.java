package com.aboff.core.controller;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIdentity;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.UserIdentityRepository;
import com.aboff.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link DevAuthController}.
 * <p>
 * Activates the {@code dev} Spring profile so that the dev-login endpoint is registered.
 * Verifies user creation, role assignment, user reuse, and cookie issuance.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("dev")
@Transactional
class DevAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Test
    void devLogin_ValidEmail_Returns200AndSetsCookieAndCreatesUser() throws Exception {
        // Act
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"devtest@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andExpect(jsonPath("$.email").value("devtest@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Verify user and identity were created
        assertThat(userRepository.findByEmailIgnoreCase("devtest@example.com")).isPresent();
        Optional<UserIdentity> identity = userIdentityRepository.findByProviderAndProviderSub("dev", "devtest@example.com");
        assertThat(identity).isPresent();
        assertThat(identity.get().getProvider()).isEqualTo("dev");
    }

    @Test
    void devLogin_WithRole_CreatesUserWithSpecifiedRole() throws Exception {
        // Act
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Verify role persisted
        assertThat(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void devLogin_ExistingUser_ReusesUser() throws Exception {
        // Arrange - pre-create the user and dev identity to simulate a prior login
        User existingUser = User.builder()
                .username("reuseuser")
                .email("reuse@example.com")
                .avatarUrl("https://avatar.url")
                .timezone("UTC")
                .build();
        existingUser = userRepository.save(existingUser);
        Long existingUserId = existingUser.getId();

        UserIdentity existingIdentity = UserIdentity.builder()
                .user(existingUser)
                .provider("dev")
                .providerSub("reuse@example.com")
                .email("reuse@example.com")
                .displayName("reuseuser")
                .linkedAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();
        userIdentityRepository.save(existingIdentity);

        // Act - devLogin should find and return the existing user, not create a new one
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reuse@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingUserId))
                .andExpect(jsonPath("$.email").value("reuse@example.com"));

        // Verify no duplicate user or identity was created
        assertThat(userRepository.findByEmailIgnoreCase("reuse@example.com")).isPresent();
        assertThat(userIdentityRepository.findByProviderAndProviderSub("dev", "reuse@example.com")).isPresent();
    }

    @Test
    void devLogin_NullEmail_Returns5xxError() throws Exception {
        // Act — omitting email causes NPE in provisioning service
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void devLogin_WithExplicitUsername_SetsUsernameAndUsernameChosenTrue() throws Exception {
        // Act
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nameduser@example.com\",\"username\":\"myname\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("myname"))
                .andExpect(jsonPath("$.usernameChosen").value(true));

        // Verify persisted
        assertThat(userRepository.findByEmailIgnoreCase("nameduser@example.com"))
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getUsername()).isEqualTo("myname");
                    assertThat(u.getUsernameChosen()).isTrue();
                });
    }

    @Test
    void devLogin_WithoutUsername_Returns200WithUsernameChosenTrue() throws Exception {
        // Act — omitting username should still produce usernameChosen=true for dev users
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"defaultuser@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usernameChosen").value(true));

        // Verify persisted
        assertThat(userRepository.findByEmailIgnoreCase("defaultuser@example.com"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getUsernameChosen()).isTrue());
    }
}
