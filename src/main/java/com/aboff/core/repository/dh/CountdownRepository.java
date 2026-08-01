package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Countdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Countdown} entities.
 * <p>
 * {@code Campaign} holds no inverse collection, so this repository is the only way to read a
 * campaign's countdowns.
 * </p>
 */
@Repository
public interface CountdownRepository extends JpaRepository<Countdown, Long> {

    /**
     * Retrieves all countdowns for a campaign in display order.
     *
     * @param campaignId The campaign ID to filter by
     * @return Countdowns ordered by display order, then by id for a stable tiebreak
     */
    @Query("SELECT c FROM Countdown c WHERE c.campaign.id = :campaignId "
            + "ORDER BY c.displayOrder ASC, c.id ASC")
    List<Countdown> findByCampaignId(@Param("campaignId") Long campaignId);

    /**
     * Finds the highest display order currently used within a campaign.
     *
     * @param campaignId The campaign ID to search within
     * @return The highest display order, or -1 if the campaign has no countdowns yet
     */
    @Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM Countdown c WHERE c.campaign.id = :campaignId")
    Integer findMaxDisplayOrderByCampaignId(@Param("campaignId") Long campaignId);
}
