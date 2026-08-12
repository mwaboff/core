package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Loot entities.
 * Represents loot items in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId, featureIds, originalLootId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>With ?expand=originalLoot: includes full original loot object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LootResponse implements Restrictable {

    /**
     * Unique identifier for the loot.
     */
    private Long id;

    /**
     * Name of the loot item.
     */
    private String name;

    /**
     * ID of the sourcebook this loot was published in (always included).
     * <p>
     * Null for custom items, which came from no book.
     * </p>
     */
    private Long expansionId;

    /**
     * Name of the expansion this loot belongs to (always included). On a redacted stub, this
     * is the only content-identifying field carried, so the frontend can tell the viewer which
     * book to buy without exposing the loot's real content.
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
     * The tier level of the loot (1–4), representing rarity.
     */
    private Integer tier;

    /**
     * Whether this loot is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this loot is SRD-licensed content, freely usable without owning the sourcebook
     * it was printed in.
     */
    private Boolean srd;

    /**
     * Whether this loot is visible to every user.
     */
    private Boolean isPublic;

    /**
     * ID of the user who authored this loot.
     * <p>
     * Null for official imports and for any row created before user authoring existed.
     * A non-null value alongside {@code isOfficial=false} is what marks a loot as homebrew.
     * </p>
     */
    private Long createdByUserId;

    /**
     * IDs of the campaigns this loot has been explicitly shared with.
     */
    private List<Long> campaignIds;

    /**
     * Whether this loot item is consumable.
     */
    private Boolean isConsumable;

    /**
     * Description of the loot item.
     */
    private String description;

    /**
     * IDs of features granted by this loot (always included when present).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * ID of the original loot if this is a custom copy (null if original).
     */
    private Long originalLootId;

    /**
     * Full original loot object (included only when ?expand=originalLoot is specified).
     */
    private LootResponse originalLoot;

    /**
     * Timestamp when the loot was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the loot was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the loot was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;
}
