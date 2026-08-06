package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Loot item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLootRequest {

    /**
     * Name of the loot item.
     */
    @Size(max = 200, message = "Loot name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this loot belongs to.
     */
    private Long expansionId;

    /**
     * The tier level of the loot (1–4), representing rarity: 1=Common, 2=Uncommon, 3=Rare, 4=Legendary.
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this loot is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this loot should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Clears the expansion, marking the loot as belonging to no sourcebook.
     * <p>
     * A JSON {@code null} for {@code expansionId} is indistinguishable from the field being
     * omitted, and omitted means "leave unchanged". This flag is the only way to actually
     * remove an expansion.
     * </p>
     */
    private Boolean clearExpansion;

    /**
     * Campaigns to share this loot with, replacing any existing tags. Null leaves tags
     * untouched; an empty list removes them all.
     */
    private List<Long> campaignIds;

    /**
     * Whether this loot item is consumable (e.g., potions, scrolls, food).
     */
    private Boolean isConsumable;

    /**
     * Optional description of the loot item.
     */
    private String description;

    /**
     * Optional list of existing feature IDs to associate with this loot.
     */
    private List<Long> featureIds;

    /**
     * Optional list of features to find or create inline.
     */
    @Valid
    private List<FeatureInput> features;

}
