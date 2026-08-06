package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
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
 * Request DTO for a user creating their own weapon.
 * <p>
 * Deliberately separate from {@link CreateWeaponRequest} rather than a relaxation of it.
 * {@code CreateWeaponRequest} serves the admin bulk-import pipeline, where {@code isOfficial}
 * and {@code expansionId} are required and a missing one should fail loudly — relaxing them so
 * regular users could post would mean an import payload that omits either silently lands as
 * un-attributed homebrew. Two types keep the import contract strict and make the two flows
 * impossible to confuse.
 * </p>
 * <p>
 * Fields absent by design: {@code isOfficial} and {@code expansionId} (custom content is never
 * canon and belongs to no sourcebook) and {@code originalWeaponId} (set only by the copy
 * endpoint, so a caller cannot claim their weapon derives from something it does not).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomWeaponRequest {

    /**
     * Name of the weapon.
     */
    @NotBlank(message = "Weapon name is required")
    @Size(max = 200, message = "Weapon name must not exceed 200 characters")
    private String name;

    /**
     * The tier level of the weapon (1–4).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this weapon should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Campaigns to share this weapon with. Everyone involved in a listed campaign can see and
     * equip it. The creator must be involved in each campaign they name.
     */
    private List<Long> campaignIds;

    /**
     * Whether this is a primary weapon (true) or secondary weapon (false).
     */
    @NotNull(message = "isPrimary is required")
    private Boolean isPrimary;

    /**
     * The trait used to attack with this weapon.
     */
    @NotNull(message = "Trait is required")
    private Trait trait;

    /**
     * The effective range of the weapon.
     */
    @NotNull(message = "Range is required")
    private Range range;

    /**
     * The burden type (one-handed or two-handed).
     */
    @NotNull(message = "Burden is required")
    private Burden burden;

    /**
     * The damage roll for this weapon.
     */
    @Valid
    @NotNull(message = "Damage is required")
    private CreateWeaponRequest.DamageRollRequest damage;

    /**
     * Features granted by this weapon, created inline.
     * <p>
     * Capped as a runaway guard: item creation is open to every authenticated user and the
     * backend has no rate limiting, so an unbounded list is a cheap way to grow the shared
     * features table.
     * </p>
     */
    @Valid
    @Size(max = 20, message = "A weapon may not have more than 20 features")
    private List<FeatureInput> features;
}
