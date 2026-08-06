package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for a user creating their own loot item or consumable.
 * <p>
 * Separate from {@link CreateLootRequest} for the reasons documented on
 * {@link CreateCustomWeaponRequest}: that type serves the strict admin import pipeline, this
 * one serves user authoring, and neither should be able to stand in for the other.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomLootRequest {

    /**
     * Name of the loot item.
     */
    @NotBlank(message = "Loot name is required")
    @Size(max = 200, message = "Loot name must not exceed 200 characters")
    private String name;

    /**
     * The rarity of the loot (1=Common, 2=Uncommon, 3=Rare, 4=Legendary).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this loot should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Campaigns to share this loot with. The creator must be involved in each campaign
     * they name.
     */
    private List<Long> campaignIds;

    /**
     * Whether this item is consumed on use.
     */
    @NotNull(message = "isConsumable is required")
    private Boolean isConsumable;

    /**
     * What the item is and what it does.
     */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * Features granted by this loot, created inline. Capped as a runaway guard — see
     * {@link CreateCustomWeaponRequest#getFeatures()}.
     */
    @Valid
    @Size(max = 20, message = "Loot may not have more than 20 features")
    private List<FeatureInput> features;
}
