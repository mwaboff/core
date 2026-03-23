package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for finding or creating a question by text.
 * Used in class create/update requests to allow clients to specify
 * questions inline instead of (or in addition to) existing question IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInput {

    /** Text of the question. When provided, matched case-insensitively against existing questions within the same expansion and type. */
    @NotBlank(message = "Question text is required")
    private String questionText;

    /** Type/category of this question (BACKGROUND or CONNECTION). */
    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    /** ID of the expansion this question belongs to. */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;
}
