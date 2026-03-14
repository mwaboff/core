package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.FeatureType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a feature in the Daggerheart TTRPG system.
 * <p>
 * Features are special abilities, traits, or bonuses that can be granted by
 * cards, classes, or other game elements. They are categorized by type
 * (HOPE, ANCESTRY, CLASS, COMMUNITY, DOMAIN, OTHER).
 * </p>
 */
@Entity
@Table(name = "features")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Feature extends BaseEntity {

    /**
     * The name of the feature.
     */
    @Column(length = 200)
    private String name;

    /**
     * Detailed description of what the feature does.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The type/category of this feature.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 20)
    private FeatureType featureType;

    /**
     * The expansion this feature belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * The cost/limitation tags associated with this feature.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "feature_card_cost_tags",
        joinColumns = @JoinColumn(name = "feature_id"),
        inverseJoinColumns = @JoinColumn(name = "card_cost_tag_id")
    )
    private Set<CardCostTag> costTags;

    /**
     * The modifiers associated with this feature that adjust character attributes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "feature_feature_modifiers",
        joinColumns = @JoinColumn(name = "feature_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_modifier_id")
    )
    private Set<FeatureModifier> modifiers = new HashSet<>();

    /**
     * Timestamp indicating when this feature was soft-deleted.
     * If null, the feature is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this feature has been soft-deleted.
     *
     * @return true if the feature is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the feature by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted feature.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
