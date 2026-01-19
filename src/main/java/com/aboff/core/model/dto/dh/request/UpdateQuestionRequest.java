package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Expansion ID is required")
    private Long expansionId;
}
