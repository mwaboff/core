package com.aboff.core.service.dh;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.BaseItem;
import com.aboff.core.model.entity.dh.Card;
import com.aboff.core.model.enums.Role;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.RoleHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Central access-control rules for SRD vs. paid-expansion ("non-SRD") game content.
 * <p>
 * Content freely licensed under the Daggerheart SRD is visible to every authenticated user.
 * Content that only exists in a paid book (Hope &amp; Fear, future expansions) is gated behind
 * ADMIN/OWNER role or a per-user {@link User#getAccessAllExpansions() Access All Expansions}
 * grant. This service is the single place that decision is made, so ~20 catalogue and
 * character-sheet services across the codebase ask it the same question rather than
 * re-deriving the rule.
 * </p>
 * <p>
 * <strong>Why {@link SecurityContextHolder} and not an {@code Authentication} parameter:</strong>
 * the content list controllers (e.g. {@code DomainCardController}) take no {@code Authentication}
 * at all today. Threading one through every controller and every service on the catalogue browse
 * path is a many-site diff where a single omission is a silent content leak. Reading the security
 * context here instead makes gating a one-line-per-query change at each call site and fails closed
 * on every unexpected principal shape (no auth, an anonymous token, a non-{@link CustomUserDetails}
 * principal) without depending on every caller remembering to pass one in.
 * </p>
 * <p>
 * This composes alongside {@link ItemAccessService}, it does not replace it.
 * {@link ItemAccessService.VisibilityScope} is resolved from an explicit {@code Authentication}
 * and threaded as bind params into the equipment visibility queries (official/public/mine/campaign);
 * SRD gating is an orthogonal, catalogue-wide concern layered on top, and is deliberately not added
 * as a field there.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentAccessService {

    private final RoleHierarchyService roleHierarchyService;

    /**
     * Kill switch for the whole feature. Defaults false: until the SRD subset of the catalogue
     * has been flagged by the bulk SRD-flagging tool, every row is {@code srd = false}, so
     * enabling gating early would empty the catalogue for every non-privileged user.
     */
    @Value("${application.content.srd-gating-enabled:false}")
    private boolean srdGatingEnabled;

    /**
     * Resolves whether the current caller may see paid-expansion (non-SRD) content at all.
     * <p>
     * Returns {@code true} unconditionally while the kill switch is off, since the feature is
     * inert until then. Once enabled, this is default-deny: a {@code null} authentication, an
     * unauthenticated {@link Authentication}, an {@link AnonymousAuthenticationToken}, or any
     * principal that is not a {@link CustomUserDetails} all resolve to {@code false}. Otherwise
     * the caller may view non-SRD content if their role is ADMIN or OWNER (deliberately not
     * MODERATOR) or they carry an explicit {@link User#getAccessAllExpansions()} grant.
     * </p>
     *
     * @return true if the current caller may view non-SRD content
     */
    public boolean mayViewNonSrd() {
        if (!srdGatingEnabled) {
            return true;
        }

        User user = currentUserOrNull();
        if (user == null) {
            return false;
        }

        return roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)
                || Boolean.TRUE.equals(user.getAccessAllExpansions());
    }

    /**
     * The repository bind-parameter accessor for {@code :includeNonSrd}.
     * <p>
     * Delegates entirely to {@link #mayViewNonSrd()}; kept as a separate name so call sites in
     * repository-invoking code read as query plumbing ("bind includeNonSrd") rather than a
     * permission check, even though it is the same decision.
     * </p>
     *
     * @return true if the current caller's queries should include non-SRD rows
     */
    public boolean includeNonSrd() {
        return mayViewNonSrd();
    }

    /**
     * Core per-entity visibility check, for the redaction layer.
     * <p>
     * An entity is visible if it is not official (custom content is never gated), or it is
     * flagged {@code srd}, or the caller may view non-SRD content per {@link #mayViewNonSrd()}.
     * Null {@code isOfficial} or {@code srd} are treated as false.
     * </p>
     * <p>
     * This is the one true overload; {@link #mayView(Card)} and {@link #mayView(BaseItem)} are
     * thin adapters over it. The twelve standalone gated entities ({@code Domain}, {@code Class},
     * {@code Adversary}, etc.) share no common supertype carrying these two fields, so their call
     * sites invoke this overload directly, e.g. {@code mayView(domain.getIsOfficial(), domain.getSrd())}.
     * </p>
     *
     * @param isOfficial whether the entity is official game content; null treated as false
     * @param srd whether the entity is SRD-licensed; null treated as false
     * @return true if the entity should be visible to the current caller
     */
    public boolean mayView(Boolean isOfficial, Boolean srd) {
        if (!Boolean.TRUE.equals(isOfficial)) {
            return true;
        }
        if (Boolean.TRUE.equals(srd)) {
            return true;
        }
        return mayViewNonSrd();
    }

    /**
     * {@link #mayView(Boolean, Boolean)} for a {@link Card} (ancestry, community, subclass, or
     * domain card).
     *
     * @param card the card to check
     * @return true if the card should be visible to the current caller
     */
    public boolean mayView(Card card) {
        return mayView(card.getIsOfficial(), card.getSrd());
    }

    /**
     * {@link #mayView(Boolean, Boolean)} for a {@link BaseItem} (weapon, armor, loot, or
     * martial stance).
     *
     * @param item the item to check
     * @return true if the item should be visible to the current caller
     */
    public boolean mayView(BaseItem item) {
        return mayView(item.getIsOfficial(), item.getSrd());
    }

    /**
     * Resolves the SRD flag a create/update request may actually apply.
     * <p>
     * Mirrors {@link ItemAccessService#resolveIsOfficial(User, Boolean)} in shape: a requested
     * flag from a caller below the required role is coerced to false and logged rather than
     * rejected, so an otherwise valid request does not fail over a field its form never shows.
     * The threshold here is ADMIN or higher, not MODERATOR — SRD licensing is a legal
     * classification of the content, not a moderation judgment call.
     * </p>
     *
     * @param user the user performing the create or update
     * @param requested the requested srd flag; may be null
     * @return true only when the user is ADMIN+ and explicitly requested srd status
     */
    public boolean resolveSrd(User user, Boolean requested) {
        boolean canMark = roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN);

        if (!canMark && Boolean.TRUE.equals(requested)) {
            log.warn("User id={} with role={} requested srd content; coercing srd to false",
                    user.getId(), user.getRole());
        }

        return canMark && Boolean.TRUE.equals(requested);
    }

    /**
     * Resolves whether an {@code includeDeleted=true} request may actually apply.
     * <p>
     * Closes a real, currently-unenforced hole: every content list controller accepts
     * {@code ?includeDeleted=true} with no role check on the endpoint, even though the Javadoc
     * on the underlying repository queries claims it is ADMIN-only. This coerces the effective
     * value to false for any caller below MODERATOR, logging a warning, using the same
     * default-deny reading of {@link SecurityContextHolder} as {@link #mayViewNonSrd()} (null
     * authentication, unauthenticated, anonymous, or a non-{@link CustomUserDetails} principal
     * all resolve to false). This check is independent of the SRD gating kill switch — it always
     * applies.
     * </p>
     *
     * @param requested the requested includeDeleted flag from the caller
     * @return true only when the caller is MODERATOR+ and requested is true
     */
    public boolean resolveIncludeDeleted(boolean requested) {
        if (!requested) {
            return false;
        }

        User user = currentUserOrNull();
        boolean canInclude = user != null && roleHierarchyService.hasModeratorOrHigher(user);

        if (!canInclude) {
            log.warn("Caller below MODERATOR (or unauthenticated) requested includeDeleted=true; "
                            + "coercing to false. userId={}",
                    user != null ? user.getId() : "none");
        }

        return canInclude;
    }

    /**
     * Resolves the authenticated user from the security context, applying the default-deny rule
     * shared by {@link #mayViewNonSrd()} and {@link #resolveIncludeDeleted(boolean)}.
     *
     * @return the authenticated user, or null if there is no authentication, it is
     *         unauthenticated, it is an {@link AnonymousAuthenticationToken}, or its principal is
     *         not a {@link CustomUserDetails}
     */
    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userDetails.getUser();
    }
}
