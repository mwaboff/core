package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.AdvancementType;
import com.aboff.core.model.enums.Trait;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a single advancement choice during level-up.
 * <p>
 * Each level-up requires exactly two advancement choices. The fields required
 * depend on the advancement type chosen.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancementChoice {

    /**
     * The type of advancement chosen.
     */
    @NotNull(message = "Advancement type is required")
    private AdvancementType type;

    /**
     * The traits to boost (required for BOOST_TRAITS, exactly 2).
     */
    private List<Trait> traits;

    /**
     * The experience IDs to boost (required for BOOST_EXPERIENCES, exactly 2).
     */
    private List<Long> experienceIds;

    /**
     * The domain card ID to gain (required for GAIN_DOMAIN_CARD).
     */
    private Long domainCardId;

    /**
     * Whether to equip the gained domain card (used with GAIN_DOMAIN_CARD).
     */
    @Builder.Default
    private Boolean equipDomainCard = false;

    /**
     * The subclass card ID (required for UPGRADE_SUBCLASS and MULTICLASS).
     */
    private Long subclassCardId;
}
