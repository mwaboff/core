package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a martial stance in the Daggerheart TTRPG system (Hope & Fear).
 * <p>
 * Martial stances are modal combat states a character can shift into by spending Focus
 * (the "Stance Fighter" subsystem, Martial Artist Martial Stances sheet, p.184). A character
 * knows a fixed set of stances gated by tier and can be in exactly one stance at a time,
 * gaining that stance's effect until they take Severe damage, mark their last Hit Point, or
 * shift into a different stance.
 * </p>
 * <p>
 * This entity is the catalogue of stance texts only (name, tier, effect description) — it does
 * not track which stances a given character knows or which one is currently active. That
 * character-state linkage (known stances, active stance FK) is a separate concern owned by a
 * later packet.
 * </p>
 * <p>
 * Custom stances can be created by users as copies of official stances, with the
 * {@code originalMartialStance} field tracking the source stance.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.MARTIAL_STANCE)
@Table(name = "martial_stances")
@AssociationOverride(
    name = "features",
    joinTable = @JoinTable(
        name = "martial_stance_features",
        joinColumns = @JoinColumn(name = "martial_stance_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MartialStance extends BaseItem {

    /**
     * The effect text of this stance — what a character gains while shifted into it.
     * Can be quite short (a single bonus) or reference a mechanical trade-off (e.g. a penalty
     * to one stat in exchange for a bonus to another).
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Reference to the original official martial stance if this is a custom copy.
     * Null for official stances or entirely new custom stances.
     * Populated when a user creates a custom copy of an existing stance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_martial_stance_id")
    private MartialStance originalMartialStance;
}
