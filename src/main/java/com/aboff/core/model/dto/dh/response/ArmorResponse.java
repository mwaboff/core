package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Armor entities.
 * Represents armor in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId, featureIds, originalArmorId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>With ?expand=originalArmor: includes full original armor object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArmorResponse {

    /**
     * Unique identifier for the armor.
     */
    private Long id;

    /**
     * Name of the armor.
     */
    private String name;

    /**
     * ID of the expansion this armor belongs to (always included).
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * The tier level of the armor (1–4).
     */
    private Integer tier;

    /**
     * Whether this armor is from official game content.
     */
    private Boolean isOfficial;

    /**
     * The minimum damage required to inflict a major injury.
     */
    private Integer baseMajorThreshold;

    /**
     * The minimum damage required to inflict a severe injury.
     */
    private Integer baseSevereThreshold;

    /**
     * The armor's base defensive score.
     */
    private Integer baseScore;

    /**
     * ID of the user who created this armor (always included; null for official armors).
     */
    private Long creatorId;

    /**
     * IDs of the features associated with this armor (null if none).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * ID of the original armor if this is a custom copy (null if original).
     */
    private Long originalArmorId;

    /**
     * Full original armor object (included only when ?expand=originalArmor is specified).
     */
    private ArmorResponse originalArmor;

    /**
     * Timestamp when the armor was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the armor was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the armor was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;
}
