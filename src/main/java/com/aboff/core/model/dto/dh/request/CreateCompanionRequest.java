package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new companion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanionRequest {

    /**
     * ID of the character sheet this companion belongs to.
     */
    @NotNull(message = "Character sheet ID is required")
    private Long characterSheetId;

    /**
     * Name of the companion.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Description of the companion.
     */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * Evasion value (difficulty to hit the companion).
     * Defaults to 10 if not provided, per the printed rule that a companion's "Evasion...
     * starts at 10."
     */
    @Min(value = 0, message = "Evasion must be zero or positive")
    @Max(value = 50, message = "Evasion must not exceed 50")
    @Builder.Default
    private Integer evasion = 10;

    /**
     * Name of the companion's attack.
     */
    @NotBlank(message = "Attack name is required")
    @Size(max = 200, message = "Attack name must not exceed 200 characters")
    private String attackName;

    /**
     * Range of the companion's attack.
     */
    @NotNull(message = "Attack range is required")
    private Range attackRange;

    /**
     * Damage dice type for the companion's attack.
     */
    @NotNull(message = "Damage dice is required")
    private DiceType damageDice;

    /**
     * Maximum stress the companion can take.
     * Defaults to 3 if not provided.
     */
    @Min(value = 1, message = "Stress max must be at least 1")
    @Max(value = 20, message = "Stress max must not exceed 20")
    @Builder.Default
    private Integer stressMax = 3;

    /**
     * Current stress marked on the companion.
     * Defaults to 0 if not provided. Must not exceed {@link #stressMax}; that cross-field rule
     * is enforced in {@code CompanionService}, not here, since Bean Validation cannot compare
     * two fields on this annotation alone.
     */
    @Min(value = 0, message = "Stress marked must be zero or positive")
    @Max(value = 20, message = "Stress marked must not exceed 20")
    @Builder.Default
    private Integer stressMarked = 0;
}
