package com.aboff.core.model.entity.dh;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing armor in the Daggerheart TTRPG system.
 * <p>
 * Armor protects characters from damage and defines thresholds for different
 * levels of injury severity. The armor system uses three key values:
 * </p>
 * <ul>
 *   <li><strong>Base Score:</strong> The armor's defensive value, added to defensive rolls</li>
 *   <li><strong>Major Threshold:</strong> Damage required to inflict a major injury</li>
 *   <li><strong>Severe Threshold:</strong> Damage required to inflict a severe injury</li>
 * </ul>
 * <p>
 * When a character takes damage, the amount is compared to these thresholds to
 * determine the severity of any wounds sustained. The severe threshold must
 * always be greater than or equal to the major threshold.
 * </p>
 * <p>
 * Custom armor can be created by users as copies of official armor,
 * with the {@code originalArmor} field tracking the source armor.
 * </p>
 */
@Entity
@Table(name = "armors")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Armor extends BaseItem {

    /**
     * The minimum damage required to inflict a major injury on the wearer.
     * Damage at or above this threshold (but below severe threshold) results in major wounds.
     * Must be a positive value.
     */
    @Column(name = "base_major_threshold", nullable = false)
    private Integer baseMajorThreshold;

    /**
     * The minimum damage required to inflict a severe injury on the wearer.
     * Damage at or above this threshold results in severe wounds.
     * Must be a positive value and greater than or equal to the major threshold.
     */
    @Column(name = "base_severe_threshold", nullable = false)
    private Integer baseSevereThreshold;

    /**
     * The armor's base defensive score.
     * This value is added to defensive rolls to help prevent or reduce damage.
     */
    @Column(name = "base_score", nullable = false)
    private Integer baseScore;

    /**
     * Optional special feature granted by this armor.
     * Examples might include special abilities, resistances, or unique protective effects.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id")
    private Feature feature;

    /**
     * Reference to the original official armor if this is a custom armor.
     * Null for official armor, populated when a user creates a custom copy.
     * This allows tracking the source of custom content and maintaining relationships.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_armor_id")
    private Armor originalArmor;
}
