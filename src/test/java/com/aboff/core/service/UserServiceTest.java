package com.aboff.core.service;

import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private AuthenticationService authenticationService;

        @Mock
        private RoleHierarchyService roleHierarchyService;

        @Mock
        private CookieUtil cookieUtil;

        @Mock
        private Authentication authentication;

        @Mock
        private HttpServletResponse response;

        @InjectMocks
        private UserService userService;

        // ==================== GET CURRENT USER TESTS ====================

        @Test
        void getCurrentUser_Authenticated_ReturnsUserResponse() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();

                CustomUserDetails userDetails = new CustomUserDetails(user);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));

                // Act
                UserResponse result = userService.getCurrentUser(authentication);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getUsername()).isEqualTo("testuser");
                assertThat(result.getEmail()).isEqualTo("test@example.com");
                assertThat(result.getRole()).isEqualTo(com.aboff.core.model.enums.Role.USER);
        }

        @Test
        void getCurrentUser_NullAuthentication_ThrowsUserNotFoundException() {
                // Act & Assert
                assertThatThrownBy(() -> userService.getCurrentUser(null))
                                .isInstanceOf(UserNotFoundException.class)
                                .hasMessage("User not found");
        }

        @Test
        void getCurrentUser_UserNotInDatabase_ThrowsUserNotFoundException() {
                // Arrange
                User user = User.builder().id(1L).build();
                CustomUserDetails userDetails = new CustomUserDetails(user);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> userService.getCurrentUser(authentication))
                                .isInstanceOf(UserNotFoundException.class)
                                .hasMessage("User not found");
        }

        // ==================== GET USER PROFILE TESTS ====================

        @Test
        void getUserProfile_AsOwner_ReturnsFullInfo() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .role(com.aboff.core.model.enums.Role.USER)
                                .createdAt(LocalDateTime.now())
                                .build();

                CustomUserDetails userDetails = new CustomUserDetails(user);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));

                // Act
                UserResponse result = userService.getUserProfile("1", authentication);

                // Assert
                assertThat(result.getEmail()).isEqualTo("test@example.com");
                assertThat(result.getUsername()).isEqualTo("testuser");
                assertThat(result.getRole()).isEqualTo(com.aboff.core.model.enums.Role.USER);
        }

        @Test
        void getUserProfile_AsMeAlias_ReturnsFullInfo() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .role(com.aboff.core.model.enums.Role.USER)
                                .createdAt(LocalDateTime.now())
                                .build();

                CustomUserDetails userDetails = new CustomUserDetails(user);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));

                // Act
                UserResponse result = userService.getUserProfile("me", authentication);

                // Assert
                assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        void getUserProfile_AsOtherUser_ReturnsLimitedInfo() {
                // Arrange
                User currentUser = User.builder().id(1L).role(com.aboff.core.model.enums.Role.USER).build();
                User targetUser = User.builder()
                                .id(2L)
                                .username("otheruser")
                                .email("other@example.com")
                                .avatarUrl("https://other.avatar")
                                .createdAt(LocalDateTime.now())
                                .build();

                CustomUserDetails userDetails = new CustomUserDetails(currentUser);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
                when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
                when(roleHierarchyService.isPrivilegedRole(any())).thenReturn(false);

                // Act
                UserResponse result = userService.getUserProfile("2", authentication);

                // Assert
                assertThat(result.getUsername()).isEqualTo("otheruser");
                assertThat(result.getAvatarUrl()).isEqualTo("https://other.avatar");
                assertThat(result.getEmail()).isNull(); // Limited info
                assertThat(result.getTimezone()).isNull();
        }

        @Test
        void getUserProfile_AsPrivilegedUser_ReturnsFullInfo() {
                // Arrange
                User moderator = User.builder().id(1L).role(com.aboff.core.model.enums.Role.MODERATOR).build();
                User targetUser = User.builder()
                                .id(2L)
                                .username("otheruser")
                                .email("other@example.com")
                                .createdAt(LocalDateTime.now())
                                .build();

                CustomUserDetails userDetails = new CustomUserDetails(moderator);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(moderator));
                when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
                when(roleHierarchyService.isPrivilegedRole(com.aboff.core.model.enums.Role.MODERATOR)).thenReturn(true);

                // Act
                UserResponse result = userService.getUserProfile("2", authentication);

                // Assert
                assertThat(result.getEmail()).isEqualTo("other@example.com");
                assertThat(result.getUsername()).isEqualTo("otheruser");
        }

        @Test
        void getUserProfile_NonExistentUser_ThrowsUserNotFoundException() {
                // Arrange
                User user = User.builder().id(1L).build();
                CustomUserDetails userDetails = new CustomUserDetails(user);
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> userService.getUserProfile("999", authentication))
                                .isInstanceOf(UserNotFoundException.class)
                                .hasMessage("User not found");
        }

        // ==================== UPDATE USER TESTS ====================

        @Test
        void updateUser_ValidData_UpdatesUser() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("old@example.com")
                                .avatarUrl("https://old.avatar")
                                .timezone("UTC")
                                .build();

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("new@example.com")
                                .avatarUrl("https://new.avatar")
                                .timezone("America/New_York")
                                .build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                UserResponse result = userService.updateUser(1L, request);

                // Assert
                assertThat(result.getEmail()).isEqualTo("new@example.com");
                assertThat(result.getAvatarUrl()).isEqualTo("https://new.avatar");
                assertThat(result.getTimezone()).isEqualTo("America/New_York");

                verify(userRepository).save(argThat(u -> u.getEmail().equals("new@example.com") &&
                                u.getAvatarUrl().equals("https://new.avatar") &&
                                u.getTimezone().equals("America/New_York")));
        }

        @Test
        void updateUser_DuplicateEmail_ThrowsUserAlreadyExistsException() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("old@example.com")
                                .build();

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("existing@example.com")
                                .build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> userService.updateUser(1L, request))
                                .isInstanceOf(UserAlreadyExistsException.class)
                                .hasMessage("Email already registered");

                verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_SameEmail_AllowsUpdate() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("test@example.com") // Same email
                                .avatarUrl("https://new.avatar")
                                .build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                UserResponse result = userService.updateUser(1L, request);

                // Assert
                assertThat(result.getEmail()).isEqualTo("test@example.com");
                assertThat(result.getAvatarUrl()).isEqualTo("https://new.avatar");

                // Should not check existsByEmailIgnoreCase since email didn't change
                verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
                verify(userRepository).save(any(User.class));
        }

        @Test
        void updateUser_UserNotFound_ThrowsUserNotFoundException() {
                // Arrange
                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("new@example.com")
                                .build();

                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> userService.updateUser(999L, request))
                                .isInstanceOf(UserNotFoundException.class)
                                .hasMessage("User not found");
        }

        @Test
        void updateUser_PartialUpdate_UpdatesOnlyProvidedFields() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .avatarUrl("https://old.avatar")
                                .timezone("UTC")
                                .build();

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .timezone("America/New_York") // Only update timezone
                                .build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                UserResponse result = userService.updateUser(1L, request);

                // Assert
                assertThat(result.getTimezone()).isEqualTo("America/New_York");

                verify(userRepository).save(argThat(u -> u.getEmail().equals("test@example.com") && // Email unchanged
                                u.getAvatarUrl().equals("https://old.avatar") && // Avatar unchanged
                                u.getTimezone().equals("America/New_York") // Timezone updated
                ));
        }

        // ==================== DELETE USER TESTS ====================

        @Test
        void deleteUser_Success_SoftDeletesAndInvalidatesTokens() {
                // Arrange
                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));

                // Act
                userService.deleteUser(1L, response);

                // Assert
                verify(userRepository).save(argThat(u -> u.getDeletedAt() != null));
                verify(authenticationService).invalidateAllUserTokens(1L);
                verify(cookieUtil).clearAuthCookie(response);
        }

        @Test
        void deleteUser_UserNotFound_ThrowsUserNotFoundException() {
                // Arrange
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> userService.deleteUser(999L, response))
                                .isInstanceOf(UserNotFoundException.class)
                                .hasMessage("User not found");

                verify(authenticationService, never()).invalidateAllUserTokens(anyLong());
                verify(cookieUtil, never()).clearAuthCookie(any());
        }
}
