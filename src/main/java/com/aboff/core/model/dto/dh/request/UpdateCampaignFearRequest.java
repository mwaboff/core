package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a campaign's Fear counter.
 * <p>
 * Fear is a shared, table-visible resource. The value is absolute (not a delta)
 * and must fall within the Daggerheart range of 0 to 12 inclusive.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignFearRequest {

    /**
     * The campaign's new Fear value. Required, and must be between 0 and 12 inclusive.
     */
    @NotNull(message = "Fear is required")
    @Min(value = 0, message = "Fear must be at least 0")
    @Max(value = 12, message = "Fear must not exceed 12")
    private Integer fear;
}
