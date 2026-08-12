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
 * Request DTO for updating an existing Encounter.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEncounterRequest {

    /**
     * Name of the encounter.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * General description of the encounter.
     */
    private String description;

    /**
     * Power tier of the encounter (1-4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * Optional ID of the campaign this encounter belongs to.
     */
    private Long campaignId;

    /**
     * Optional ID of the environment (scene stat block) this encounter takes place in.
     */
    private Long environmentId;

    /**
     * Whether this encounter is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * Whether this encounter is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Optional; only ADMIN+ callers may actually set it to true --
     * see {@code ContentAccessService#resolveSrd}. See {@code CreateEncounterRequest#srd} for
     * why this is currently inert for API-created encounters.
     */
    private Boolean srd;

    /**
     * The number of PCs in combat, manually entered by the GM.
     */
    @Min(value = 1, message = "Party size must be at least 1")
    @Max(value = 12, message = "Party size must not exceed 12")
    private Integer partySize;

    /**
     * Battle Point adjustment: -1, the fight should be less difficult or shorter.
     */
    private Boolean adjustmentEasier;

    /**
     * Battle Point adjustment: -2, using 2 or more Solo adversaries.
     */
    private Boolean adjustmentTwoPlusSolos;

    /**
     * Battle Point adjustment: -2, adding +1d4 (or a static +2) to all adversaries' damage rolls.
     */
    private Boolean adjustmentBonusDamage;

    /**
     * Battle Point adjustment: +1, choosing an adversary from a lower tier.
     */
    private Boolean adjustmentLowerTier;

    /**
     * Battle Point adjustment: +1, including no Bruisers, Hordes, Leaders, or Solos.
     */
    private Boolean adjustmentNoElites;

    /**
     * Battle Point adjustment: +2, the fight should be more dangerous or last longer.
     */
    private Boolean adjustmentHarder;

    /**
     * List of adversary instances to replace the current adversaries in the encounter, each
     * optionally carrying a GM label and a retier target. If provided, completely replaces the
     * existing adversary list. Each entry represents a single adversary instance; to include
     * multiple instances of the same adversary, add multiple entries with the same
     * {@code adversaryId}.
     * <p>
     * Preferred over the deprecated {@link #adversaryIds}. If both are provided, this field
     * wins and {@link #adversaryIds} is ignored.
     * </p>
     */
    @Valid
    private List<CreateEncounterRequest.AdversaryEntry> adversaries;

    /**
     * Deprecated: list of bare adversary IDs to replace the current adversaries in the
     * encounter. Each entry represents a single adversary instance with no label or retier
     * target. Kept for backward compatibility with existing clients; prefer
     * {@link #adversaries}, which also supports a label and tier override per instance.
     */
    @Valid
    @Deprecated
    private List<Long> adversaryIds;
}
