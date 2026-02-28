package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for finding or creating a feature modifier by its composite key.
 * Used in feature create/update requests to allow clients to specify modifiers inline
 * instead of (or in addition to) existing modifier IDs.
 * <p>
 * The service looks up an existing modifier by {@code (target, operation, value)}
 * and creates a new one if no match is found.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureModifierInput {

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
