package com.aboff.core.model.dto.dh.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing one eligible companion's Training options for a character's next level-up.
 * <p>
 * "Eligible" means the companion is active, has {@code advancesOnLevelUp} set, and already
 * existed before this level-up -- a companion this same level-up creates or restores gets no
 * entry (and no Training pick or Experience grant) until the level-up after.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionLevelUpOptionsResponse {

    /**
     * The companion's id.
     */
    private Long companionId;

    /**
     * The companion's name.
     */
    private String name;

    /**
     * The companion's current derived and base stats, reusing {@code CompanionService.toResponse}
     * so this never drifts from the single real mapping.
     */
    private CompanionResponse currentStats;

    /**
     * Every Training option and how many times it can still be selected.
     */
    private List<AvailableCompanionTrainingOption> availableOptions;

    /**
     * How many Training picks this companion can make in this level-up.
     * <p>
     * Always the baseline {@code 1} here -- this endpoint runs before the player has chosen
     * their two advancements for the level-up, so the {@code +1}/{@code +2} bonus from taking a
     * Beastbound Specialization/Mastery card this same level-up cannot be known yet. The
     * frontend recomputes this reactively once the Advancements step is filled in;
     * {@code LevelUpService.validateLevelUpRequest} is the authoritative check on submit.
     * </p>
     */
    private int picksAvailable;
}
