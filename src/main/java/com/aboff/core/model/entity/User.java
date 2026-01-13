package com.aboff.core.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 50)
    private String timezone;

    @Column(length = 50)
    private String pronouns;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this user has been soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the user by setting the deleted_at timestamp
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted user
     */
    public void restore() {
        this.deletedAt = null;
    }
}
