package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for an armor piece in a character's inventory.
 * <p>
 * Always includes the linking entity ID, armor ID, and equipped status.
 * The full armor object is included only when {@code ?expand=inventoryArmors} is specified.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryArmorResponse {

    /**
     * Unique identifier for this inventory armor linking entity
     */
    private Long id;

    /**
     * ID of the armor
     */
    private Long armorId;

    /**
     * Whether this armor is currently equipped
     */
    private Boolean equipped;

    /**
     * Full armor details (included only when {@code ?expand=inventoryArmors} is specified)
     */
    private ArmorResponse armor;
}
