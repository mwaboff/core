package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing an invite link for a campaign.
 * <p>
 * Campaign invites are short-lived (24 hours), single-use tokens that allow
 * a user to join a campaign as a player. Once used or expired, the invite
 * is no longer valid.
 * </p>
 */
@Entity
@Table(name = "campaign_invites")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignInvite extends BaseEntity {

    /**
     * The campaign this invite is for.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * UUID token used in the invite URL.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /**
     * ID of the user who created this invite.
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * ID of the user who used this invite, null if unused.
     */
    @Column(name = "used_by")
    private Long usedBy;

    /**
     * When this invite expires.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When this invite was used, null if unused.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Returns whether this invite is still valid (not expired and not used).
     *
     * @return true if the invite can still be used
     */
    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Marks this invite as used by the specified user.
     *
     * @param userId the ID of the user who is using the invite
     */
    public void markUsed(Long userId) {
        this.usedBy = userId;
        this.usedAt = LocalDateTime.now();
    }
}
