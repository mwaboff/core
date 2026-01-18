package com.aboff.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Abstract base entity class that provides common fields for all JPA entities.
 * <p>
 * This class should be extended by all entity classes in the application to ensure
 * consistent ID generation and automatic timestamp tracking.
 * </p>
 *
 * <h2>Usage Pattern</h2>
 * <pre>
 * {@code
 * @Entity
 * @Table(name = "table_name")
 * @Data
 * @Builder
 * @NoArgsConstructor
 * @AllArgsConstructor
 * public class MyEntity extends BaseEntity {
 *     // Do NOT define: id, createdAt, lastModifiedAt (inherited from BaseEntity)
 *     // Define only domain-specific fields
 *     private String name;
 *     private String description;
 * }
 * }
 * </pre>
 *
 * <h2>Provided Fields</h2>
 * <ul>
 *   <li><strong>id</strong> - Auto-generated primary key using database identity strategy</li>
 *   <li><strong>createdAt</strong> - Timestamp automatically set when entity is first persisted</li>
 *   <li><strong>lastModifiedAt</strong> - Timestamp automatically updated on every entity modification</li>
 * </ul>
 *
 * <h2>Lombok Compatibility</h2>
 * <p>
 * This class uses {@code @Getter} and {@code @Setter} instead of {@code @Data} to avoid
 * conflicts with Lombok's {@code @Builder} pattern in child classes. Child entities can
 * safely use {@code @Data} and {@code @Builder}, which will include inherited fields
 * automatically.
 * </p>
 *
 * <h2>Database Mapping</h2>
 * <p>
 * Uses {@code @MappedSuperclass} annotation, so fields are mapped to columns in the
 * child entity's table. This class itself does not create a separate database table.
 * </p>
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    /**
     * Primary key identifier for the entity.
     * <p>
     * Uses database identity generation strategy for auto-incrementing values.
     * This field is automatically populated by the database upon insert.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp indicating when the entity was first created.
     * <p>
     * This field is automatically set by Hibernate when the entity is first
     * persisted to the database. It is mapped to the {@code created_at} column
     * in the database table.
     * </p>
     * <p>
     * The timestamp is immutable after creation and will not be updated on
     * subsequent modifications to the entity.
     * </p>
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the entity was last modified.
     * <p>
     * This field is automatically updated by Hibernate whenever the entity is
     * modified and persisted to the database. It is mapped to the
     * {@code last_modified_at} column in the database table.
     * </p>
     * <p>
     * On initial creation, this field is set to the same value as {@code createdAt}.
     * It is then updated automatically on every subsequent modification.
     * </p>
     */
    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;
}
