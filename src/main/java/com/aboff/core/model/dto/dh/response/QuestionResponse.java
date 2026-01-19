package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Question entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private Long expansionId;
    private ExpansionResponse expansion;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime deletedAt;
}
