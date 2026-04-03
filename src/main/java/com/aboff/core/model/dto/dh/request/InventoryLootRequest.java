package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a loot item in a character's inventory.
 * <p>
 * Represents a loot entry in the character's inventory. Duplicate loot IDs
 * are allowed to represent multiple copies of the same item.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLootRequest {

    /**
     * ID of the loot item to add to inventory
     */
    @NotNull(message = "Loot ID is required")
    private Long lootId;
}
