package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for an armor piece in a character's inventory.
 * <p>
 * Represents an armor entry with equipped status. Multiple armor pieces
 * can be equipped simultaneously (e.g., body armor and amulets).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryArmorRequest {

    /**
     * ID of the armor to add to inventory
     */
    @NotNull(message = "Armor ID is required")
    private Long armorId;

    /**
     * Whether this armor is currently equipped (defaults to false)
     */
    @Builder.Default
    private Boolean equipped = false;
}
