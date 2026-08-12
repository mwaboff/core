package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionRequest {
    private String questionText;

    private QuestionType questionType;

    private Long expansionId;

    /**
     * Whether this question is SRD-licensed content. Only ADMIN/OWNER may set this to true;
     * see {@code ContentAccessService#resolveSrd}.
     */
    private Boolean srd;
}
