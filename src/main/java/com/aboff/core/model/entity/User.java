package com.aboff.core.model.entity;

import com.aboff.core.model.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_display_name", columnList = "display_name")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    @Column(name = "display_name", nullable = false, unique = true, length = 100)
    private String displayName;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserIntegration> integrations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoginHistory> loginHistory = new ArrayList<>();

    @OneToMany(mappedBy = "adminUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdminLog> adminActions = new ArrayList<>();

    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdminLog> adminActionsOnUser = new ArrayList<>();
}
