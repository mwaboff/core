package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for campaign invite details.
 * <p>
 * Returned when a GM generates an invite link for their campaign.
 * Contains the token and a pre-built invite URL for sharing.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignInviteResponse {

    /**
     * Invite ID
     */
    private Long id;

    /**
     * Campaign ID this invite is for
     */
    private Long campaignId;

    /**
     * Invite token (UUID)
     */
    private String token;

    /**
     * When this invite expires
     */
    private LocalDateTime expiresAt;

    /**
     * When this invite was created
     */
    private LocalDateTime createdAt;
}
