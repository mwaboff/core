package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for FeatureModifier entities.
 * Represents a structured, machine-readable stat modification that can be
 * associated with Features in the Daggerheart TTRPG system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureModifierResponse {

    /**
     * Unique identifier for the feature modifier
     */
    private Long id;

    /**
     * The stat or attribute this modifier targets (e.g., EVASION, STRENGTH)
     */
    private ModifierTarget target;

    /**
     * The operation to apply (ADD, SET, or MULTIPLY)
     */
    private ModifierOperation operation;

    /**
     * The numeric value for the modifier (e.g., -1, 2, 15)
     */
    private Integer value;

    /**
     * Timestamp when the feature modifier was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the feature modifier was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the feature modifier was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}