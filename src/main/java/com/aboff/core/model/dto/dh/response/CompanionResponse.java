package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for companion data.
 * Supports expansion of related entities via ?expand query parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanionResponse {

    /**
     * Unique identifier for the companion.
     */
    private Long id;

    /**
     * ID of the character sheet this companion belongs to.
     * Always included in response.
     */
    private Long characterSheetId;

    /**
     * Full character sheet data.
     * Included only when ?expand=characterSheet is requested.
     */
    private CharacterSheetResponse characterSheet;

    /**
     * Name of the companion.
     */
    private String name;

    /**
     * Description of the companion.
     */
    private String description;

    /**
     * Evasion value (difficulty to hit the companion).
     */
    private Integer evasion;

    /**
     * Name of the companion's attack.
     */
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

    /**
     * List of experiences associated with this companion.
     * Included only when ?expand=experiences is requested.
     */
    private List<ExperienceResponse> experiences;

    /**
     * Timestamp when the companion was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the companion was last modified.
     */
    private LocalDateTime lastModifiedAt;
}
