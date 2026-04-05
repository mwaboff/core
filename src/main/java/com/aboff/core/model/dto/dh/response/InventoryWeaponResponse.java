package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a weapon in a character's inventory.
 * <p>
 * Always includes the linking entity ID, weapon ID, equipped status, and slot.
 * The full weapon object is included only when {@code ?expand=inventoryWeapons} is specified.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryWeaponResponse {

    /**
     * Unique identifier for this inventory weapon linking entity
     */
    private Long id;

    /**
     * ID of the weapon
     */
    private Long weaponId;

    /**
     * Whether this weapon is currently equipped
     */
    private Boolean equipped;

    /**
     * The equipment slot: "PRIMARY" or "SECONDARY" (null if not equipped)
     */
    private String slot;

    /**
     * Full weapon details (included only when {@code ?expand=inventoryWeapons} is specified)
     */
    private WeaponResponse weapon;
}
