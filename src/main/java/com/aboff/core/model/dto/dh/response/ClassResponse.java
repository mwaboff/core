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
public class ClassResponse implements Restrictable {
    private Long id;
    private String name;
    private String description;
    private Boolean isOfficial;

    /**
     * Whether this class is SRD-licensed content, freely usable without owning the sourcebook
     * it was printed in.
     */
    private Boolean srd;

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

    /**
     * The display name of the expansion this class belongs to. Set on a redacted stub so the
     * caller can tell which book to buy, even though {@link #expansion} itself is unset.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;
}
