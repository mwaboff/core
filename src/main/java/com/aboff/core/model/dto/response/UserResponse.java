package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.aboff.core.model.enums.Role;

import java.time.LocalDateTime;

/**
 * DTO for user profile response.
 * Contains non-sensitive user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    /**
     * The user's unique identifier.
     */
    private Long id;

    /**
     * The user's username.
     */
    private String username;

    /**
     * The user's role (e.g., USER, MODERATOR, ADMIN, OWNER).
     */
    private Role role;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * The URL to the user's avatar image.
     */
    private String avatarUrl;

    /**
     * The user's timezone.
     */
    private String timezone;

    /**
     * The timestamp when the user account was created.
     */
    private LocalDateTime createdAt;

    /**
     * The timestamp when the user account was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * The timestamp when the user account was soft-deleted (privileged users only).
     */
    private LocalDateTime deletedAt;

    /**
     * The timestamp when the user was banned (privileged users only).
     */
    private LocalDateTime bannedAt;
}
