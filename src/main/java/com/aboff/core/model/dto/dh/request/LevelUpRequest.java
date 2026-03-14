package com.aboff.core.model.dto.dh.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for leveling up a character sheet.
 * <p>
 * A level-up consists of:
 * </p>
 * <ol>
 *   <li>Exactly 2 advancement choices (e.g., boost traits, gain HP, etc.)</li>
 *   <li>A new domain card selection (Step 4)</li>
 *   <li>Optionally, a new experience description (required at tier transitions: levels 2, 5, 8)</li>
 *   <li>Optionally, domain card trades (equal-count swaps)</li>
 *   <li>Optionally, unequipping a domain card to make room when at 5 equipped</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelUpRequest {

    /**
     * Exactly 2 advancement choices for this level-up.
     */
    @NotNull(message = "Advancements are required")
    @Size(min = 2, max = 2, message = "Exactly 2 advancements are required")
    @Valid
    private List<AdvancementChoice> advancements;

    /**
     * Description for a new experience. Required at tier transitions (levels 2, 5, 8).
     */
    private String newExperienceDescription;

    /**
     * ID of the new domain card to gain in Step 4.
     */
    @NotNull(message = "New domain card ID is required")
    private Long newDomainCardId;

    /**
     * Whether to equip the new domain card. Defaults to {@code false}.
     */
    @Builder.Default
    private Boolean equipNewDomainCard = false;

    /**
     * Optional ID of an equipped domain card to unequip, making room when at 5 equipped cards.
     */
    private Long unequipDomainCardId;

    /**
     * Optional list of equal-swap domain card trades.
     */
    private List<DomainCardTradeRequest> trades;
}
