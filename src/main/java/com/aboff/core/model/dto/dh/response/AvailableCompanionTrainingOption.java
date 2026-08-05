package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CompanionTrainingOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing one Training option's remaining selections for a specific companion during
 * level-up, mirroring {@link AvailableAdvancement}'s shape for the character's own advancements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCompanionTrainingOption {

    /**
     * The Training option.
     */
    private CompanionTrainingOption option;

    /**
     * How many more times this option can still be selected by this companion
     * (its per-companion-lifetime cap minus trainings already taken).
     */
    private int remaining;
}
