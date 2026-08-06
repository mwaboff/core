package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.service.dh.ItemAccessService.VisibilityScope;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ItemAccessService}.
 * <p>
 * Uses the real {@link RoleHierarchyService} rather than a mock. It is stateless ordinal
 * comparison with no collaborators, so stubbing it would mean these tests asserted against
 * hand-written role answers instead of the actual hierarchy — exactly the thing most worth
 * verifying here.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemAccessServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private Authentication authentication;

    private ItemAccessService itemAccessService;

    private User regularUser;
    private User otherUser;
    private User moderatorUser;
    private User adminUser;
    private User ownerUser;
    private Expansion expansion;

    @BeforeEach
    void setUp() {
        itemAccessService = new ItemAccessService(
                campaignRepository, expansionRepository, new RoleHierarchyService());

        regularUser = User.builder().id(1L).username("regular").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        moderatorUser = User.builder().id(3L).username("mod").role(Role.MODERATOR).build();
        adminUser = User.builder().id(4L).username("admin").role(Role.ADMIN).build();
        ownerUser = User.builder().id(5L).username("owner").role(Role.OWNER).build();

        expansion = Expansion.builder().id(10L).name("Core Set").isPublished(true).build();
    }

    private void authenticateAs(User user) {
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
    }

    private Weapon weapon(boolean official, User creator) {
        return Weapon.builder().id(100L).name("Test Blade").tier(1)
                .isOfficial(official).isPublic(false).createdBy(creator).build();
    }

    // ==================== VISIBILITY SCOPE ====================

    @Test
    void visibilityScope_ForUserInCampaigns_ReturnsThoseCampaignIds() {
        authenticateAs(regularUser);
        when(campaignRepository.findActiveCampaignIdsByUserInvolvement(1L)).thenReturn(List.of(7L, 8L));

        VisibilityScope scope = itemAccessService.visibilityScope(authentication);

        assertThat(scope.memberCampaignIds()).containsExactly(7L, 8L);
    }

    @Test
    void visibilityScope_ForUserInNoCampaigns_UsesNonMatchingSentinelNotEmptyList() {
        authenticateAs(regularUser);
        when(campaignRepository.findActiveCampaignIdsByUserInvolvement(1L)).thenReturn(List.of());

        VisibilityScope scope = itemAccessService.visibilityScope(authentication);

        // An empty IN () list is a hard runtime failure in PostgreSQL, so this must never be empty.
        assertThat(scope.memberCampaignIds()).isNotEmpty().containsExactly(-1L);
    }

    @Test
    void visibilityScope_ForRegularUser_IsNotPrivileged() {
        authenticateAs(regularUser);
        when(campaignRepository.findActiveCampaignIdsByUserInvolvement(anyLong())).thenReturn(List.of());

        assertThat(itemAccessService.visibilityScope(authentication).privileged()).isFalse();
    }

    @Test
    void visibilityScope_ForModerator_IsPrivileged() {
        authenticateAs(moderatorUser);
        when(campaignRepository.findActiveCampaignIdsByUserInvolvement(anyLong())).thenReturn(List.of());

        assertThat(itemAccessService.visibilityScope(authentication).privileged()).isTrue();
    }

    @Test
    void visibilityScope_CarriesTheCallersUserId() {
        authenticateAs(regularUser);
        when(campaignRepository.findActiveCampaignIdsByUserInvolvement(anyLong())).thenReturn(List.of());

        assertThat(itemAccessService.visibilityScope(authentication).userId()).isEqualTo(1L);
    }

    // ==================== FLAG COERCION ====================

    @Test
    void resolveIsOfficial_ForRegularUserRequestingOfficial_CoercesToFalse() {
        assertThat(itemAccessService.resolveIsOfficial(regularUser, true)).isFalse();
    }

    @Test
    void resolveIsOfficial_ForModeratorRequestingOfficial_ReturnsTrue() {
        assertThat(itemAccessService.resolveIsOfficial(moderatorUser, true)).isTrue();
    }

    @Test
    void resolveIsOfficial_ForModeratorNotRequesting_ReturnsFalse() {
        assertThat(itemAccessService.resolveIsOfficial(moderatorUser, null)).isFalse();
    }

    @Test
    void resolveIsPublic_ForRegularUserRequestingPublic_CoercesToFalse() {
        assertThat(itemAccessService.resolveIsPublic(regularUser, true)).isFalse();
    }

    @Test
    void resolveIsPublic_ForModeratorRequestingPublic_ReturnsTrue() {
        assertThat(itemAccessService.resolveIsPublic(moderatorUser, true)).isTrue();
    }

    @Test
    void resolveIsPublic_ForAdminRequestingPublic_ReturnsTrue() {
        assertThat(itemAccessService.resolveIsPublic(adminUser, true)).isTrue();
    }

    // ==================== EXPANSION RESOLUTION ====================

    @Test
    void resolveExpansion_ForCustomItem_ReturnsNullEvenWhenOneIsRequested() {
        assertThat(itemAccessService.resolveExpansion(regularUser, 10L, false)).isNull();
    }

    @Test
    void resolveExpansion_ForOfficialItem_ReturnsTheExpansion() {
        when(expansionRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(expansion));

        assertThat(itemAccessService.resolveExpansion(moderatorUser, 10L, true)).isEqualTo(expansion);
    }

    @Test
    void resolveExpansion_ForOfficialItemWithNoExpansionNamed_ReturnsNull() {
        assertThat(itemAccessService.resolveExpansion(moderatorUser, null, true)).isNull();
    }

    @Test
    void resolveExpansion_ForOfficialItemWithUnknownExpansion_Throws() {
        when(expansionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemAccessService.resolveExpansion(moderatorUser, 99L, true))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void resolveExpansion_ForCustomItem_NeverQueriesTheExpansionRepository() {
        // A custom item's expansion is dropped outright, so a bogus id must not even be looked up.
        assertThat(itemAccessService.resolveExpansion(regularUser, 99L, false)).isNull();
    }

    // ==================== CAMPAIGN RESOLUTION ====================

    private Campaign campaignOwnedBy(Long id, User creator) {
        return Campaign.builder().id(id).name("Campaign " + id).creator(creator).build();
    }

    @Test
    void resolveCampaigns_WithNullList_ReturnsNullToLeaveTagsUnchanged() {
        assertThat(itemAccessService.resolveCampaigns(regularUser, null)).isNull();
    }

    @Test
    void resolveCampaigns_WithEmptyList_ReturnsEmptySetToClearTags() {
        assertThat(itemAccessService.resolveCampaigns(regularUser, List.of())).isEmpty();
    }

    @Test
    void resolveCampaigns_ForCampaignTheUserCreated_ResolvesIt() {
        Campaign campaign = campaignOwnedBy(7L, regularUser);
        when(campaignRepository.findActiveById(7L)).thenReturn(Optional.of(campaign));

        Set<Campaign> resolved = itemAccessService.resolveCampaigns(regularUser, List.of(7L));

        assertThat(resolved).containsExactly(campaign);
    }

    @Test
    void resolveCampaigns_ForCampaignTheUserIsNotPartOf_Throws() {
        when(campaignRepository.findActiveById(7L)).thenReturn(Optional.of(campaignOwnedBy(7L, otherUser)));

        assertThatThrownBy(() -> itemAccessService.resolveCampaigns(regularUser, List.of(7L)))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("7");
    }

    @Test
    void resolveCampaigns_ForModeratorOnACampaignTheyAreNotPartOf_IsAllowed() {
        Campaign campaign = campaignOwnedBy(7L, otherUser);
        when(campaignRepository.findActiveById(7L)).thenReturn(Optional.of(campaign));

        assertThat(itemAccessService.resolveCampaigns(moderatorUser, List.of(7L))).containsExactly(campaign);
    }

    @Test
    void resolveCampaigns_ForDeletedOrUnknownCampaign_Throws() {
        when(campaignRepository.findActiveById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemAccessService.resolveCampaigns(regularUser, List.of(7L)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("7");
    }

    // ==================== MODIFY PERMISSION ====================

    @Test
    void validateModifyPermission_ForOfficialItemAsAdmin_IsAllowed() {
        // Official items must stay editable by ADMIN, who run the content import pipeline.
        authenticateAs(adminUser);

        assertThatCode(() -> itemAccessService.validateModifyPermission(
                weapon(true, null), "weapon", authentication)).doesNotThrowAnyException();
    }

    @Test
    void validateModifyPermission_ForOfficialItemAsOwner_IsAllowed() {
        authenticateAs(ownerUser);

        assertThatCode(() -> itemAccessService.validateModifyPermission(
                weapon(true, null), "weapon", authentication)).doesNotThrowAnyException();
    }

    @Test
    void validateModifyPermission_ForOfficialItemAsModerator_Throws() {
        authenticateAs(moderatorUser);

        assertThatThrownBy(() -> itemAccessService.validateModifyPermission(
                weapon(true, null), "weapon", authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void validateModifyPermission_ForOfficialItemAsRegularUser_Throws() {
        authenticateAs(regularUser);

        assertThatThrownBy(() -> itemAccessService.validateModifyPermission(
                weapon(true, null), "weapon", authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void validateModifyPermission_ForOwnCustomItem_IsAllowed() {
        authenticateAs(regularUser);

        assertThatCode(() -> itemAccessService.validateModifyPermission(
                weapon(false, regularUser), "weapon", authentication)).doesNotThrowAnyException();
    }

    @Test
    void validateModifyPermission_ForSomeoneElsesCustomItem_Throws() {
        authenticateAs(regularUser);

        assertThatThrownBy(() -> itemAccessService.validateModifyPermission(
                weapon(false, otherUser), "weapon", authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("weapon");
    }

    @Test
    void validateModifyPermission_ForSomeoneElsesCustomItemAsModerator_IsAllowed() {
        authenticateAs(moderatorUser);

        assertThatCode(() -> itemAccessService.validateModifyPermission(
                weapon(false, otherUser), "weapon", authentication)).doesNotThrowAnyException();
    }

    @Test
    void validateModifyPermission_ForCreatorlessCustomItemAsRegularUser_Throws() {
        // An official row later demoted to custom has no creator and belongs to nobody.
        authenticateAs(regularUser);

        assertThatThrownBy(() -> itemAccessService.validateModifyPermission(
                weapon(false, null), "weapon", authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void validateModifyPermission_ForCreatorlessCustomItemAsModerator_IsAllowed() {
        authenticateAs(moderatorUser);

        assertThatCode(() -> itemAccessService.validateModifyPermission(
                weapon(false, null), "weapon", authentication)).doesNotThrowAnyException();
    }

    // ==================== MISC ====================

    @Test
    void requireModerator_ForRegularUser_Throws() {
        authenticateAs(regularUser);

        assertThatThrownBy(() -> itemAccessService.requireModerator(authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void requireModerator_ForModerator_IsAllowed() {
        authenticateAs(moderatorUser);

        assertThatCode(() -> itemAccessService.requireModerator(authentication))
                .doesNotThrowAnyException();
    }

    @Test
    void currentUser_ReturnsThePrincipalsUser() {
        authenticateAs(regularUser);

        assertThat(itemAccessService.currentUser(authentication)).isEqualTo(regularUser);
    }
}
