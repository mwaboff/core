package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for attaching a Condition instance to a character sheet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterSheetConditionRequest {

    /**
     * ID of the character sheet this condition instance applies to.
     */
    @NotNull(message = "Character sheet ID is required")
    private Long characterSheetId;

    /**
     * ID of the catalogue condition being applied.
     */
    @NotNull(message = "Condition ID is required")
    private Long conditionId;

    /**
     * The magnitude (stack count or intensity) of this condition instance.
     * Null for conditions that do not stack.
     */
    private Integer magnitude;
}
