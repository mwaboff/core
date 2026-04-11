package com.aboff.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * JPA entity representing a row in the {@code search_index} table.
 * <p>
 * Each row mirrors a single searchable game-content entity and stores denormalized data
 * that supports full-text search (via PostgreSQL {@code tsvector}) as well as a set of
 * filter columns that allow callers to narrow results by entity-specific attributes such
 * as tier, domain, card type, and more.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Extends {@link BaseEntity} to inherit {@code id}, {@code createdAt}, and
 *       {@code lastModifiedAt} fields.</li>
 *   <li>The {@code searchVector} column is a PostgreSQL {@code TSVECTOR}. JPA has no
 *       native TSVECTOR type, so it is mapped as a {@code String}. Writes are handled
 *       via a database trigger or raw SQL; this field is read-only from the JPA layer.</li>
 *   <li>The {@code deletedAt} field mirrors the soft-deletion state of the referenced
 *       entity. When an entity is soft-deleted, its search index row is also marked with
 *       a {@code deletedAt} timestamp so it is excluded from search results.</li>
 *   <li>All filter columns are nullable because they are only relevant for a subset of
 *       entity types.</li>
 * </ul>
 */
@Entity
@Table(name = "search_index")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIndex extends BaseEntity {

    /**
     * The type of the referenced entity (e.g., "WEAPON", "DOMAIN_CARD").
     * Stored as a plain string matching the {@link com.aboff.core.model.enums.SearchableEntityType} name.
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * The primary key of the referenced entity in its own table.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * The display name of the entity, used for result labeling and keyword matching.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Pre-computed PostgreSQL {@code TSVECTOR} for full-text search.
     * This column is populated by a database trigger and is treated as read-only by the application.
     */
    @Column(name = "search_vector", columnDefinition = "tsvector")
    private String searchVector;

    // -------------------------------------------------------------------------
    // Filter columns — nullable; only applicable to a subset of entity types
    // -------------------------------------------------------------------------

    /**
     * Numeric tier level of the entity (e.g., card tier 1–4).
     * Applicable to cards and some other content types.
     */
    @Column(name = "tier")
    private Integer tier;

    /**
     * Foreign key reference to the expansion that introduced this entity.
     */
    @Column(name = "expansion_id")
    private Long expansionId;

    /**
     * Whether this entity is official (published by the game publisher).
     * {@code null} for entity types that do not use this flag.
     */
    @Column(name = "is_official")
    private Boolean isOfficial;

    /**
     * Whether this entity is visible to all users.
     * {@code null} for entity types that do not use this flag.
     */
    @Column(name = "is_public")
    private Boolean isPublic;

    /**
     * The user ID of the user who created this entity.
     * {@code null} for official/system-created content.
     */
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    /**
     * The card type discriminator (e.g., "ANCESTRY", "COMMUNITY", "DOMAIN", "SUBCLASS").
     * Applicable to card entity types only.
     */
    @Column(name = "card_type", length = 50)
    private String cardType;

    /**
     * The feature type (e.g., "CLASS_FEATURE", "SUBCLASS_FEATURE").
     * Applicable to feature entity types only.
     */
    @Column(name = "feature_type", length = 50)
    private String featureType;

    /**
     * The adversary type/role (e.g., "MINION", "BRUISER", "LEADER").
     * Applicable to adversary entity types only.
     */
    @Column(name = "adversary_type", length = 50)
    private String adversaryType;

    /**
     * The domain card type (e.g., "ABILITY", "SPELL", "REACTION").
     * Applicable to domain card entity types only.
     */
    @Column(name = "domain_card_type", length = 50)
    private String domainCardType;

    /**
     * Foreign key reference to the associated domain for this entity.
     * Applicable to domain cards and similar domain-scoped entities.
     */
    @Column(name = "associated_domain_id")
    private Long associatedDomainId;

    /**
     * The primary trait associated with this entity (e.g., "AGILITY", "STRENGTH").
     * Stored as the enum name string.
     */
    @Column(name = "trait", length = 50)
    private String trait;

    /**
     * The range category of this entity (e.g., "MELEE", "RANGED", "FAR").
     * Applicable to weapons and some adversary attacks.
     */
    @Column(name = "range", length = 50)
    private String range;

    /**
     * The burden (encumbrance category) of this entity (e.g., "ONE_HANDED", "TWO_HANDED").
     * Applicable to items such as weapons and armor.
     */
    @Column(name = "burden", length = 50)
    private String burden;

    /**
     * Whether this entity is a primary item or ability.
     * Applicable where a primary/secondary distinction exists (e.g., primary weapons).
     */
    @Column(name = "is_primary")
    private Boolean isPrimary;

    /**
     * The damage type of this entity (e.g., "PHYSICAL", "MAGIC").
     * Applicable to weapons and adversary attacks.
     */
    @Column(name = "damage_type", length = 50)
    private String damageType;

    /**
     * Whether this entity is consumable (single-use item).
     * Applicable to loot and item entity types.
     */
    @Column(name = "is_consumable")
    private Boolean isConsumable;

    /**
     * Whether this entity has a mixed content type or multi-type classification.
     */
    @Column(name = "is_mixed")
    private Boolean isMixed;

    /**
     * The subclass level at which this entity is unlocked (e.g., "FOUNDATION", "SPECIALIZATION", "MASTERY").
     * Applicable to subclass path features.
     */
    @Column(name = "subclass_level", length = 50)
    private String subclassLevel;

    /**
     * The cost tag category (e.g., "ACTION", "REACTION", "PASSIVE").
     * Applicable to card cost tag entity types.
     */
    @Column(name = "cost_tag_category", length = 50)
    private String costTagCategory;

    /**
     * Soft-deletion timestamp.
     * When non-null, this index row mirrors the soft-deletion of the referenced entity
     * and must be excluded from search results.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this search index entry has been soft-deleted.
     *
     * @return {@code true} if {@code deletedAt} is set, {@code false} otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Marks this search index entry as soft-deleted by setting {@code deletedAt} to the current time.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores this search index entry by clearing the {@code deletedAt} timestamp.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
