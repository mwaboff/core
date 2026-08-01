package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for ticking a countdown.
 * <p>
 * Carries an absolute value rather than a delta, matching {@code UpdateCampaignFearRequest}:
 * a GM tapping quickly can have several requests in flight at once, and absolute values make
 * the last one win instead of compounding into a lost update.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCountdownValueRequest {

    @NotNull(message = "Current value is required")
    @Min(value = 0, message = "Current value must be at least 0")
    @Max(value = 99, message = "Current value must not exceed 99")
    private Integer currentValue;
}
