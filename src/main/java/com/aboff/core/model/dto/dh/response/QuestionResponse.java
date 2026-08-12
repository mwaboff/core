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
public class QuestionResponse implements Restrictable {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private Long expansionId;
    private ExpansionResponse expansion;

    /**
     * Whether this question is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in.
     */
    private Boolean srd;

    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime deletedAt;

    /**
     * The display name of the expansion this question belongs to. Set on a redacted stub so
     * the caller can tell which book to buy, even though {@link #expansion} itself is unset.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;

    /**
     * Restrictable's setter for the question's display name — a QuestionResponse has no
     * {@code name} field (it uses {@link #questionText}), so this is a no-op. Never called by
     * {@link com.aboff.core.util.ContentRedaction#stub}, which never sets a name on a stub.
     *
     * @param name unused
     */
    @Override
    public void setName(String name) {
        // No-op: QuestionResponse has no "name" field.
    }
}
