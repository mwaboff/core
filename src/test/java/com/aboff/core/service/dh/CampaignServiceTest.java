package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationAccessRequest;
import com.aboff.core.model.dto.dh.response.CampaignInviteResponse;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.JoinCampaignResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CampaignInvite;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CampaignInviteRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CampaignService.
 * Tests all CRUD operations, user management, character sheet management, and access control.
 */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignInviteRepository campaignInviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private TransformationCardRepository transformationCardRepository;

    @Mock
    private CharacterSheetService characterSheetService;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CampaignService campaignService;

    // ==================== GET ALL CAMPAIGNS TESTS ====================

    @Test
    void getAllCampaigns_WithoutFilters_ReturnsPagedCampaigns() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        Campaign campaign1 = createTestCampaign(1L, "Campaign One", creator);
        Campaign campaign2 = createTestCampaign(2L, "Campaign Two", creator);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign1, campaign2));
        when(campaignRepository.findActiveWithFilters(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getAllCampaigns(0, 20, null, null, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getAllCampaigns_FilterByCreatorId_ReturnsFiltered() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        Campaign campaign = createTestCampaign(1L, "Campaign One", creator);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign));
        when(campaignRepository.findActiveWithFilters(eq(1L), eq(null), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getAllCampaigns(0, 20, 1L, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCreatorId()).isEqualTo(1L);
    }

    @Test
    void getAllCampaigns_FilterByName_ReturnsFiltered() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        Campaign campaign = createTestCampaign(1L, "Dragon Hunt", creator);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign));
        when(campaignRepository.findActiveWithFilters(eq(null), eq("Dragon"), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getAllCampaigns(0, 20, null, "Dragon", null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Dragon Hunt");
    }

    // ==================== GET CAMPAIGN BY ID TESTS ====================

    @Test
    void getCampaignById_AsParticipant_ReturnsCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act
        CampaignResponse result = campaignService.getCampaignById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Campaign");
    }

    @Test
    void getCampaignById_WithInvalidId_ThrowsEntityNotFoundException() {
        // Arrange
        when(campaignRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> campaignService.getCampaignById(999L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Campaign not found with id: 999");
    }

    @Test
    void getCampaignById_AsNonParticipant_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.getCampaignById(1L, null, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void getCampaignById_AsModerator_ReturnsCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User moderator = User.builder().id(2L).username("mod").role(Role.MODERATOR).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        CampaignResponse result = campaignService.getCampaignById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Campaign");
    }

    @Test
    void getCampaignById_WithExpansion_IncludesExpandedData() {
        // Arrange
        User creator = User.builder()
                .id(1L)
                .username("gm1")
                .email("gm1@example.com")
                .role(Role.USER)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act
        CampaignResponse result = campaignService.getCampaignById(1L, "creator", authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCreator()).isNotNull();
        assertThat(result.getCreator().getUsername()).isEqualTo("gm1");
    }

    // ==================== CREATE CAMPAIGN TESTS ====================

    @Test
    void createCampaign_WithValidData_CreatesCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();

        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("New Campaign")
                .description("A test campaign")
                .build();

        Campaign savedCampaign = createTestCampaign(1L, "New Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

        // Act
        CampaignResponse result = campaignService.createCampaign(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Campaign");
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void createCampaign_CreatorIsAutomaticallyGM() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();

        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("New Campaign")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        CampaignResponse result = campaignService.createCampaign(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getGameMasterIds()).contains(1L);
    }

    @Test
    void createCampaign_WithAdditionalGMsAndPlayers_AddsAllUsers() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User gm2 = User.builder().id(2L).username("gm2").build();
        User player1 = User.builder().id(3L).username("player1").build();

        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("New Campaign")
                .gameMasterIds(List.of(2L))
                .playerIds(List.of(3L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(gm2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(player1));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        CampaignResponse result = campaignService.createCampaign(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getGameMasterIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.getPlayerIds()).contains(3L);
    }

    // ==================== UPDATE CAMPAIGN TESTS ====================

    @Test
    void updateCampaign_AsCreator_UpdatesCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Old Name", creator);

        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name")
                .description("Updated description")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.updateCampaign(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void updateCampaign_AsModerator_UpdatesCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User moderator = User.builder().id(2L).username("mod").role(Role.MODERATOR).build();
        Campaign campaign = createTestCampaign(1L, "Old Name", creator);

        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        CampaignResponse result = campaignService.updateCampaign(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateCampaign_AsNonCreator_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Old Name", creator);

        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.updateCampaign(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateCampaign_PartialUpdate_OnlyUpdatesProvidedFields() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Original Name", creator);
        campaign.setDescription("Original Description");

        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("Updated Name")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.updateCampaign(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Original Description");
    }

    // ==================== DELETE CAMPAIGN TESTS ====================

    @Test
    void deleteCampaign_AsCreator_SoftDeletesCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act
        campaignService.deleteCampaign(1L, authentication);

        // Assert
        assertThat(campaign.isDeleted()).isTrue();
        verify(campaignRepository).save(campaign);
    }

    @Test
    void deleteCampaign_AsModerator_SoftDeletesCampaign() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User moderator = User.builder().id(2L).username("mod").role(Role.MODERATOR).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        campaignService.deleteCampaign(1L, authentication);

        // Assert
        assertThat(campaign.isDeleted()).isTrue();
        verify(campaignRepository).save(campaign);
    }

    @Test
    void deleteCampaign_AsNonCreator_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.deleteCampaign(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    // ==================== USER MANAGEMENT TESTS ====================

    @Test
    void addGameMaster_AsCreator_AddsGM() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User newGM = User.builder().id(2L).username("gm2").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newGM));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.addGameMaster(1L, 2L, authentication);

        // Assert
        assertThat(result.getGameMasterIds()).contains(2L);
    }

    @Test
    void addGameMaster_AsNonCreator_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User gm = User.builder().id(2L).username("gm2").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getGameMasters().add(gm);

        CustomUserDetails userDetails = new CustomUserDetails(gm);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.addGameMaster(1L, 3L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void removeGameMaster_AsCreator_RemovesGM() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User gm2 = User.builder().id(2L).username("gm2").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getGameMasters().add(gm2);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.removeGameMaster(1L, 2L, authentication);

        // Assert
        assertThat(result.getGameMasterIds()).doesNotContain(2L);
    }

    @Test
    void removeGameMaster_RemoveCreator_ThrowsIllegalStateException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.removeGameMaster(1L, 1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove the campaign creator");
    }

    @Test
    void addPlayer_AsGM_AddsPlayer() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User gm = User.builder().id(2L).username("gm2").role(Role.USER).build();
        User newPlayer = User.builder().id(3L).username("player1").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getGameMasters().add(gm);

        CustomUserDetails userDetails = new CustomUserDetails(gm);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newPlayer));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.addPlayer(1L, 3L, authentication);

        // Assert
        assertThat(result.getPlayerIds()).contains(3L);
    }

    @Test
    void addPlayer_AsNonGM_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.addPlayer(1L, 3L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void kickPlayer_AsGM_RemovesPlayerAndCascadesCharacterSheets() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User player = User.builder().id(2L).username("player1").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CharacterSheet playerSheet = CharacterSheet.builder()
                .id(10L).name("Hero").owner(player).build();
        campaign.getPlayerCharacters().add(playerSheet);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.kickPlayer(1L, 2L, authentication);

        // Assert
        assertThat(result.getPlayerIds()).doesNotContain(2L);
        assertThat(result.getPlayerCharacterIds()).doesNotContain(10L);
    }

    // ==================== CHARACTER SHEET MANAGEMENT TESTS ====================

    @Test
    void submitCharacterSheet_AsOwnerAndPlayer_AddsToPending() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(player)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(characterSheet));
        when(campaignRepository.isCharacterSheetInActiveCampaign(1L)).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.submitCharacterSheet(1L, 1L, authentication);

        // Assert
        assertThat(result.getPendingCharacterSheetIds()).contains(1L);
    }

    @Test
    void submitCharacterSheet_AsNonOwner_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        User sheetOwner = User.builder().id(3L).username("owner").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(sheetOwner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(characterSheet));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.submitCharacterSheet(1L, 1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("must be the owner");
    }

    @Test
    void submitCharacterSheet_AsNonPlayer_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User nonPlayer = User.builder().id(2L).username("nonplayer").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(nonPlayer)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(nonPlayer);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(characterSheet));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.submitCharacterSheet(1L, 1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("must be a player");
    }

    @Test
    void approveCharacterSheet_AsGM_MovesToPlayerCharacters() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User sheetOwner = User.builder().id(2L).username("player1").build();
        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(sheetOwner)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPendingCharacterSheets().add(characterSheet);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.approveCharacterSheet(1L, 1L, authentication);

        // Assert
        assertThat(result.getPendingCharacterSheetIds()).doesNotContain(1L);
        assertThat(result.getPlayerCharacterIds()).contains(1L);
    }

    @Test
    void approveCharacterSheet_NotInPending_ThrowsIllegalStateException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.approveCharacterSheet(1L, 999L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in pending list");
    }

    @Test
    void approveCharacterSheet_AsNonGM_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.approveCharacterSheet(1L, 1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void rejectCharacterSheet_AsGM_RemovesFromPending() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User sheetOwner = User.builder().id(2L).username("player1").build();
        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(sheetOwner)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPendingCharacterSheets().add(characterSheet);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.rejectCharacterSheet(1L, 1L, authentication);

        // Assert
        assertThat(result.getPendingCharacterSheetIds()).doesNotContain(1L);
    }

    @Test
    void rejectCharacterSheet_NotInPending_ThrowsIllegalStateException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.rejectCharacterSheet(1L, 999L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in pending list");
    }

    @Test
    void addNonPlayerCharacter_AsGM_AddsToNPCs() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Villain")
                .owner(creator)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(characterSheet));
        when(campaignRepository.isCharacterSheetInActiveCampaign(1L)).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.addNonPlayerCharacter(1L, 1L, authentication);

        // Assert
        assertThat(result.getNonPlayerCharacterIds()).contains(1L);
    }

    @Test
    void addNonPlayerCharacter_AsNonGM_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.addNonPlayerCharacter(1L, 1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void removeCharacterSheet_AsGM_RemovesFromAllCollections() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(creator)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayerCharacters().add(characterSheet);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.removeCharacterSheet(1L, 1L, authentication);

        // Assert
        assertThat(result.getPlayerCharacterIds()).doesNotContain(1L);
    }

    @Test
    void removeCharacterSheet_AsSheetOwner_RemovesSheet() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CharacterSheet sheet = CharacterSheet.builder()
                .id(5L).name("Hero").owner(player).build();
        campaign.getPlayerCharacters().add(sheet);

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.removeCharacterSheet(1L, 5L, authentication);

        // Assert
        assertThat(result.getPlayerCharacterIds()).doesNotContain(5L);
    }

    @Test
    void removeCharacterSheet_AsNonGMNonOwner_ThrowsInsufficientPermissionsException() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        User otherPlayer = User.builder().id(3L).username("player2").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);
        campaign.getPlayers().add(otherPlayer);

        CharacterSheet sheet = CharacterSheet.builder()
                .id(5L).name("Hero").owner(player).build();
        campaign.getPlayerCharacters().add(sheet);

        CustomUserDetails userDetails = new CustomUserDetails(otherPlayer);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert - otherPlayer is not the sheet owner and not a GM
        assertThatThrownBy(() -> campaignService.removeCharacterSheet(1L, 5L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    // ==================== GET MY CAMPAIGNS TESTS ====================

    @Test
    void getMyCampaigns_ReturnsUserCampaigns() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign1 = createTestCampaign(1L, "My Campaign", creator);
        Campaign campaign2 = createTestCampaign(2L, "Another Campaign", creator);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign1, campaign2));
        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveByUserInvolvement(eq(1L), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getMyCampaigns(0, 20, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ==================== GET USER CAMPAIGNS TESTS ====================

    @Test
    void getUserCampaigns_SelfAccess_ReturnsCampaigns() {
        // Arrange
        User user = User.builder().id(1L).username("player1").role(Role.USER).build();
        Campaign campaign1 = createTestCampaign(1L, "Campaign One", user);
        Campaign campaign2 = createTestCampaign(2L, "Campaign Two", user);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign1, campaign2));
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(campaignRepository.findActiveByUserInvolvement(eq(1L), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getUserCampaigns(1L, 0, 20, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getUserCampaigns_ModeratorAccess_ReturnsCampaigns() {
        // Arrange
        User moderator = User.builder().id(1L).username("mod").role(Role.MODERATOR).build();
        User targetUser = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Target Campaign", targetUser);

        Page<Campaign> campaignPage = new PageImpl<>(List.of(campaign));
        CustomUserDetails modDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(modDetails);
        when(roleHierarchyService.hasModeratorOrHigher(modDetails)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(campaignRepository.findActiveByUserInvolvement(eq(2L), any(Pageable.class)))
                .thenReturn(campaignPage);

        // Act
        PagedResponse<CampaignResponse> result = campaignService.getUserCampaigns(2L, 0, 20, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUserCampaigns_UnauthorizedAccess_ThrowsInsufficientPermissions() {
        // Arrange
        User regularUser = User.builder().id(1L).username("player1").role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(regularUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> campaignService.getUserCampaigns(2L, 0, 20, null, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void getUserCampaigns_UserNotFound_ThrowsEntityNotFound() {
        // Arrange
        User user = User.builder().id(999L).username("ghost").role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> campaignService.getUserCampaigns(999L, 0, 20, null, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getUserCampaigns_SizeExceedsMax_CapsAt100() {
        // Arrange
        User user = User.builder().id(1L).username("player1").role(Role.USER).build();
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(campaignRepository.findActiveByUserInvolvement(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        campaignService.getUserCampaigns(1L, 0, 200, null, authentication);

        // Assert - verify the pageable passed to the repository has size capped at 100
        verify(campaignRepository).findActiveByUserInvolvement(eq(1L), argThat(pageable ->
                pageable.getPageSize() == 100
        ));
    }

    // ==================== END CAMPAIGN TESTS ====================

    @Test
    void endCampaign_AsCreator_SetEndedAt() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.endCampaign(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsEnded()).isTrue();
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void endCampaign_AsNonCreator_ThrowsInsufficientPermissions() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.endCampaign(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void endCampaign_AlreadyEnded_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.endCampaign(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already ended");
    }

    // ==================== GENERATE INVITE TESTS ====================

    @Test
    void generateInvite_AsGM_CreatesInvite() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignInviteRepository.save(any(CampaignInvite.class))).thenAnswer(invocation -> {
            CampaignInvite saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        CampaignInviteResponse result = campaignService.generateInvite(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCampaignId()).isEqualTo(1L);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getExpiresAt()).isNotNull();
        verify(campaignInviteRepository).save(any(CampaignInvite.class));
    }

    @Test
    void generateInvite_EndedCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.generateInvite(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    // ==================== JOIN VIA INVITE TESTS ====================

    @Test
    void joinViaInvite_ValidToken_AddsPlayerAndMarksUsed() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User joiner = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CampaignInvite invite = CampaignInvite.builder()
                .id(1L)
                .campaign(campaign)
                .token("valid-token")
                .createdBy(1L)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(joiner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignInviteRepository.findByToken("valid-token")).thenReturn(Optional.of(invite));
        when(userRepository.findById(2L)).thenReturn(Optional.of(joiner));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        JoinCampaignResponse result = campaignService.joinViaInvite("valid-token", authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCampaignId()).isEqualTo(1L);
        assertThat(result.getCampaignName()).isEqualTo("Test Campaign");
        assertThat(campaign.getPlayers()).contains(joiner);
        assertThat(invite.getUsedBy()).isEqualTo(2L);
        assertThat(invite.getUsedAt()).isNotNull();
        verify(campaignInviteRepository).save(invite);
    }

    @Test
    void joinViaInvite_ExpiredToken_ThrowsIllegalState() {
        // Arrange
        User joiner = User.builder().id(2L).username("player1").role(Role.USER).build();

        CampaignInvite invite = CampaignInvite.builder()
                .id(1L)
                .token("expired-token")
                .createdBy(1L)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(joiner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignInviteRepository.findByToken("expired-token")).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.joinViaInvite("expired-token", authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired or already used");
    }

    @Test
    void joinViaInvite_UsedToken_ThrowsIllegalState() {
        // Arrange
        User joiner = User.builder().id(2L).username("player1").role(Role.USER).build();

        CampaignInvite invite = CampaignInvite.builder()
                .id(1L)
                .token("used-token")
                .createdBy(1L)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .usedBy(3L)
                .usedAt(LocalDateTime.now().minusHours(1))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(joiner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignInviteRepository.findByToken("used-token")).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.joinViaInvite("used-token", authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired or already used");
    }

    @Test
    void joinViaInvite_EndedCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User joiner = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        CampaignInvite invite = CampaignInvite.builder()
                .id(1L)
                .campaign(campaign)
                .token("ended-campaign-token")
                .createdBy(1L)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(joiner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignInviteRepository.findByToken("ended-campaign-token")).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.joinViaInvite("ended-campaign-token", authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    @Test
    void joinViaInvite_AlreadyMember_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CampaignInvite invite = CampaignInvite.builder()
                .id(1L)
                .campaign(campaign)
                .token("member-token")
                .createdBy(1L)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // Creator is already involved as GM
        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignInviteRepository.findByToken("member-token")).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.joinViaInvite("member-token", authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already a member");
    }

    // ==================== LEAVE CAMPAIGN TESTS ====================

    @Test
    void leaveCampaign_AsPlayer_RemovesSelf() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.leaveCampaign(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayerIds()).doesNotContain(2L);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void leaveCampaign_AsNonPlayer_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User nonPlayer = User.builder().id(2L).username("other").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        CustomUserDetails userDetails = new CustomUserDetails(nonPlayer);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.leaveCampaign(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a player");
    }

    // ==================== ENDED CAMPAIGN CONSTRAINT TESTS ====================

    @Test
    void submitCharacterSheet_AlreadyInActiveCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);

        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(player)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(player);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(characterSheet));
        when(campaignRepository.isCharacterSheetInActiveCampaign(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> campaignService.submitCharacterSheet(1L, 1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in an active campaign");
    }

    @Test
    void updateCampaign_EndedCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.updateCampaign(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    @Test
    void addPlayer_EndedCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.addPlayer(1L, 2L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    @Test
    void addGameMaster_EndedCampaign_ThrowsIllegalState() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThatThrownBy(() -> campaignService.addGameMaster(1L, 2L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    @Test
    void removeCharacterSheet_OnEndedCampaign_Succeeds() {
        // Arrange
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        CharacterSheet characterSheet = CharacterSheet.builder()
                .id(1L)
                .name("Hero")
                .owner(creator)
                .build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.setEndedAt(LocalDateTime.now());
        campaign.getPlayerCharacters().add(characterSheet);

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CampaignResponse result = campaignService.removeCharacterSheet(1L, 1L, authentication);

        // Assert
        assertThat(result.getPlayerCharacterIds()).doesNotContain(1L);
        verify(campaignRepository).save(any(Campaign.class));
    }

    // ==================== TRANSFORMATION ACCESS TESTS ====================

    @Test
    void updateTransformationAccess_AsCreator_EnablesTransformations() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", creator);
        campaign.getPlayerCharacters().add(sheet);

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(characterSheetService.toResponse(any(CharacterSheet.class), anySet()))
                .thenReturn(CharacterSheetResponse.builder().id(10L).transformationEnabled(true).build());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .build();

        CharacterSheetResponse result = campaignService.updateTransformationAccess(1L, 10L, request, authentication);

        assertThat(result.isTransformationEnabled()).isTrue();
        assertThat(sheet.isTransformationEnabled()).isTrue();
    }

    @Test
    void updateTransformationAccess_EndedCampaign_ThrowsIllegalState() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayerCharacters().add(createTestCharacterSheet(10L, "Hero", creator));
        campaign.setEndedAt(LocalDateTime.now());

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .build();

        assertThatThrownBy(() -> campaignService.updateTransformationAccess(1L, 10L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ended campaign");
    }

    @Test
    void updateTransformationAccess_Disable_PreservesCardTokensAndWolfForm() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", creator);
        TransformationCard card = createTestTransformationCard(5L, "Werewolf");
        sheet.setTransformationEnabled(true);
        sheet.setTransformationCard(card);
        sheet.setTransformationTokens(3);
        sheet.setWolfFormActive(true);
        campaign.getPlayerCharacters().add(sheet);

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(characterSheetService.toResponse(any(CharacterSheet.class), anySet()))
                .thenReturn(CharacterSheetResponse.builder().id(10L).build());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(false)
                .build();

        campaignService.updateTransformationAccess(1L, 10L, request, authentication);

        assertThat(sheet.isTransformationEnabled()).isFalse();
        assertThat(sheet.getTransformationCard()).isSameAs(card);
        assertThat(sheet.getTransformationTokens()).isEqualTo(3);
        assertThat(sheet.getWolfFormActive()).isTrue();
    }

    @Test
    void updateTransformationAccess_AssignsCardWhileDisabled() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", creator);
        campaign.getPlayerCharacters().add(sheet);
        TransformationCard card = createTestTransformationCard(5L, "Vampire");

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(card));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(characterSheetService.toResponse(any(CharacterSheet.class), anySet()))
                .thenReturn(CharacterSheetResponse.builder().id(10L).build());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(false)
                .transformationCardId(5L)
                .build();

        campaignService.updateTransformationAccess(1L, 10L, request, authentication);

        assertThat(sheet.isTransformationEnabled()).isFalse();
        assertThat(sheet.getTransformationCard()).isSameAs(card);
    }

    @Test
    void updateTransformationAccess_ClearFlag_TakesPrecedenceOverCardId() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", creator);
        sheet.setTransformationCard(createTestTransformationCard(5L, "Werewolf"));
        sheet.setTransformationTokens(2);
        sheet.setWolfFormActive(true);
        campaign.getPlayerCharacters().add(sheet);

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(characterSheetService.toResponse(any(CharacterSheet.class), anySet()))
                .thenReturn(CharacterSheetResponse.builder().id(10L).build());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .transformationCardId(9L)
                .clearTransformationCard(true)
                .build();

        campaignService.updateTransformationAccess(1L, 10L, request, authentication);

        assertThat(sheet.getTransformationCard()).isNull();
        assertThat(sheet.getTransformationTokens()).isNull();
        assertThat(sheet.getWolfFormActive()).isFalse();
        verify(transformationCardRepository, never()).findByIdAndDeletedAtIsNull(anyLong());
    }

    @Test
    void updateTransformationAccess_UnknownCardId_ThrowsEntityNotFound() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", creator);
        campaign.getPlayerCharacters().add(sheet);

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .transformationCardId(999L)
                .build();

        assertThatThrownBy(() -> campaignService.updateTransformationAccess(1L, 10L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("TransformationCard not found");
    }

    @Test
    void updateTransformationAccess_SheetNotInCampaign_ThrowsEntityNotFound() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);

        mockAuthenticatedUser(creator);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .build();

        assertThatThrownBy(() -> campaignService.updateTransformationAccess(1L, 99L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 99");
    }

    @Test
    void updateTransformationAccess_AsUnrelatedPlayer_ThrowsInsufficientPermissions() {
        User creator = User.builder().id(1L).username("gm1").role(Role.USER).build();
        User player = User.builder().id(2L).username("player1").role(Role.USER).build();
        Campaign campaign = createTestCampaign(1L, "Test Campaign", creator);
        campaign.getPlayers().add(player);
        CharacterSheet sheet = createTestCharacterSheet(10L, "Hero", player);
        campaign.getPlayerCharacters().add(sheet);

        mockAuthenticatedUser(player);
        when(campaignRepository.findActiveById(1L)).thenReturn(Optional.of(campaign));

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .build();

        assertThatThrownBy(() -> campaignService.updateTransformationAccess(1L, 10L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateTransformationAccess_CampaignNotFound_ThrowsEntityNotFound() {
        when(campaignRepository.findActiveById(99L)).thenReturn(Optional.empty());

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .build();

        assertThatThrownBy(() -> campaignService.updateTransformationAccess(99L, 10L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Campaign not found");
    }

    // ==================== HELPER METHODS ====================

    private void mockAuthenticatedUser(User user) {
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
    }

    private CharacterSheet createTestCharacterSheet(Long id, String name, User owner) {
        return CharacterSheet.builder()
                .id(id)
                .name(name)
                .level(1)
                .owner(owner)
                .build();
    }

    private TransformationCard createTestTransformationCard(Long id, String name) {
        Expansion expansion = Expansion.builder().id(3L).name("Hope & Fear").build();
        return TransformationCard.builder().id(id).name(name).expansion(expansion).build();
    }

    private Campaign createTestCampaign(Long id, String name, User creator) {
        Campaign campaign = Campaign.builder()
                .id(id)
                .name(name)
                .creator(creator)
                .gameMasters(new HashSet<>())
                .players(new HashSet<>())
                .pendingCharacterSheets(new HashSet<>())
                .playerCharacters(new HashSet<>())
                .nonPlayerCharacters(new HashSet<>())
                .build();
        campaign.getGameMasters().add(creator);
        return campaign;
    }
}
