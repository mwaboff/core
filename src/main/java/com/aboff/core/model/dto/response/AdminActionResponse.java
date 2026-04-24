package com.aboff.core.model.dto.response;

import com.aboff.core.model.enums.AdminActionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Admin-facing projection of an
 * {@link com.aboff.core.model.entity.AdminActionLog} row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminActionResponse {

    private Long id;
    private Long actorUserId;
    /** Resolved username of {@code actorUserId}, if the user still exists. */
    private String actorUsername;
    private AdminActionType action;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
