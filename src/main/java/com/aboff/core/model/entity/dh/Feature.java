package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.FeatureTiming;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
 * (see {@link FeatureType}).
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.FEATURE)
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
     * The timing tag for this feature (e.g. Action, Reaction), as printed as part of
     * the feature heading in the source material. Null when the feature has no timing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "timing", length = 20)
    private FeatureTiming timing;

    /**
     * The sourcebook this feature was published in.
     * <p>
     * Null for features authored by users alongside their custom items, which came
     * from no book.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id")
    private Expansion expansion;

    /**
     * The user who authored this feature.
     * <p>
     * Null for official content and for features created before user authoring
     * existed. Populated when a user creates a feature inline on a custom item.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Indicates whether this feature is from official game content, mirroring how
     * {@link Card} declares the same flag. A feature has no official/custom distinction of
     * its own — it is derived by {@code FeatureService} from whatever it is attached to (a
     * card, a class, an item, ...) and is never settable from a request DTO.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * Indicates whether this feature is SRD-licensed content, freely usable without owning
     * the sourcebook it was printed in. Defaults to false at creation time; only an explicit
     * SRD flag opens the feature to users who have not been granted expansion access. See
     * {@code ContentAccessService} for how this is enforced.
     */
    @Column(name = "srd", nullable = false)
    @Builder.Default
    private Boolean srd = false;

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
