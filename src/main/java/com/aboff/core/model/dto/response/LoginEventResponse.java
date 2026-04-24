package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Admin-facing projection of a {@link com.aboff.core.model.entity.LoginEvent}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginEventResponse {

    private Long id;
    private String provider;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime createdAt;
}
