package com.aboff.core.model.dto.dh.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a character's Condition instance — namely, adjusting its magnitude
 * (e.g., a stacking condition gaining or losing stacks).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCharacterSheetConditionRequest {

    /**
     * Updated magnitude (stack count or intensity) of this condition instance.
     * If null, the magnitude will not be changed.
     */
    private Integer magnitude;
}
