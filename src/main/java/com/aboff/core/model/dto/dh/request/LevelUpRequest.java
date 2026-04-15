package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a level-up request for a character sheet.
 * <p>
 * Contains the two advancement choices, optional domain card operations,
 * and tier transition data required to level up a character.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpRequest {

    /**
     * The advancements applied during this level-up.
     * <p>
     * The list must contain the two player-chosen advancements and may additionally include
     * any number of {@link com.aboff.core.model.enums.AdvancementType#FEATURE_DOMAIN_CARD}
     * entries granted by subclass features (e.g. foundation features with a
     * {@code BONUS_DOMAIN_CARD_SELECTIONS} modifier). The "exactly 2 player advancements"
     * invariant is enforced in {@code LevelUpService.validateLevelUpRequest}, counting only
     * non-{@code FEATURE_DOMAIN_CARD} entries.
     * </p>
     */
    @NotNull(message = "Advancements are required")
    @Size(min = 2, message = "At least 2 advancements are required")
    @Valid
    private List<AdvancementChoice> advancements;

    /**
     * Description for the new experience gained on tier transition.
     * Required when leveling up crosses a tier boundary.
     */
    private String newExperienceDescription;

    /**
     * ID of a new domain card to add during Step 4.
     */
    private Long newDomainCardId;

    /**
     * Whether to equip the new domain card from Step 4.
     */
    @Builder.Default
    private Boolean equipNewDomainCard = false;

    /**
     * ID of a domain card to unequip to make room for a new equipped card.
     */
    private Long unequipDomainCardId;

    /**
     * Optional list of domain card trades.
     */
    @Valid
    private List<DomainCardTradeRequest> trades;
}
