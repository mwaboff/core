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
 * Request DTO for creating a new Loot item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLootRequest {

    /**
     * Name of the loot item.
     */
    @NotBlank(message = "Loot name is required")
    @Size(max = 200, message = "Loot name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this loot belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * The tier level of the loot (1–4), representing rarity: 1=Common, 2=Uncommon, 3=Rare, 4=Legendary.
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this loot is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this loot is SRD-licensed content, freely usable without owning the sourcebook
     * it was printed in. Optional and honoured only for ADMIN+ — see
     * {@code ContentAccessService#resolveSrd}. Omitted by existing bulk-import payloads, which
     * must keep working, so this is never required.
     */
    private Boolean srd;

    /**
     * Whether this loot item is consumable (e.g., potions, scrolls, food).
     */
    @NotNull(message = "isConsumable is required")
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

    /**
     * Optional ID of the original loot if this is a custom copy.
     */
    private Long originalLootId;
}
