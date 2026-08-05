package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing companion.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanionRequest {

    /**
     * Name of the companion.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Description of the companion.
     */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * Evasion value (difficulty to hit the companion).
     */
    @Min(value = 0, message = "Evasion must be zero or positive")
    @Max(value = 50, message = "Evasion must not exceed 50")
    private Integer evasion;

    /**
     * Name of the companion's attack.
     */
    @Size(max = 200, message = "Attack name must not exceed 200 characters")
    private String attackName;

    /**
     * Range of the companion's attack.
     */
    private Range attackRange;

    /**
     * Damage dice type for the companion's attack.
     */
    private DiceType damageDice;

    /**
     * Whether the companion's attack deals physical or magic damage.
     * Left {@code null} to leave the existing choice unchanged. Per the printed rule this is a
     * one-time either/or choice (core-01:1327) -- {@code CompanionService} rejects
     * {@link DamageType#PHYSICAL_AND_MAGIC}, which is a per-attack weapon mechanic, not a
     * companion concept.
     */
    private DamageType damageType;

    /**
     * Maximum stress the companion can take.
     */
    @Min(value = 1, message = "Stress max must be at least 1")
    @Max(value = 20, message = "Stress max must not exceed 20")
    private Integer stressMax;

    /**
     * Current stress marked on the companion. Must not exceed the companion's (possibly
     * concurrently updated) stress max; that cross-field rule is enforced in
     * {@code CompanionService}, not here.
     */
    @Min(value = 0, message = "Stress marked must be zero or positive")
    @Max(value = 20, message = "Stress marked must not exceed 20")
    private Integer stressMarked;
}
