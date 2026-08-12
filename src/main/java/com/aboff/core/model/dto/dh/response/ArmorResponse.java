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
public class ArmorResponse implements Restrictable {

    /**
     * Unique identifier for the armor.
     */
    private Long id;

    /**
     * Name of the armor.
     */
    private String name;

    /**
     * ID of the sourcebook this armor was published in (always included).
     * <p>
     * Null for custom items, which came from no book.
     * </p>
     */
    private Long expansionId;

    /**
     * Name of the expansion this armor belongs to (always included). On a redacted stub, this
     * is the only content-identifying field carried, so the frontend can tell the viewer which
     * book to buy without exposing the armor's real content.
     */
    private String expansionName;

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * view; every field but {@code id}, {@code restricted}, and {@code expansionName} is absent.
     */
    private Boolean restricted;

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
     * Whether this armor is SRD-licensed content, freely usable without owning the sourcebook
     * it was printed in.
     */
    private Boolean srd;

    /**
     * Whether this armor is visible to every user.
     */
    private Boolean isPublic;

    /**
     * ID of the user who authored this armor.
     * <p>
     * Null for official imports and for any row created before user authoring existed.
     * A non-null value alongside {@code isOfficial=false} is what marks a armor as homebrew.
     * </p>
     */
    private Long createdByUserId;

    /**
     * IDs of the campaigns this armor has been explicitly shared with.
     */
    private List<Long> campaignIds;

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
