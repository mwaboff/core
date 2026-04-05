package com.aboff.core.model.dto.dh.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned when a user successfully joins a campaign via invite.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinCampaignResponse {

    /**
     * ID of the campaign joined
     */
    private Long campaignId;

    /**
     * Name of the campaign joined
     */
    private String campaignName;

    /**
     * Human-readable success message
     */
    private String message;
}
