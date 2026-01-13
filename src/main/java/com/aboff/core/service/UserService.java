package com.aboff.core.service;

import com.aboff.core.exception.InvalidPasswordException;
import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.dto.request.ChangePasswordRequest;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.util.CookieUtil;
import com.aboff.core.util.PasswordValidator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final CookieUtil cookieUtil;

    public UserService(
            UserRepository userRepository,
            AuthenticationService authenticationService,
            PasswordEncoder passwordEncoder,
            PasswordValidator passwordValidator,
            CookieUtil cookieUtil) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Gets the current authenticated user
     */
    public UserResponse getCurrentUser(Authentication authentication) {
        User user = extractUserFromAuthentication(authentication);
        return mapToUserResponse(user);
    }

    /**
     * Updates user profile information (email, avatarUrl, timezone)
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if email is being updated and is already taken by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already registered");
            }
            user.setEmail(request.getEmail());
        }

        // Update avatar URL if provided
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Update timezone if provided
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Changes user password and invalidates all existing tokens (force re-login on all devices)
     */
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request,
            HttpServletResponse response) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Validate new password strength
        passwordValidator.validatePassword(request.getNewPassword());

        // Hash and save new password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        // Invalidate ALL user's tokens (force re-login on all devices)
        authenticationService.invalidateAllUserTokens(userId);

        // Clear AUTH_TOKEN cookie for current session
        cookieUtil.clearAuthCookie(response);
    }

    /**
     * Soft deletes a user account
     */
    @Transactional
    public void deleteUser(Long userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete user
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all user's tokens
        authenticationService.invalidateAllUserTokens(userId);

        // Clear AUTH_TOKEN cookie
        cookieUtil.clearAuthCookie(response);
    }

    /**
     * Extracts User entity from Spring Security Authentication
     */
    private User extractUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UserNotFoundException("User not found");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Maps User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .createdAt(user.getCreatedAt())
                .lastModifiedAt(user.getLastModifiedAt())
                .build();
    }
}
