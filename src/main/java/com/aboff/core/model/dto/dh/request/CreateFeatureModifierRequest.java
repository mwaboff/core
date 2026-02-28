package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new FeatureModifier directly via the API.
 * <p>
 * A feature modifier represents a structured, machine-readable stat modification
 * (e.g., -1 Evasion, +2 Strength) that can be associated with Features.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeatureModifierRequest {

    /**
     * The stat or attribute this modifier targets (e.g., EVASION, STRENGTH).
     */
    @NotNull(message = "Modifier target is required")
    private ModifierTarget target;

    /**
     * The operation to apply (ADD, SET, or MULTIPLY).
     */
    @NotNull(message = "Modifier operation is required")
    private ModifierOperation operation;

    /**
     * The numeric value for the modifier (e.g., -1, 2, 15).
     */
    @NotNull(message = "Modifier value is required")
    private Integer value;
}