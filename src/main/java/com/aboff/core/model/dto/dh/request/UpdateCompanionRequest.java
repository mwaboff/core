package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
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
     * Maximum stress the companion can take.
     */
    private Integer stressMax;

    /**
     * Current stress marked on the companion.
     */
    private Integer stressMarked;
}
