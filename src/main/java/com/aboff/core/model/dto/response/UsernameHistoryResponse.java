package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Admin-facing projection of a
 * {@link com.aboff.core.model.entity.UsernameHistory} row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsernameHistoryResponse {

    private String previousUsername;
    private String newUsername;
    private Long changedByUserId;
    /** Resolved username of {@code changedByUserId}, if the user still exists. */
    private String changedByUsername;
    private LocalDateTime changedAt;
}
