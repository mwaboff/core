package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the automatic Experience a companion gains on a character's tier
 * transition.
 * <p>
 * Per the rules, "whenever you gain a new Experience, your companion also gains one" -- the
 * player names it, same as the character's own new tier Experience. Only consulted when the
 * level-up is a tier transition; silently ignored otherwise, matching
 * {@link LevelUpRequest#getNewExperienceDescription()}'s existing convention.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionExperienceGrant {

    /**
     * The companion gaining the new Experience.
     */
    @NotNull(message = "companionId is required")
    private Long companionId;

    /**
     * The player-supplied description of the new Experience.
     */
    @NotBlank(message = "description is required")
    private String description;
}
