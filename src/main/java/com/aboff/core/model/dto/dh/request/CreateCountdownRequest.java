package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.model.enums.CountdownType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a countdown within a campaign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCountdownRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Countdown type is required")
    private CountdownType type;

    /** Optional; defaults to NONE when omitted. */
    private CountdownLoop loopBehavior;

    @NotNull(message = "Starting value is required")
    @Min(value = 1, message = "Starting value must be at least 1")
    @Max(value = 99, message = "Starting value must not exceed 99")
    private Integer startingValue;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;
}
