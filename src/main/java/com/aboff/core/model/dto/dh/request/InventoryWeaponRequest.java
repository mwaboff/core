package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a weapon in a character's inventory.
 * <p>
 * Represents a weapon entry with equipped status and optional slot assignment.
 * Equipped weapons must specify a slot (PRIMARY or SECONDARY), and unequipped
 * weapons must not have a slot.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryWeaponRequest {

    /**
     * ID of the weapon to add to inventory
     */
    @NotNull(message = "Weapon ID is required")
    private Long weaponId;

    /**
     * Whether this weapon is currently equipped (defaults to false)
     */
    @Builder.Default
    private Boolean equipped = false;

    /**
     * The equipment slot for this weapon: "PRIMARY" or "SECONDARY".
     * Required when equipped is true, must be null when equipped is false.
     */
    @Size(max = 20)
    private String slot;
}
