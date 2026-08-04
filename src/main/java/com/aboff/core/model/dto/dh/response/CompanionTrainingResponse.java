package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.ViciousAxis;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single Training selection on a companion.
 * Always included in full on {@link CompanionResponse#getTrainings()} -- not gated by
 * {@code ?expand=}, since a companion's Training list is small and core to its state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanionTrainingResponse {

    /**
     * Unique identifier for this Training selection.
     */
    private Long id;

    /**
     * Which Training option this selection is for.
     */
    private CompanionTrainingOption option;

    /**
     * Which ladder this selection advances. Only set when {@code option} is {@code VICIOUS}.
     */
    private ViciousAxis viciousAxis;

    /**
     * The id of the Experience this selection grants a permanent +1 to. Only set when
     * {@code option} is {@code INTELLIGENT}.
     */
    private Long targetExperienceId;

    /**
     * The character level at which this Training was acquired.
     */
    private Integer acquiredAtLevel;
}
