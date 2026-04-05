package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a loot item in a character's inventory.
 * <p>
 * Always includes the linking entity ID and loot ID.
 * The full loot object is included only when {@code ?expand=inventoryItems} is specified.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryLootResponse {

    /**
     * Unique identifier for this inventory loot linking entity
     */
    private Long id;

    /**
     * ID of the loot item
     */
    private Long lootId;

    /**
     * Full loot details (included only when {@code ?expand=inventoryItems} is specified)
     */
    private LootResponse loot;
}
