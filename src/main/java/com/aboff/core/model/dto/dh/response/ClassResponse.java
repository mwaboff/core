package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Class entities.
 * Supports expansion of relationships via ?expand parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isOfficial;
    private Long expansionId;
    private ExpansionResponse expansion;
    private String startingClassItems;
    private Integer startingEvasion;
    private Integer startingHitPoints;

    // ID arrays (always included)
    private List<Long> associatedDomainIds;
    private List<Long> hopeFeatureIds;
    private List<Long> classFeatureIds;
    private List<Long> backgroundQuestionIds;
    private List<Long> connectionQuestionIds;

    // Full objects (included only with ?expand)
    private List<DomainResponse> associatedDomains;
    private List<FeatureResponse> hopeFeatures;
    private List<FeatureResponse> classFeatures;
    private List<QuestionResponse> backgroundQuestions;
    private List<QuestionResponse> connectionQuestions;

    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime deletedAt;
}
