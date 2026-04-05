package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CampaignInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CampaignInvite entity operations.
 * <p>
 * Provides data access methods for campaign invites including token lookup,
 * listing by campaign, and cleanup of expired/used invites.
 * </p>
 */
@Repository
public interface CampaignInviteRepository extends JpaRepository<CampaignInvite, Long> {

    /**
     * Finds an invite by its token.
     *
     * @param token the invite token
     * @return the invite if found
     */
    Optional<CampaignInvite> findByToken(String token);

    /**
     * Finds all invites for a given campaign.
     *
     * @param campaignId the campaign ID
     * @return list of invites for the campaign
     */
    List<CampaignInvite> findByCampaignId(Long campaignId);

    /**
     * Deletes expired unused invites older than the cutoff.
     *
     * @param cutoff the cutoff date
     * @return number of deleted invites
     */
    @Modifying
    @Query("DELETE FROM CampaignInvite ci WHERE ci.usedAt IS NULL AND ci.expiresAt < :cutoff")
    int deleteExpiredInvites(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Deletes used invites older than the cutoff.
     *
     * @param cutoff the cutoff date
     * @return number of deleted invites
     */
    @Modifying
    @Query("DELETE FROM CampaignInvite ci WHERE ci.usedAt IS NOT NULL AND ci.usedAt < :cutoff")
    int deleteOldUsedInvites(@Param("cutoff") LocalDateTime cutoff);
}
