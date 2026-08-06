package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.BaseItem;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared access-control rules for user-authored equipment (weapons, armor, and loot).
 * <p>
 * Weapons, armor, and loot need byte-identical answers to the same four questions: who may
 * publish content, which sourcebook a row may claim, which campaigns it may be shared with,
 * and who may edit it. Those rules live here rather than being copied into
 * {@code WeaponService}, {@code ArmorService}, and {@code LootService} — the equivalent logic
 * for adversaries and encounters was duplicated per entity and has already drifted apart.
 * </p>
 * <p>
 * Item visibility is: official, or public, or authored by the caller, or explicitly tagged to
 * a campaign the caller is involved in. Sharing is deliberate — an untagged custom item stays
 * private to its author. There is no derived "we share a campaign so you see my things" rule,
 * so nothing a user makes becomes visible without them choosing it.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemAccessService {

    /**
     * Stand-in for an empty ID collection.
     * <p>
     * PostgreSQL and Hibernate both reject an empty {@code IN ()} list, so a user who belongs
     * to no campaigns would fail every browse request. No row can have id -1, so binding this
     * makes the campaign clause match nothing while staying syntactically valid.
     * </p>
     */
    private static final List<Long> NO_MATCH = List.of(-1L);

    private final CampaignRepository campaignRepository;
    private final ExpansionRepository expansionRepository;
    private final RoleHierarchyService roleHierarchyService;

    /**
     * The precomputed inputs to an item visibility query, resolved once per request.
     *
     * @param userId the caller's user ID, matched against a row's creator
     * @param memberCampaignIds campaigns the caller is involved in; never empty, see
     *                          {@link #NO_MATCH}
     * @param privileged true for MODERATOR and above, who bypass visibility filtering entirely
     */
    public record VisibilityScope(Long userId, Collection<Long> memberCampaignIds, boolean privileged) {
    }

    /**
     * Resolves what the given caller is allowed to see.
     *
     * @param authentication the current authentication
     * @return the caller's visibility scope
     */
    @Transactional(readOnly = true)
    public VisibilityScope visibilityScope(Authentication authentication) {
        User user = currentUser(authentication);
        boolean privileged = roleHierarchyService.hasModeratorOrHigher(user);

        List<Long> campaignIds = campaignRepository.findActiveCampaignIdsByUserInvolvement(user.getId());
        return new VisibilityScope(
                user.getId(),
                campaignIds.isEmpty() ? NO_MATCH : campaignIds,
                privileged);
    }

    /**
     * Resolves the official flag a request may actually apply.
     * <p>
     * A requested flag from a user below MODERATOR is coerced to false and logged rather than
     * rejected. Rejecting would fail an otherwise valid creation over a field the custom-item
     * form never shows, which reads as an unexplained error to the person who hit it.
     * </p>
     *
     * @param user the user performing the create or update
     * @param requestedIsOfficial the requested official flag; may be null
     * @return true only when the user is MODERATOR+ and explicitly requested official status
     */
    public boolean resolveIsOfficial(User user, Boolean requestedIsOfficial) {
        return resolveModeratorFlag(user, requestedIsOfficial, "official");
    }

    /**
     * Resolves the public flag a request may actually apply, with the same coercion rules as
     * {@link #resolveIsOfficial}.
     *
     * @param user the user performing the create or update
     * @param requestedIsPublic the requested public flag; may be null
     * @return true only when the user is MODERATOR+ and explicitly requested public status
     */
    public boolean resolveIsPublic(User user, Boolean requestedIsPublic) {
        return resolveModeratorFlag(user, requestedIsPublic, "public");
    }

    private boolean resolveModeratorFlag(User user, Boolean requested, String flagName) {
        boolean canMark = roleHierarchyService.hasModeratorOrHigher(user);

        if (!canMark && Boolean.TRUE.equals(requested)) {
            log.warn("User id={} with role={} requested {} content; coercing {} to false",
                    user.getId(), user.getRole(), flagName, flagName);
        }

        return canMark && Boolean.TRUE.equals(requested);
    }

    /**
     * Resolves the sourcebook an item may claim.
     * <p>
     * An expansion identifies the book a piece of content was printed in, so only official
     * content can hold one — a weapon invented at someone's table came from no book. This is
     * enforced here rather than left to the database constraint so the rule is applied where
     * the request context exists, and a stray {@code expansionId} is quietly dropped instead
     * of surfacing as a constraint violation.
     * </p>
     *
     * @param user the user performing the create or update
     * @param requestedExpansionId the requested expansion ID; may be null
     * @param resolvedIsOfficial the already-resolved official flag for this item
     * @return the expansion when the item is official and one was named, otherwise null
     * @throws EntityNotFoundException if an official item names an expansion that does not exist
     */
    public Expansion resolveExpansion(User user, Long requestedExpansionId, boolean resolvedIsOfficial) {
        if (!resolvedIsOfficial) {
            if (requestedExpansionId != null) {
                log.warn("User id={} supplied expansionId={} for custom content; dropping it",
                        user.getId(), requestedExpansionId);
            }
            return null;
        }

        if (requestedExpansionId == null) {
            return null;
        }

        return expansionRepository.findByIdAndDeletedAtIsNull(requestedExpansionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + requestedExpansionId));
    }

    /**
     * Rejects an item that claims official status without naming the sourcebook it was printed in.
     * <p>
     * The database enforces the same rule as a check constraint, but reaching it produces an
     * opaque 500: promoting a custom item to official is a moderator's deliberate act, and the
     * only useful response is to tell them which field is missing. The migration that added
     * {@code chk_*_official_has_expansion} says the service is where this rule lives.
     * </p>
     * <p>
     * Call this after both the official flag and the expansion have been resolved, since either
     * one can be the field that changed.
     * </p>
     *
     * @param item the item about to be saved
     * @param label the item type name, used in the error message (e.g. "weapon")
     * @throws IllegalStateException if the item is official but has no expansion; surfaces as a
     *         400 through {@code GlobalExceptionHandler}
     */
    public void validateOfficialHasExpansion(BaseItem item, String label) {
        if (Boolean.TRUE.equals(item.getIsOfficial()) && item.getExpansion() == null) {
            throw new IllegalStateException("An official " + label
                    + " must name the sourcebook it was printed in; set expansionId to a sourcebook");
        }
    }

    /**
     * Resolves the campaigns an item may be shared with, rejecting any the user is not part of.
     * <p>
     * Distinguishes "not mentioned" from "cleared": a null list means leave existing tags
     * untouched, while an empty list means remove them all. Those are different intents on an
     * update and collapsing them would silently unshare items.
     * </p>
     *
     * @param user the user performing the create or update
     * @param campaignIds the requested campaign IDs; null to leave tags unchanged
     * @return the resolved campaigns, an empty set to clear all tags, or null to leave unchanged
     * @throws EntityNotFoundException if a campaign does not exist or has been deleted
     * @throws InsufficientPermissionsException if the user is not involved in a requested campaign
     */
    @Transactional(readOnly = true)
    public Set<Campaign> resolveCampaigns(User user, List<Long> campaignIds) {
        if (campaignIds == null) {
            return null;
        }
        if (campaignIds.isEmpty()) {
            // Mutable on purpose. Hibernate calls clear() on this collection while merging an
            // item that currently has tags, so an immutable empty set throws
            // UnsupportedOperationException and the update fails with a 500.
            return new LinkedHashSet<>();
        }

        boolean privileged = roleHierarchyService.hasModeratorOrHigher(user);
        Set<Campaign> resolved = new LinkedHashSet<>();

        for (Long campaignId : campaignIds) {
            Campaign campaign = campaignRepository.findActiveById(campaignId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Campaign not found with id: " + campaignId));

            if (!privileged && !campaign.isInvolved(user.getId())) {
                log.warn("User id={} attempted to share an item with campaign id={} they are not part of",
                        user.getId(), campaignId);
                throw new InsufficientPermissionsException(
                        "You are not a member of campaign " + campaignId);
            }

            resolved.add(campaign);
        }

        return resolved;
    }

    /**
     * Validates that the caller may modify the given item.
     * <p>
     * Official content stays restricted to ADMIN and above — the same audience that could edit
     * it before user authoring existed, which keeps the content-import pipeline working.
     * Custom content is editable by its author or by a moderator. An item with no author (an
     * official row later demoted to custom) falls to moderators only, since there is nobody
     * else it could belong to.
     * </p>
     *
     * @param item the item being modified
     * @param label the item type name, used in the error message (e.g. "weapon")
     * @param authentication the current authentication
     * @throws InsufficientPermissionsException if the caller may not modify the item
     */
    public void validateModifyPermission(BaseItem item, String label, Authentication authentication) {
        User user = currentUser(authentication);

        if (Boolean.TRUE.equals(item.getIsOfficial())) {
            roleHierarchyService.requireRoleOrHigher(user, Role.ADMIN);
            return;
        }

        boolean isCreator = item.getCreatedBy() != null
                && item.getCreatedBy().getId().equals(user.getId());

        if (!isCreator && !roleHierarchyService.hasModeratorOrHigher(user)) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to modify this " + label);
        }
    }

    /**
     * Requires that the caller is a moderator or above.
     *
     * @param authentication the current authentication
     * @throws InsufficientPermissionsException if the caller is below MODERATOR
     */
    public void requireModerator(Authentication authentication) {
        roleHierarchyService.requireRoleOrHigher(currentUser(authentication), Role.MODERATOR);
    }

    /**
     * Extracts the authenticated user from the security context principal.
     *
     * @param authentication the current authentication
     * @return the authenticated user
     */
    public User currentUser(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }
}
