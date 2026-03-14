package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.AdvancementType;
import com.aboff.core.model.enums.Trait;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single advancement choice made during a level-up.
 * <p>
 * Each level-up requires exactly two advancement choices. The fields that must be populated
 * depend on the {@link AdvancementType} selected:
 * </p>
 * <ul>
 *   <li>{@code BOOST_TRAITS} — requires {@code boostTraits} (exactly 2 unmarked traits)</li>
 *   <li>{@code BOOST_EXPERIENCES} — requires {@code boostExperienceIds} (exactly 2 experience IDs)</li>
 *   <li>{@code GAIN_DOMAIN_CARD} — requires {@code domainCardId}, optionally {@code equipDomainCard}</li>
 *   <li>{@code UPGRADE_SUBCLASS} — requires {@code subclassCardId}</li>
 *   <li>{@code MULTICLASS} — requires {@code multiclassSubclassPathId} and {@code multiclassFoundationCardId}</li>
 *   <li>{@code GAIN_HP}, {@code GAIN_STRESS}, {@code BOOST_EVASION}, {@code BOOST_PROFICIENCY} — no additional fields</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvancementChoice {

    /**
     * The type of advancement to apply.
     */
    @NotNull(message = "Advancement type is required")
    private AdvancementType type;

    /**
     * For {@code BOOST_TRAITS}: exactly 2 unmarked traits to boost by +1 and mark.
     */
    private List<Trait> boostTraits;

    /**
     * For {@code BOOST_EXPERIENCES}: exactly 2 experience IDs to boost by +1.
     */
    private List<Long> boostExperienceIds;

    /**
     * For {@code GAIN_DOMAIN_CARD}: the ID of the domain card to gain.
     */
    private Long domainCardId;

    /**
     * For {@code GAIN_DOMAIN_CARD}: whether to equip the gained domain card.
     * Defaults to {@code false}.
     */
    @Builder.Default
    private Boolean equipDomainCard = false;

    /**
     * For {@code UPGRADE_SUBCLASS}: the ID of the subclass card to take.
     */
    private Long subclassCardId;

    /**
     * For {@code MULTICLASS}: the ID of the new subclass path to multiclass into.
     */
    private Long multiclassSubclassPathId;

    /**
     * For {@code MULTICLASS}: the ID of the foundation card from the new subclass path.
     */
    private Long multiclassFoundationCardId;
}
