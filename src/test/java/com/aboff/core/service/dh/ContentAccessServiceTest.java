package com.aboff.core.service.dh;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.BaseItem;
import com.aboff.core.model.entity.dh.Card;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.CardType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.RoleHierarchyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContentAccessService}.
 * <p>
 * Uses the real {@link RoleHierarchyService} rather than a mock, for the same reason
 * {@code ItemAccessServiceTest} does: it is stateless ordinal comparison with no
 * collaborators, so stubbing it would mean these tests asserted against hand-written role
 * answers instead of the actual hierarchy.
 * </p>
 * <p>
 * The service reads {@link SecurityContextHolder} directly rather than taking an
 * {@code Authentication} parameter, so each test that needs an authenticated caller
 * populates the (thread-local) security context itself, and {@link #clearContext()} resets it
 * afterward so no test leaks its authentication into the next one.
 * </p>
 */
class ContentAccessServiceTest {

    private ContentAccessService contentAccessService;

    private User regularUser;
    private User moderatorUser;
    private User adminUser;
    private User ownerUser;
    private User accessGrantedUser;

    @BeforeEach
    void setUp() {
        contentAccessService = new ContentAccessService(new RoleHierarchyService());

        regularUser = User.builder().id(1L).username("regular").role(Role.USER).build();
        moderatorUser = User.builder().id(2L).username("mod").role(Role.MODERATOR).build();
        adminUser = User.builder().id(3L).username("admin").role(Role.ADMIN).build();
        ownerUser = User.builder().id(4L).username("owner").role(Role.OWNER).build();
        accessGrantedUser = User.builder().id(5L).username("granted").role(Role.USER)
                .accessAllExpansions(true).build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void enableGating() {
        ReflectionTestUtils.setField(contentAccessService, "srdGatingEnabled", true);
    }

    private void authenticateAs(User user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        new CustomUserDetails(user), null, authorities));
    }

    private Weapon weapon(boolean official, boolean srd) {
        return Weapon.builder().id(100L).name("Test Blade").tier(1)
                .isOfficial(official).srd(srd).build();
    }

    private Domain domain(boolean official, boolean srd) {
        return Domain.builder().id(200L).name("Test Domain").isOfficial(official).srd(srd)
                .expansion(Expansion.builder().id(1L).name("Core Set").build())
                .build();
    }

    // ==================== KILL SWITCH OFF ====================

    @Test
    void mayViewNonSrd_WithGatingDisabled_IsPermittedWithNoAuthentication() {
        // The kill switch defaults off, and the feature must be fully inert until flipped.
        assertThat(contentAccessService.mayViewNonSrd()).isTrue();
    }

    @Test
    void mayView_WithGatingDisabled_PermitsOfficialNonSrdContent() {
        assertThat(contentAccessService.mayView(true, false)).isTrue();
    }

    @Test
    void resolveSrd_WithGatingDisabled_StillCoercesBelowAdmin() {
        // The kill switch only affects read-side visibility; write-side coercion is unconditional.
        assertThat(contentAccessService.resolveSrd(regularUser, true)).isFalse();
    }

    // ==================== DEFAULT-DENY PRINCIPAL SHAPES ====================

    @Test
    void mayViewNonSrd_WithGatingEnabledAndNoAuthentication_IsDenied() {
        enableGating();
        SecurityContextHolder.clearContext();

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    @Test
    void mayViewNonSrd_WithGatingEnabledAndUnauthenticatedToken_IsDenied() {
        enableGating();
        TestingAuthenticationToken unauthenticated =
                new TestingAuthenticationToken(new CustomUserDetails(adminUser), null);
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    @Test
    void mayViewNonSrd_WithGatingEnabledAndAnonymousToken_IsDenied() {
        enableGating();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    @Test
    void mayViewNonSrd_WithGatingEnabledAndNonCustomUserDetailsPrincipal_IsDenied() {
        enableGating();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-custom-user-details", null, List.of()));

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    // ==================== ROLE THRESHOLD ====================

    @Test
    void mayViewNonSrd_ForRegularUser_IsDenied() {
        enableGating();
        authenticateAs(regularUser);

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    @Test
    void mayViewNonSrd_ForModerator_IsDenied() {
        // Deliberate spec decision: MODERATOR does not imply non-SRD access, unlike the
        // MODERATOR-or-higher threshold ItemAccessService uses for official/public flags.
        enableGating();
        authenticateAs(moderatorUser);

        assertThat(contentAccessService.mayViewNonSrd()).isFalse();
    }

    @Test
    void mayViewNonSrd_ForAdmin_IsAllowed() {
        enableGating();
        authenticateAs(adminUser);

        assertThat(contentAccessService.mayViewNonSrd()).isTrue();
    }

    @Test
    void mayViewNonSrd_ForOwner_IsAllowed() {
        enableGating();
        authenticateAs(ownerUser);

        assertThat(contentAccessService.mayViewNonSrd()).isTrue();
    }

    @Test
    void mayViewNonSrd_ForRegularUserWithAccessAllExpansionsGrant_IsAllowed() {
        enableGating();
        authenticateAs(accessGrantedUser);

        assertThat(contentAccessService.mayViewNonSrd()).isTrue();
    }

    @Test
    void includeNonSrd_DelegatesToMayViewNonSrd() {
        enableGating();
        authenticateAs(adminUser);

        assertThat(contentAccessService.includeNonSrd()).isTrue();
    }

    // ==================== MAY VIEW (redaction check) ====================

    @Test
    void mayView_ForCustomContent_IsAlwaysAllowed() {
        // isOfficial=false is never gated, regardless of srd or the caller's access.
        enableGating();
        authenticateAs(regularUser);

        assertThat(contentAccessService.mayView(false, false)).isTrue();
    }

    @Test
    void mayView_ForOfficialSrdContent_IsAllowedForRegularUser() {
        enableGating();
        authenticateAs(regularUser);

        assertThat(contentAccessService.mayView(true, true)).isTrue();
    }

    @Test
    void mayView_ForOfficialNonSrdContent_IsDeniedForRegularUser() {
        enableGating();
        authenticateAs(regularUser);

        assertThat(contentAccessService.mayView(true, false)).isFalse();
    }

    @Test
    void mayView_TreatsNullIsOfficialAndSrdAsFalse() {
        enableGating();
        authenticateAs(regularUser);

        // null isOfficial is not TRUE, so this is treated as custom content and always visible.
        assertThat(contentAccessService.mayView(null, null)).isTrue();
    }

    @Test
    void mayView_ForCard_DelegatesToTheCoreOverload() {
        enableGating();
        authenticateAs(regularUser);

        Card officialNonSrdDomainCard = DomainCard.builder()
                .id(1L).name("Test Card").cardType(CardType.DOMAIN)
                .isOfficial(true).srd(false).build();

        assertThat(contentAccessService.mayView(officialNonSrdDomainCard)).isFalse();
    }

    @Test
    void mayView_ForBaseItem_DelegatesToTheCoreOverload() {
        enableGating();
        authenticateAs(adminUser);

        BaseItem officialNonSrdWeapon = weapon(true, false);

        assertThat(contentAccessService.mayView(officialNonSrdWeapon)).isTrue();
    }

    @Test
    void mayView_ForStandaloneEntity_UsesTheCoreOverloadDirectly() {
        // The 12 standalone gated entities (Domain, Class, Adversary, ...) share no common
        // supertype carrying isOfficial/srd, so callers use mayView(Boolean, Boolean) directly.
        enableGating();
        authenticateAs(regularUser);

        Domain officialNonSrdDomain = domain(true, false);

        assertThat(contentAccessService.mayView(officialNonSrdDomain.getIsOfficial(),
                officialNonSrdDomain.getSrd())).isFalse();
    }

    // ==================== RESOLVE SRD (write-side coercion) ====================

    @Test
    void resolveSrd_ForRegularUserRequestingSrd_CoercesToFalse() {
        assertThat(contentAccessService.resolveSrd(regularUser, true)).isFalse();
    }

    @Test
    void resolveSrd_ForModeratorRequestingSrd_CoercesToFalse() {
        // The threshold is ADMIN or higher, not MODERATOR, unlike ItemAccessService's flags.
        assertThat(contentAccessService.resolveSrd(moderatorUser, true)).isFalse();
    }

    @Test
    void resolveSrd_ForAdminRequestingSrd_ReturnsTrue() {
        assertThat(contentAccessService.resolveSrd(adminUser, true)).isTrue();
    }

    @Test
    void resolveSrd_ForOwnerRequestingSrd_ReturnsTrue() {
        assertThat(contentAccessService.resolveSrd(ownerUser, true)).isTrue();
    }

    @Test
    void resolveSrd_ForAdminNotRequesting_ReturnsFalse() {
        assertThat(contentAccessService.resolveSrd(adminUser, null)).isFalse();
    }

    // ==================== RESOLVE INCLUDE DELETED ====================

    @Test
    void resolveIncludeDeleted_WhenNotRequested_ReturnsFalseWithNoAuthenticationNeeded() {
        SecurityContextHolder.clearContext();

        assertThat(contentAccessService.resolveIncludeDeleted(false)).isFalse();
    }

    @Test
    void resolveIncludeDeleted_ForRegularUserRequesting_CoercesToFalse() {
        authenticateAs(regularUser);

        assertThat(contentAccessService.resolveIncludeDeleted(true)).isFalse();
    }

    @Test
    void resolveIncludeDeleted_ForModeratorRequesting_ReturnsTrue() {
        authenticateAs(moderatorUser);

        assertThat(contentAccessService.resolveIncludeDeleted(true)).isTrue();
    }

    @Test
    void resolveIncludeDeleted_ForAdminRequesting_ReturnsTrue() {
        authenticateAs(adminUser);

        assertThat(contentAccessService.resolveIncludeDeleted(true)).isTrue();
    }

    @Test
    void resolveIncludeDeleted_WithNoAuthenticationRequesting_CoercesToFalse() {
        // Independent of the SRD gating kill switch -- this ADMIN-only hole-closing check
        // always applies, so it must default-deny even while srdGatingEnabled is false.
        SecurityContextHolder.clearContext();

        assertThat(contentAccessService.resolveIncludeDeleted(true)).isFalse();
    }
}
