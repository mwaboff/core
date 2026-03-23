package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClassRequest {
    @NotBlank(message = "Class name is required")
    @Size(max = 100, message = "Class name must not exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    private String startingClassItems;

    @NotNull(message = "Starting evasion is required")
    @Positive(message = "Starting evasion must be positive")
    private Integer startingEvasion;

    @NotNull(message = "Starting hit points is required")
    @Positive(message = "Starting hit points must be positive")
    private Integer startingHitPoints;

    private List<Long> associatedDomainIds;
    private List<Long> hopeFeatureIds;
    private List<Long> classFeatureIds;
    private List<Long> backgroundQuestionIds;
    private List<Long> connectionQuestionIds;

    /** Hope features to find or create inline. Merged with hopeFeatureIds if both provided. */
    @Valid
    private List<FeatureInput> hopeFeatures;

    /** Class features to find or create inline. Merged with classFeatureIds if both provided. */
    @Valid
    private List<FeatureInput> classFeatures;

    /** Background questions to find or create inline. Merged with backgroundQuestionIds if both provided. */
    @Valid
    private List<QuestionInput> backgroundQuestions;

    /** Connection questions to find or create inline. Merged with connectionQuestionIds if both provided. */
    @Valid
    private List<QuestionInput> connectionQuestions;
}
