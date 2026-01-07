package com.aboff.core.model.entity;

import com.aboff.core.model.enums.OAuthProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_integrations", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_integration", columnNames = {"user_id", "provider"})
    },
    indexes = {
        @Index(name = "idx_user_integrations_provider_id", columnList = "provider_id")
    })
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UserIntegration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;

    @NotBlank(message = "Provider ID is required")
    @Size(max = 255, message = "Provider ID must not exceed 255 characters")
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Email(message = "Provider email should be valid")
    @Size(max = 255, message = "Provider email must not exceed 255 characters")
    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}
