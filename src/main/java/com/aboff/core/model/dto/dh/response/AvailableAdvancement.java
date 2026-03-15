package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.AdvancementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing an available advancement option during level-up.
 * <p>
 * Includes the advancement type, how many times it can still be used in the
 * current tier, and any mutual exclusion relationships.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableAdvancement {

    /**
     * The advancement type.
     */
    private AdvancementType type;

    /**
     * How many times this advancement can still be used in the current tier.
     */
    private int remaining;

    /**
     * Advancement types that are mutually exclusive with this one in the current tier.
     */
    private List<AdvancementType> mutuallyExclusiveWith;
}
