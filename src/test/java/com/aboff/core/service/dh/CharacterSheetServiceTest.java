package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.request.InventoryArmorRequest;
import com.aboff.core.model.dto.dh.request.InventoryLootRequest;
import com.aboff.core.model.dto.dh.request.InventoryWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.response.*;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CharacterSheetService.
 * Tests all CRUD operations, access control, pagination, filtering, and expansion.
 */
@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private WeaponRepository weaponRepository;

    @Mock
    private ArmorRepository armorRepository;

    @Mock
    private CommunityCardRepository communityCardRepository;

    @Mock
    private AncestryCardRepository ancestryCardRepository;

    @Mock
    private SubclassCardRepository subclassCardRepository;

    @Mock
    private DomainCardRepository domainCardRepository;

    @Mock
    private LootRepository lootRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CharacterSheetDomainCardRepository characterSheetDomainCardRepository;

    @Mock
    private CharacterSheetWeaponRepository characterSheetWeaponRepository;

    @Mock
    private CharacterSheetArmorRepository characterSheetArmorRepository;

    @Mock
    private CharacterSheetLootRepository characterSheetLootRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private WeaponService weaponService;

    @Mock
    private ArmorService armorService;

    @Mock
    private CommunityCardService communityCardService;

    @Mock
    private AncestryCardService ancestryCardService;

    @Mock
    private SubclassCardService subclassCardService;

    @Mock
    private DomainCardService domainCardService;

    @Mock
    private LootService lootService;

    @Mock
    private ClassService classService;

    @Mock
    private TransformationCardRepository transformationCardRepository;

    @Mock
    private MartialStanceRepository martialStanceRepository;

    @Mock
    private TransformationCardService transformationCardService;

    @Mock
    private MartialStanceService martialStanceService;

    @Mock
    private CompanionRepository companionRepository;

    @Mock
    private CompanionService companionService;

    @Mock
    private UserService userService;

    @Mock
    private CharacterAdvancementLogRepository characterAdvancementLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CharacterSheetService characterSheetService;

    // ==================== GET ALL CHARACTER SHEETS TESTS ====================

    @Test
    void getAllCharacterSheets_AsPrivilegedUser_WithoutFilters_ReturnsPagedSheets() {
        // Arrange
        User moderator = User.builder().id(10L).username("moderator").role(Role.MODERATOR).build();
        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.MODERATOR)).thenReturn(true);

        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet1 = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        CharacterSheet sheet2 = CharacterSheet.builder()
                .id(2L)
                .name("Legolas")
                .level(6)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet1, sheet2));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, null, null, null, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Aragorn");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Legolas");
    }

    @Test
    void getAllCharacterSheets_AsPrivilegedUser_FilterByOwnerId_ReturnsFiltered() {
        // Arrange
        User moderator = User.builder().id(10L).username("moderator").role(Role.MODERATOR).build();
        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.MODERATOR)).thenReturn(true);

        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(1L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, 1L, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOwnerId()).isEqualTo(1L);
        verify(characterSheetRepository).findActiveWithFilters(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_AsPrivilegedUser_FilterByName_ReturnsFiltered() {
        // Arrange
        User moderator = User.builder().id(10L).username("moderator").role(Role.MODERATOR).build();
        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.MODERATOR)).thenReturn(true);

        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq("Ara"), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, "Ara", null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Aragorn");
        verify(characterSheetRepository).findActiveWithFilters(eq(null), eq("Ara"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_AsPrivilegedUser_FilterByLevelRange_ReturnsFiltered() {
        // Arrange
        User moderator = User.builder().id(10L).username("moderator").role(Role.MODERATOR).build();
        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.MODERATOR)).thenReturn(true);

        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq(null), eq(3), eq(7), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, null, 3, 7, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLevel()).isEqualTo(5);
        verify(characterSheetRepository).findActiveWithFilters(eq(null), eq(null), eq(3), eq(7), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_AsRegularUser_ForcesOwnerIdToOwnId() {
        // Arrange
        User regularUser = User.builder().id(5L).username("player1").role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(regularUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of());
        when(characterSheetRepository.findActiveWithFilters(
                eq(5L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act - pass ownerId=99 but it should be overridden to 5
        characterSheetService.getAllCharacterSheets(0, 20, 99L, null, null, null, null, authentication);

        // Assert - repository is called with the user's own ID, not the requested one
        verify(characterSheetRepository).findActiveWithFilters(eq(5L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_AsRegularUser_WithNullOwnerId_ForcesToOwnId() {
        // Arrange
        User regularUser = User.builder().id(5L).username("player1").role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(regularUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of());
        when(characterSheetRepository.findActiveWithFilters(
                eq(5L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act - pass null ownerId, should be forced to 5
        characterSheetService.getAllCharacterSheets(0, 20, null, null, null, null, null, authentication);

        // Assert
        verify(characterSheetRepository).findActiveWithFilters(eq(5L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_AsPrivilegedUser_CanUseNullOwnerId() {
        // Arrange
        User admin = User.builder().id(10L).username("admin").role(Role.ADMIN).build();
        CustomUserDetails userDetails = new CustomUserDetails(admin);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.isPrivilegedRole(Role.ADMIN)).thenReturn(true);

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of());
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act - null ownerId should stay null for privileged users
        characterSheetService.getAllCharacterSheets(0, 20, null, null, null, null, null, authentication);

        // Assert
        verify(characterSheetRepository).findActiveWithFilters(eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    // ==================== GET CHARACTER SHEET BY ID TESTS ====================

    @Test
    void getCharacterSheetById_WithValidId_ReturnsSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .pronouns("he/him")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(2)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Aragorn");
        assertThat(result.getLevel()).isEqualTo(5);
        assertThat(result.getEvasion()).isEqualTo(10);
    }

    @Test
    void getCharacterSheetById_WithInvalidId_ThrowsEntityNotFoundException() {
        // Arrange
        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.getCharacterSheetById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 999");
    }

    @Test
    void getCharacterSheetById_WithExpansion_IncludesOwnerAndExperiences() {
        // Arrange
        User owner = User.builder()
                .id(1L)
                .username("player1")
                .email("player1@example.com")
                .build();
        User creator = User.builder()
                .id(2L)
                .username("gm1")
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .description("Survived dragon attack")
                .modifier(2)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>(List.of(exp)))
                .createdAt(LocalDateTime.now())
                .build();

        exp.setCharacterSheet(sheet);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(userService.mapToUserResponse(eq(owner), any()))
                .thenReturn(UserResponse.builder().id(1L).username("player1").build());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "owner,experiences");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOwner()).isNotNull();
        assertThat(result.getOwner().getUsername()).isEqualTo("player1");
        assertThat(result.getExperiences()).isNotNull();
        assertThat(result.getExperiences()).hasSize(1);
        assertThat(result.getExperiences().get(0).getDescription()).isEqualTo("Survived dragon attack");
    }

    @Test
    void getCharacterSheetById_ExpandOwner_RoutesThroughUserServiceRedaction() {
        // Arrange -- CharacterSheetService must never build the owner UserResponse itself; it has
        // to delegate to UserService.mapToUserResponse so the email/avatarUrl/timezone redaction
        // GET /api/users/{id} applies is reused here too (see UserServiceTest for the redaction
        // rules themselves).
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User viewer = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(viewer);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        UserResponse redacted = UserResponse.builder().id(1L).username("player1").build();
        when(userService.mapToUserResponse(owner, authentication)).thenReturn(redacted);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "owner", authentication);

        // Assert
        assertThat(result.getOwner()).isSameAs(redacted);
        verify(userService).mapToUserResponse(owner, authentication);
    }

    // ==================== NOTES VISIBILITY ON toResponse TESTS ====================
    // The `notes` field on CharacterSheetResponse is a private field, not an expansion: it must
    // be gated the same way the dedicated getNotes()/updateNotes() endpoints are, or leaking it
    // through the general sheet response would make the getNotes() access check pointless.

    private CharacterSheet sheetWithNotes(User owner, String notes) {
        return CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .notes(notes)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();
    }

    @Test
    void toResponse_NoAuthentication_OmitsNotes() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = sheetWithNotes(owner, "Secret notes");

        CharacterSheetResponse result = characterSheetService.toResponse(sheet, Set.of());

        assertThat(result.getNotes()).isNull();
    }

    @Test
    void toResponse_AsOwner_IncludesNotes() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = sheetWithNotes(owner, "Secret notes");

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        CharacterSheetResponse result = characterSheetService.toResponse(sheet, Set.of(), authentication);

        assertThat(result.getNotes()).isEqualTo("Secret notes");
    }

    @Test
    void toResponse_AsModerator_IncludesNotes() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User moderator = User.builder().id(2L).username("mod1").role(Role.MODERATOR).build();
        CharacterSheet sheet = sheetWithNotes(owner, "Secret notes");

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(true);

        CharacterSheetResponse result = characterSheetService.toResponse(sheet, Set.of(), authentication);

        assertThat(result.getNotes()).isEqualTo("Secret notes");
    }

    @Test
    void toResponse_AsOtherNonPrivilegedUser_OmitsNotes() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User otherUser = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = sheetWithNotes(owner, "Secret notes");

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        CharacterSheetResponse result = characterSheetService.toResponse(sheet, Set.of(), authentication);

        assertThat(result.getNotes()).isNull();
    }

    // ==================== CREATE CHARACTER SHEET TESTS ====================

    @Test
    void createCharacterSheet_WithValidData_CreatesSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .pronouns("he/him")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(2)
                .agilityMarked(false)
                .strengthModifier(3)
                .strengthMarked(false)
                .finesseModifier(1)
                .finesseMarked(false)
                .instinctModifier(2)
                .instinctMarked(false)
                .presenceModifier(2)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        CharacterSheet savedSheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .pronouns("he/him")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenReturn(savedSheet);

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Aragorn");
        assertThat(result.getLevel()).isEqualTo(5);
        verify(characterSheetRepository).save(any(CharacterSheet.class));
    }

    @Test
    void createCharacterSheet_WithArmorMarkedExceedsMax_PreservesMarked() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(10) // Exceeds max
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getArmorMarked()).isEqualTo(10);
    }

    @Test
    void createCharacterSheet_WithHitPointMarkedExceedsMax_PreservesMarked() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(15) // Exceeds max
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getHitPointMarked()).isEqualTo(15);
    }

    @Test
    void createCharacterSheet_WithStressMarkedExceedsMax_PreservesMarked() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(10) // Exceeds max
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getStressMarked()).isEqualTo(10);
    }

    @Test
    void createCharacterSheet_WithHopeMarkedExceedsMax_PreservesMarked() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(5) // Exceeds max
                .gold(50)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getHopeMarked()).isEqualTo(5);
    }

    @Test
    void createCharacterSheet_WithSevereThresholdLessThanMajor_ThrowsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(6)
                .severeDamageThreshold(3) // Less than major
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Severe damage threshold")
                .hasMessageContaining("must be greater than or equal to major damage threshold");
    }

    // ==================== DELETE CHARACTER SHEET TESTS ====================

    @Test
    void deleteCharacterSheet_AsOwner_SoftDeletesSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(campaignRepository.findActiveByCampaignCharacterSheetId(1L)).thenReturn(List.of());

        // Act
        characterSheetService.deleteCharacterSheet(1L, authentication);

        // Assert
        assertThat(sheet.isDeleted()).isTrue();
        verify(characterSheetRepository).save(sheet);
    }

    @Test
    void deleteCharacterSheet_AsModerator_SoftDeletesSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User moderator = User.builder().id(2L).username("moderator1").role(Role.MODERATOR).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);
        when(campaignRepository.findActiveByCampaignCharacterSheetId(1L)).thenReturn(List.of());

        // Act
        characterSheetService.deleteCharacterSheet(1L, authentication);

        // Assert
        assertThat(sheet.isDeleted()).isTrue();
        verify(characterSheetRepository).save(sheet);
    }

    @Test
    void deleteCharacterSheet_InCampaigns_RemovesFromCampaignsBeforeSoftDelete() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Campaign campaign1 = Campaign.builder().id(10L).name("Campaign A").build();
        campaign1.getPlayerCharacters().add(sheet);
        Campaign campaign2 = Campaign.builder().id(11L).name("Campaign B").build();
        campaign2.getNonPlayerCharacters().add(sheet);

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(campaignRepository.findActiveByCampaignCharacterSheetId(1L)).thenReturn(List.of(campaign1, campaign2));

        // Act
        characterSheetService.deleteCharacterSheet(1L, authentication);

        // Assert
        assertThat(campaign1.getPlayerCharacters()).doesNotContain(sheet);
        assertThat(campaign2.getNonPlayerCharacters()).doesNotContain(sheet);
        verify(campaignRepository).saveAll(List.of(campaign1, campaign2));
        assertThat(sheet.isDeleted()).isTrue();
        verify(characterSheetRepository).save(sheet);
    }

    @Test
    void deleteCharacterSheet_AsOtherUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User otherUser = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.deleteCharacterSheet(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to delete this character sheet");

        verify(characterSheetRepository, never()).save(any(CharacterSheet.class));
    }

    @Test
    void deleteCharacterSheet_WithInvalidId_ThrowsEntityNotFoundException() {
        // Arrange
        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.deleteCharacterSheet(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 999");
    }

    // ==================== SECTION 2: EQUIPMENT AND CARDS TESTS ====================

    @Test
    void createCharacterSheet_WithEquipment_SetsEquipment() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon primaryWeapon = Weapon.builder().id(1L).name("Longsword").build();
        Weapon secondaryWeapon = Weapon.builder().id(2L).name("Dagger").build();
        Armor armor = Armor.builder().id(1L).name("Plate Mail").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("PRIMARY").build(),
                        InventoryWeaponRequest.builder().weaponId(2L).equipped(true).slot("SECONDARY").build()
                ))
                .inventoryArmors(List.of(
                        InventoryArmorRequest.builder().armorId(1L).equipped(true).build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(primaryWeapon));
        when(weaponRepository.findById(2L)).thenReturn(Optional.of(secondaryWeapon));
        when(armorRepository.findById(1L)).thenReturn(Optional.of(armor));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInventoryWeapons()).hasSize(2);
        assertThat(result.getInventoryArmors()).hasSize(1);
        assertThat(result.getInventoryArmors().get(0).getEquipped()).isTrue();
    }

    @Test
    void createCharacterSheet_WithCards_SetsCards() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CommunityCard communityCard = CommunityCard.builder().id(1L).name("Nomad").build();
        AncestryCard ancestryCard = AncestryCard.builder().id(1L).name("Human").build();
        SubclassCard subclassCard = SubclassCard.builder().id(1L).name("Guardian").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .communityCardIds(List.of(1L))
                .ancestryCardIds(List.of(1L))
                .subclassCardIds(List.of(1L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(communityCardRepository.findById(1L)).thenReturn(Optional.of(communityCard));
        when(ancestryCardRepository.findById(1L)).thenReturn(Optional.of(ancestryCard));
        when(subclassCardRepository.findById(1L)).thenReturn(Optional.of(subclassCard));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCommunityCardIds()).contains(1L);
        assertThat(result.getAncestryCardIds()).contains(1L);
        assertThat(result.getSubclassCardIds()).contains(1L);
    }

    @Test
    void createCharacterSheet_WithInventory_SetsInventory() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon = Weapon.builder().id(1L).name("Spare Sword").build();
        Armor armor = Armor.builder().id(1L).name("Leather Armor").build();
        Loot loot = Loot.builder().id(1L).name("Healing Potion").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .inventoryWeapons(List.of(InventoryWeaponRequest.builder().weaponId(1L).build()))
                .inventoryArmors(List.of(InventoryArmorRequest.builder().armorId(1L).build()))
                .inventoryItems(List.of(InventoryLootRequest.builder().lootId(1L).build()))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));
        when(armorRepository.findById(1L)).thenReturn(Optional.of(armor));
        when(lootRepository.findById(1L)).thenReturn(Optional.of(loot));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInventoryWeapons()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getWeaponId()).isEqualTo(1L);
        assertThat(result.getInventoryArmors()).hasSize(1);
        assertThat(result.getInventoryArmors().get(0).getArmorId()).isEqualTo(1L);
        assertThat(result.getInventoryItems()).hasSize(1);
        assertThat(result.getInventoryItems().get(0).getLootId()).isEqualTo(1L);
    }

    @Test
    void createCharacterSheet_WithInvalidWeaponId_ThrowsEntityNotFoundException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .inventoryWeapons(List.of(InventoryWeaponRequest.builder().weaponId(999L).build()))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Weapon not found with id: 999");
    }

    @Test
    void createCharacterSheet_WithInvalidCardId_ThrowsEntityNotFoundException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .communityCardIds(List.of(999L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(communityCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CommunityCard not found with id: 999");
    }

    @Test
    void createCharacterSheet_WithDomainCards_SetsDomainCards() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Domain domain = Domain.builder().id(1L).name("Blade").build();
        DomainCard domainCard = DomainCard.builder().id(1L).name("Blade Strike").associatedDomain(domain).level(1).recallCost(0).build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .equippedDomainCardIds(List.of(1L))
                .vaultDomainCardIds(List.of())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(domainCardRepository.findById(1L)).thenReturn(Optional.of(domainCard));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomainCardIds()).contains(1L);
        assertThat(result.getEquippedDomainCardIds()).contains(1L);
        assertThat(result.getVaultDomainCardIds()).isEmpty();
    }

    @Test
    void createCharacterSheet_WithInvalidDomainCardId_ThrowsEntityNotFoundException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .equippedDomainCardIds(List.of(999L))
                .vaultDomainCardIds(List.of())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(domainCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("DomainCard not found with id: 999");
    }

    @Test
    void createCharacterSheet_WithDuplicateDomainCardIds_ThrowsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .equippedDomainCardIds(List.of(1L, 1L))
                .vaultDomainCardIds(List.of())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate domain card IDs");
    }

    // ==================== UPDATE CHARACTER SHEET TESTS ====================

    @Test
    void updateCharacterSheet_AsOwner_UpdatesSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .level(6)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Aragorn II");
        assertThat(result.getLevel()).isEqualTo(6);
        verify(characterSheetRepository).save(any(CharacterSheet.class));
    }

    @Test
    void updateCharacterSheet_AsModerator_UpdatesSheet() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User moderator = User.builder().id(2L).username("moderator1").role(Role.MODERATOR).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Aragorn II");
        verify(characterSheetRepository).save(any(CharacterSheet.class));
    }

    @Test
    void updateCharacterSheet_AsOtherUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User otherUser = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to update this character sheet");

        verify(characterSheetRepository, never()).save(any(CharacterSheet.class));
    }

    @Test
    void updateCharacterSheet_PartialUpdate_OnlyUpdatesProvidedFields() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .evasion(10)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Aragorn II");
        assertThat(result.getLevel()).isEqualTo(5); // Level should remain unchanged
        assertThat(result.getEvasion()).isEqualTo(10); // Evasion should remain unchanged
    }

    @Test
    void updateCharacterSheet_UpdatesBasicFields_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .pronouns("he/him")
                .level(5)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Strider")
                .pronouns("they/them")
                .level(6)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Strider");
        assertThat(result.getPronouns()).isEqualTo("they/them");
        assertThat(result.getLevel()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheet_UpdatesCombatFields_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .evasion(12)
                .armorMax(7)
                .armorMarked(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getEvasion()).isEqualTo(12);
        assertThat(result.getArmorMax()).isEqualTo(7);
        assertThat(result.getArmorMarked()).isEqualTo(2);
    }

    @Test
    void updateCharacterSheet_UpdatesTraitFields_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .agilityModifier(3)
                .agilityMarked(true)
                .strengthModifier(4)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getAgilityModifier()).isEqualTo(3);
        assertThat(result.getAgilityMarked()).isTrue();
        assertThat(result.getStrengthModifier()).isEqualTo(4);
        assertThat(result.getStrengthMarked()).isFalse(); // Should remain unchanged
    }

    @Test
    void updateCharacterSheet_UpdatesResourceFields_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .hitPointMax(12)
                .hitPointMarked(3)
                .gold(100)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getHitPointMax()).isEqualTo(12);
        assertThat(result.getHitPointMarked()).isEqualTo(3);
        assertThat(result.getGold()).isEqualTo(100);
    }

    // ==================== HOPE & FEAR RESOURCE TESTS ====================

    private CharacterSheet buildHfSheet(User owner) {
        return CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(1)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(50)
                .focusMax(6)
                .focusMarked(0)
                .favor(0)
                // Transformations are GM-granted; enable them so these tests exercise the
                // transformation behaviour rather than the access gate.
                .transformationEnabled(true)
                .knownMartialStances(new HashSet<>())
                .build();
    }

    private void stubSaveWithEmptyCollections(CharacterSheetRepository repo) {
        when(repo.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            if (saved.getCommunityCards() == null) saved.setCommunityCards(new HashSet<>());
            if (saved.getAncestryCards() == null) saved.setAncestryCards(new HashSet<>());
            if (saved.getSubclassCards() == null) saved.setSubclassCards(new HashSet<>());
            if (saved.getCharacterSheetDomainCards() == null) saved.setCharacterSheetDomainCards(new HashSet<>());
            if (saved.getCharacterSheetWeapons() == null) saved.setCharacterSheetWeapons(new HashSet<>());
            if (saved.getCharacterSheetArmors() == null) saved.setCharacterSheetArmors(new HashSet<>());
            if (saved.getCharacterSheetLoot() == null) saved.setCharacterSheetLoot(new HashSet<>());
            if (saved.getExperiences() == null) saved.setExperiences(new HashSet<>());
            if (saved.getKnownMartialStances() == null) saved.setKnownMartialStances(new HashSet<>());
            return saved;
        });
    }

    @Test
    void updateCharacterSheet_FocusMarked_ClampsToZeroWhenNegativeIsRejectedByRange() {
        // The DTO's @PositiveOrZero prevents negative input at the controller layer; the service
        // still clamps defensively so a marked value can never end up below zero.
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setFocusMarked(4);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .focusMarked(0)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getFocusMarked()).isZero();
    }

    @Test
    void updateCharacterSheet_FocusMarked_ClampedToFocusMaxWhenExceedingIt() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setFocusMax(6);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .focusMarked(99)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getFocusMarked()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheet_FocusMarked_AtBoundarySix_IsAccepted() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .focusMarked(6)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getFocusMarked()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheet_LoweringFocusMax_ClampsFocusMarkedDown() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setFocusMarked(6);
        sheet.setFocusMax(6);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .focusMax(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getFocusMax()).isEqualTo(2);
        assertThat(result.getFocusMarked()).isEqualTo(2);
    }

    @Test
    void updateCharacterSheet_TransformationTokens_ClampedToSixMaximum() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationTokens(99)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getTransformationTokens()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheet_TransformationTokens_ZeroIsAccepted() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationTokens(0)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getTransformationTokens()).isZero();
    }

    @Test
    void updateCharacterSheet_TransformationTokens_NullLeavesFieldUnchanged() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationTokens(null);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .gold(10)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getTransformationTokens()).isNull();
    }

    @Test
    void updateCharacterSheet_AttachTransformationCard_SetsTransformationCardId() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        TransformationCard card = TransformationCard.builder().id(5L).name("Vampire").expansion(expansion).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationCardId(5L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(card));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getTransformationCardId()).isEqualTo(5L);
    }

    @Test
    void updateCharacterSheet_AttachTransformationCard_NotFound_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationCardId(999L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateCharacterSheet_ClearTransformationCard_DetachesCardTokensAndWolfForm() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        TransformationCard card = TransformationCard.builder().id(5L).name("Werewolf").expansion(expansion).build();
        sheet.setTransformationCard(card);
        sheet.setTransformationTokens(3);
        sheet.setWolfFormActive(true);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .clearTransformationCard(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getTransformationCardId()).isNull();
        assertThat(result.getTransformationTokens()).isNull();
        assertThat(result.getWolfFormActive()).isFalse();
    }

    // ==================== TRANSFORMATION ACCESS GATE TESTS ====================

    @Test
    void updateCharacterSheet_TransformationEnabled_IsIncludedInResponse() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder().gold(10).build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.isTransformationEnabled()).isTrue();
    }

    @Test
    void updateCharacterSheet_AttachTransformationCard_WhenNotEnabled_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationEnabled(false);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationCardId(5L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Transformations are not enabled for this character. Ask your GM to enable them.");
    }

    @Test
    void updateCharacterSheet_ClearTransformationCard_WhenNotEnabled_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationEnabled(false);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .clearTransformationCard(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ask your GM to enable them");
    }

    @Test
    void updateCharacterSheet_TransformationTokens_WhenNotEnabled_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationEnabled(false);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .transformationTokens(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateCharacterSheet_WolfFormActive_WhenNotEnabled_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationEnabled(false);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .wolfFormActive(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateCharacterSheet_NonTransformationFields_WhenNotEnabled_Succeeds() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setTransformationEnabled(false);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .gold(75)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getGold()).isEqualTo(75);
        assertThat(result.isTransformationEnabled()).isFalse();
    }

    @Test
    void updateCharacterSheet_KnownMartialStances_ActiveStanceNotInKnownSet_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        MartialStance knownStance = MartialStance.builder().id(1L).name("Bear Stance").tier(1).expansion(expansion).isOfficial(true).build();
        MartialStance unknownStance = MartialStance.builder().id(2L).name("Tiger Stance").tier(1).expansion(expansion).isOfficial(true).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .knownMartialStanceIds(List.of(1L))
                .activeMartialStanceId(2L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(martialStanceRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(knownStance));
        when(martialStanceRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(unknownStance));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("known stances");
    }

    @Test
    void updateCharacterSheet_KnownMartialStances_ActiveStanceInKnownSet_Succeeds() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        MartialStance stance = MartialStance.builder().id(1L).name("Bear Stance").tier(1).expansion(expansion).isOfficial(true).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .knownMartialStanceIds(List.of(1L))
                .activeMartialStanceId(1L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(martialStanceRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(stance));
        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(stance));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getKnownMartialStanceIds()).containsExactly(1L);
        assertThat(result.getActiveMartialStanceId()).isEqualTo(1L);
    }

    @Test
    void updateCharacterSheet_KnownMartialStance_TierAboveCharacterTier_ThrowsException() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        sheet.setLevel(1); // tier 1
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        MartialStance tier2Stance = MartialStance.builder().id(1L).name("Iron Stance").tier(2).expansion(expansion).isOfficial(true).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .knownMartialStanceIds(List.of(1L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(martialStanceRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(tier2Stance));

        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tier");
    }

    @Test
    void updateCharacterSheet_ClearActiveMartialStance_ClearsActiveStance() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        MartialStance stance = MartialStance.builder().id(1L).name("Bear Stance").tier(1).expansion(expansion).isOfficial(true).build();
        sheet.getKnownMartialStances().add(stance);
        sheet.setActiveMartialStance(stance);

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .clearActiveMartialStance(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        stubSaveWithEmptyCollections(characterSheetRepository);

        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        assertThat(result.getActiveMartialStanceId()).isNull();
    }

    @Test
    void toResponse_ExpandTransformationCard_IncludesFullObject() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        TransformationCard card = TransformationCard.builder().id(5L).name("Ghost").expansion(expansion).build();
        sheet.setTransformationCard(card);

        TransformationCardResponse cardResponse = TransformationCardResponse.builder().id(5L).name("Ghost").build();
        when(transformationCardService.toResponse(eq(card), eq(Set.of()))).thenReturn(cardResponse);

        CharacterSheetResponse response = characterSheetService.toResponse(sheet, Set.of("transformationCard"));

        assertThat(response.getTransformationCardId()).isEqualTo(5L);
        assertThat(response.getTransformationCard()).isNotNull();
        assertThat(response.getTransformationCard().getName()).isEqualTo("Ghost");
    }

    @Test
    void toResponse_WithoutExpand_TransformationCardIdOnlyNoFullObject() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        TransformationCard card = TransformationCard.builder().id(5L).name("Ghost").expansion(expansion).build();
        sheet.setTransformationCard(card);

        CharacterSheetResponse response = characterSheetService.toResponse(sheet, Set.of());

        assertThat(response.getTransformationCardId()).isEqualTo(5L);
        assertThat(response.getTransformationCard()).isNull();
    }

    @Test
    void toResponse_ExpandKnownMartialStances_IncludesFullObjects() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);
        Expansion expansion = Expansion.builder().id(2L).name("Hope & Fear").build();
        MartialStance stance = MartialStance.builder().id(1L).name("Bear Stance").tier(1).expansion(expansion).isOfficial(true).build();
        sheet.getKnownMartialStances().add(stance);

        MartialStanceResponse stanceResponse = MartialStanceResponse.builder().id(1L).name("Bear Stance").build();
        when(martialStanceService.toResponse(eq(stance), eq(Set.of()))).thenReturn(stanceResponse);

        CharacterSheetResponse response = characterSheetService.toResponse(sheet, Set.of("knownMartialStances"));

        assertThat(response.getKnownMartialStanceIds()).containsExactly(1L);
        assertThat(response.getKnownMartialStances()).hasSize(1);
    }

    @Test
    void toResponse_NoTransformationOrStances_NullFieldsAndEmptyIds() {
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildHfSheet(owner);

        CharacterSheetResponse response = characterSheetService.toResponse(sheet, Set.of());

        assertThat(response.getTransformationCardId()).isNull();
        assertThat(response.getActiveMartialStanceId()).isNull();
        assertThat(response.getKnownMartialStanceIds()).isEmpty();
        assertThat(response.getFocusMarked()).isZero();
        assertThat(response.getFocusMax()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheet_UpdatesEquipment_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        Weapon weapon = Weapon.builder().id(1L).name("Longsword").build();
        Armor armor = Armor.builder().id(1L).name("Plate Mail").build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("PRIMARY").build()
                ))
                .inventoryArmors(List.of(
                        InventoryArmorRequest.builder().armorId(1L).equipped(true).build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));
        when(armorRepository.findById(1L)).thenReturn(Optional.of(armor));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getInventoryWeapons()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getEquipped()).isTrue();
        assertThat(result.getInventoryWeapons().get(0).getSlot()).isEqualTo("PRIMARY");
        assertThat(result.getInventoryArmors()).hasSize(1);
        assertThat(result.getInventoryArmors().get(0).getEquipped()).isTrue();
    }

    @Test
    void updateCharacterSheet_UpdatesCards_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .communityCards(new HashSet<>())
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        CommunityCard communityCard = CommunityCard.builder().id(1L).name("Nomad").build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .communityCardIds(List.of(1L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(communityCardRepository.findById(1L)).thenReturn(Optional.of(communityCard));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getCommunityCardIds()).contains(1L);
    }

    @Test
    void updateCharacterSheet_UpdatesInventory_Success() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .characterSheetWeapons(new HashSet<>())
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        Weapon weapon = Weapon.builder().id(1L).name("Spare Sword").build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .inventoryWeapons(List.of(InventoryWeaponRequest.builder().weaponId(1L).build()))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getInventoryWeapons()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getWeaponId()).isEqualTo(1L);
    }

    @Test
    void updateCharacterSheet_WithArmorMarkedExceedsMaxWithoutChangingMax_PreservesMarked() {
        // Equipped items/features can raise the effective max above the stored base max.
        // When the update does NOT touch armorMax, the marked value must be preserved
        // as-is even if it exceeds the stored base max.
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .armorMarked(10) // Exceeds armorMax but max is not being changed
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getArmorMarked()).isEqualTo(10);
    }

    @Test
    void updateCharacterSheet_WhenReducingMaxBelowExistingMarked_ClampsMarkedToNewMax() {
        // When the user explicitly lowers a *_max below the current marked value,
        // the marked value should be clamped down to the new max.
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .evasion(10)
                .armorMax(10)
                .armorMarked(8)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .armorMax(5) // Reduces max below current armorMarked=8
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getArmorMax()).isEqualTo(5);
        assertThat(captor.getValue().getArmorMarked()).isEqualTo(5);
    }

    @Test
    void getCharacterSheetById_WithFullExpansion_IncludesAllRelationships() {
        // Arrange
        User owner = User.builder()
                .id(1L)
                .username("player1")
                .email("player1@example.com")
                .build();
        User creator = User.builder()
                .id(2L)
                .username("gm1")
                .build();

        Weapon primaryWeapon = Weapon.builder().id(1L).name("Longsword").build();
        Armor armor = Armor.builder().id(1L).name("Plate Mail").build();
        CommunityCard communityCard = CommunityCard.builder().id(1L).name("Nomad").build();

        Experience exp = Experience.builder()
                .id(1L)
                .description("Survived dragon attack")
                .modifier(2)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>(List.of(communityCard)))
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>(List.of(exp)))
                .createdAt(LocalDateTime.now())
                .build();

        // Add linking entities after sheet is built so we can set the back-reference
        CharacterSheetWeapon csw = CharacterSheetWeapon.builder()
                .id(10L).characterSheet(sheet).weapon(primaryWeapon).equipped(true).slot("PRIMARY").build();
        sheet.getCharacterSheetWeapons().add(csw);
        CharacterSheetArmor csa = CharacterSheetArmor.builder()
                .id(11L).characterSheet(sheet).armor(armor).equipped(true).build();
        sheet.getCharacterSheetArmors().add(csa);

        exp.setCharacterSheet(sheet);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(weaponService.toResponse(any(Weapon.class), anySet()))
                .thenReturn(WeaponResponse.builder().id(1L).name("Longsword").build());
        when(armorService.toResponse(any(Armor.class), anySet()))
                .thenReturn(ArmorResponse.builder().id(1L).name("Plate Mail").build());
        when(communityCardService.toResponse(any(CommunityCard.class), anySet()))
                .thenReturn(CommunityCardResponse.builder().id(1L).name("Nomad").build());
        when(userService.mapToUserResponse(eq(owner), any()))
                .thenReturn(UserResponse.builder().id(1L).username("player1").build());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(
                1L, "owner,experiences,inventoryWeapons,inventoryArmors,communityCards");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOwner()).isNotNull();
        assertThat(result.getOwner().getUsername()).isEqualTo("player1");
        assertThat(result.getExperiences()).hasSize(1);
        assertThat(result.getExperiences().get(0).getDescription()).isEqualTo("Survived dragon attack");
        assertThat(result.getInventoryWeapons()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getWeapon()).isNotNull();
        assertThat(result.getInventoryWeapons().get(0).getWeapon().getName()).isEqualTo("Longsword");
        assertThat(result.getInventoryArmors()).hasSize(1);
        assertThat(result.getInventoryArmors().get(0).getArmor()).isNotNull();
        assertThat(result.getInventoryArmors().get(0).getArmor().getName()).isEqualTo("Plate Mail");
        assertThat(result.getCommunityCards()).hasSize(1);
        assertThat(result.getCommunityCards().get(0).getName()).isEqualTo("Nomad");
    }

    // ==================== DOMAIN CARD TESTS ====================

    @Test
    void updateCharacterSheet_WithDomainCards_UpdatesDomainCards() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Domain domain = Domain.builder().id(1L).name("Blade").build();
        DomainCard domainCard = DomainCard.builder().id(1L).name("Blade Strike").associatedDomain(domain).level(1).recallCost(0).build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .equippedDomainCardIds(List.of(1L))
                .vaultDomainCardIds(List.of())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(domainCardRepository.findById(1L)).thenReturn(Optional.of(domainCard));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomainCardIds()).contains(1L);
        verify(domainCardRepository).findById(1L);
    }

    @Test
    void updateCharacterSheet_WithDuplicateDomainCardInEquippedList_ThrowsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(5).owner(owner)
                .evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .hitPointMax(10).hitPointMarked(0).stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .equippedDomainCardIds(List.of(1L, 1L))
                .vaultDomainCardIds(List.of())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate domain card IDs");
    }

    @Test
    void updateCharacterSheet_WithSameCardInEquippedAndVault_ThrowsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(5).owner(owner)
                .evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .hitPointMax(10).hitPointMarked(0).stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .equippedDomainCardIds(List.of(1L))
                .vaultDomainCardIds(List.of(1L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate domain card IDs");
    }

    @Test
    void updateCharacterSheet_WithOnlyEquippedDomainCardIds_ThrowsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(5).owner(owner)
                .evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .hitPointMax(10).hitPointMarked(0).stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).build();

        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .equippedDomainCardIds(List.of(1L))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both equippedDomainCardIds and vaultDomainCardIds must be provided together");
    }

    @Test
    void getCharacterSheetById_WithDomainCardsExpansion_IncludesDomainCards() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Domain domain = Domain.builder().id(1L).name("Blade").build();
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").build();
        DomainCard domainCard = DomainCard.builder()
                .id(1L)
                .name("Blade Strike")
                .expansion(expansion)
                .associatedDomain(domain)
                .level(1)
                .recallCost(0)
                .createdAt(LocalDateTime.now())
                .build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                .id(1L)
                .characterSheet(sheet)
                .domainCard(domainCard)
                .equipped(true)
                .build();
        sheet.setCharacterSheetDomainCards(new HashSet<>(List.of(csdc)));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(domainCardService.toResponse(any(DomainCard.class), anySet()))
                .thenReturn(DomainCardResponse.builder()
                        .id(1L)
                        .name("Blade Strike")
                        .associatedDomainId(1L)
                        .build());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "domainCards");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomainCardIds()).contains(1L);
        assertThat(result.getDomainCards()).isNotNull();
        assertThat(result.getDomainCards()).hasSize(1);
        assertThat(result.getDomainCards().get(0).getName()).isEqualTo("Blade Strike");
        assertThat(result.getDomainCards().get(0).getAssociatedDomainId()).isEqualTo(1L);
    }

    @Test
    void getCharacterSheetById_WithFeaturesExpand_ExpandsNestedFeaturesOnWeapon() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon primaryWeapon = Weapon.builder().id(1L).name("Longsword").build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        CharacterSheetWeapon csw = CharacterSheetWeapon.builder()
                .id(10L).characterSheet(sheet).weapon(primaryWeapon).equipped(true).slot("PRIMARY").build();
        sheet.getCharacterSheetWeapons().add(csw);

        List<FeatureResponse> features = List.of(
                FeatureResponse.builder().id(10L).name("Parry").build()
        );
        WeaponResponse weaponResponseWithFeatures = WeaponResponse.builder()
                .id(1L)
                .name("Longsword")
                .features(features)
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(weaponService.toResponse(any(Weapon.class), anySet())).thenReturn(weaponResponseWithFeatures);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(
                1L, "inventoryWeapons,features");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInventoryWeapons()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getWeapon()).isNotNull();
        assertThat(result.getInventoryWeapons().get(0).getWeapon().getFeatures()).isNotNull();
        assertThat(result.getInventoryWeapons().get(0).getWeapon().getFeatures()).hasSize(1);
        assertThat(result.getInventoryWeapons().get(0).getWeapon().getFeatures().get(0).getName()).isEqualTo("Parry");
    }

    // ==================== WEAPON SLOT VALIDATION TESTS ====================

    @Test
    void createCharacterSheet_WithTwoPrimaryWeapons_ThrowsIllegalStateException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon1 = Weapon.builder().id(1L).name("Sword1").build();
        Weapon weapon2 = Weapon.builder().id(2L).name("Sword2").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("PRIMARY").build(),
                        InventoryWeaponRequest.builder().weaponId(2L).equipped(true).slot("PRIMARY").build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon1));
        when(weaponRepository.findById(2L)).thenReturn(Optional.of(weapon2));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only one PRIMARY weapon slot is allowed");
    }

    @Test
    void createCharacterSheet_WithTwoSecondaryWeapons_ThrowsIllegalStateException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon1 = Weapon.builder().id(1L).name("Dagger1").build();
        Weapon weapon2 = Weapon.builder().id(2L).name("Dagger2").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("SECONDARY").build(),
                        InventoryWeaponRequest.builder().weaponId(2L).equipped(true).slot("SECONDARY").build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon1));
        when(weaponRepository.findById(2L)).thenReturn(Optional.of(weapon2));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only one SECONDARY weapon slot is allowed");
    }

    @Test
    void createCharacterSheet_WithEquippedWeaponWithoutSlot_ThrowsIllegalStateException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Equipped weapons must have a slot");
    }

    @Test
    void createCharacterSheet_WithUnequippedWeaponWithSlot_ThrowsIllegalStateException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(false).slot("PRIMARY").build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unequipped weapons must not have a slot");
    }

    @Test
    void createCharacterSheet_WithInvalidSlot_ThrowsIllegalStateException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("TERTIARY").build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Weapon slot must be PRIMARY or SECONDARY");
    }

    @Test
    void createCharacterSheet_WithDuplicateWeaponIds_Succeeds() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();

        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .name("Aragorn").level(5).evasion(10).armorMax(5).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0).gold(50)
                .inventoryWeapons(List.of(
                        InventoryWeaponRequest.builder().weaponId(1L).equipped(true).slot("PRIMARY").build(),
                        InventoryWeaponRequest.builder().weaponId(1L).build()
                ))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(weaponRepository.findById(1L)).thenReturn(Optional.of(weapon));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInventoryWeapons()).hasSize(2);
    }

    // ==================== CLASS INFO TESTS ====================

    @Test
    void getCharacterSheet_WithSubclassCards_PopulatesClassIdAndName() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();

        Class characterClass = Class.builder().id(10L).name("Warrior").build();
        SubclassPath path = SubclassPath.builder().id(5L).name("Stalwart").associatedClass(characterClass).build();
        SubclassCard subclassCard = SubclassCard.builder().id(1L).name("Guardian").subclassPath(path).build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(1).proficiency(0)
                .evasion(10).armorMax(0).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(2).hopeMarked(0).gold(0)
                .owner(owner)
                .subclassCards(new HashSet<>(List.of(subclassCard)))
                .communityCards(new HashSet<>()).ancestryCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassId()).isEqualTo(10L);
        assertThat(result.getClassName()).isEqualTo("Warrior");
        assertThat(result.getClassObject()).isNull();
    }

    @Test
    void getCharacterSheet_WithNoSubclassCards_ClassInfoIsNull() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(1).proficiency(0)
                .evasion(10).armorMax(0).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(2).hopeMarked(0).gold(0)
                .owner(owner)
                .subclassCards(new HashSet<>())
                .communityCards(new HashSet<>()).ancestryCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassId()).isNull();
        assertThat(result.getClassName()).isNull();
        assertThat(result.getClassObject()).isNull();
    }

    @Test
    void getCharacterSheet_WithExpandClass_PopulatesClassObject() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();

        Class characterClass = Class.builder().id(10L).name("Warrior").build();
        SubclassPath path = SubclassPath.builder().id(5L).name("Stalwart").associatedClass(characterClass).build();
        SubclassCard subclassCard = SubclassCard.builder().id(1L).name("Guardian").subclassPath(path).build();

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(1).proficiency(0)
                .evasion(10).armorMax(0).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(2).hopeMarked(0).gold(0)
                .owner(owner)
                .subclassCards(new HashSet<>(List.of(subclassCard)))
                .communityCards(new HashSet<>()).ancestryCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        ClassResponse classResponse = ClassResponse.builder().id(10L).name("Warrior").build();
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(classService.toResponse(eq(characterClass), anySet())).thenReturn(classResponse);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "class");

        // Assert
        assertThat(result.getClassId()).isEqualTo(10L);
        assertThat(result.getClassName()).isEqualTo("Warrior");
        assertThat(result.getClassObject()).isNotNull();
        assertThat(result.getClassObject().getId()).isEqualTo(10L);
        assertThat(result.getClassObject().getName()).isEqualTo("Warrior");
    }

    @Test
    void getCharacterSheet_SubclassCardWithNullSubclassPath_ClassInfoIsNull() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();

        SubclassCard subclassCard = SubclassCard.builder().id(1L).name("Guardian").build(); // no subclassPath

        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L).name("Aragorn").level(1).proficiency(0)
                .evasion(10).armorMax(0).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(2).hopeMarked(0).gold(0)
                .owner(owner)
                .subclassCards(new HashSet<>(List.of(subclassCard)))
                .communityCards(new HashSet<>()).ancestryCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassId()).isNull();
        assertThat(result.getClassName()).isNull();
    }

    // ==================== MULTICLASS CLASS INFO TESTS ====================

    /**
     * Builds a minimal character sheet carrying the supplied subclass cards.
     * Every collection must be initialised or toResponse throws a NullPointerException.
     */
    private CharacterSheet buildSheetWithSubclassCards(User owner, SubclassCard... subclassCards) {
        return CharacterSheet.builder()
                .id(1L).name("Aragorn").level(1).proficiency(0)
                .evasion(10).armorMax(0).armorMarked(0)
                .majorDamageThreshold(3).severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(2).hopeMarked(0).gold(0)
                .owner(owner)
                .subclassCards(new HashSet<>(List.of(subclassCards)))
                .communityCards(new HashSet<>()).ancestryCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();
    }

    /**
     * Builds a subclass card whose path is associated with the supplied class.
     */
    private SubclassCard buildSubclassCard(Long cardId, String cardName, Long pathId, String pathName, Class associatedClass) {
        SubclassPath path = SubclassPath.builder().id(pathId).name(pathName).associatedClass(associatedClass).build();
        return SubclassCard.builder().id(cardId).name(cardName).subclassPath(path).build();
    }

    @Test
    void getCharacterSheet_WithNoSubclassCards_ClassPluralsAreEmpty() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "class");

        // Assert
        assertThat(result.getClassIds()).isEmpty();
        assertThat(result.getClassNames()).isEmpty();
        assertThat(result.getClasses()).isNull();
        assertThat(result.getClassId()).isNull();
        assertThat(result.getClassName()).isNull();
        assertThat(result.getClassObject()).isNull();
    }

    @Test
    void getCharacterSheet_SingleClass_PluralAndSingularFieldsAgree() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class warrior = Class.builder().id(10L).name("Warrior").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "Call of the Brave", 5L, "Call of the Brave", warrior));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassIds()).containsExactly(10L);
        assertThat(result.getClassNames()).containsExactly("Warrior");
        assertThat(result.getClassId()).isEqualTo(result.getClassIds().get(0));
        assertThat(result.getClassName()).isEqualTo(result.getClassNames().get(0));
    }

    @Test
    void getCharacterSheet_SingleClassWithTwoCardsFromSamePath_DeduplicatesClass() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        SubclassPath wardenOfRenewal = SubclassPath.builder().id(6L).name("Warden of Renewal").associatedClass(druid).build();
        SubclassCard foundation = SubclassCard.builder().id(1L).name("Foundation").subclassPath(wardenOfRenewal).build();
        SubclassCard specialization = SubclassCard.builder().id(2L).name("Specialization").subclassPath(wardenOfRenewal).build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner, foundation, specialization);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassIds()).containsExactly(20L);
        assertThat(result.getClassNames()).containsExactly("Druid");
    }

    /**
     * Builds an advancement log entry whose blob multiclasses into the supplied subclass cards.
     */
    private CharacterAdvancementLog buildMulticlassLog(Long logId, int toLevel, Long... subclassCardIds) throws Exception {
        List<Map<String, Object>> advancements = new ArrayList<>();
        advancements.add(Map.of("type", "GAIN_HP"));
        for (Long subclassCardId : subclassCardIds) {
            advancements.add(Map.of("type", "MULTICLASS", "subclassCardId", subclassCardId));
        }
        String advancementData = objectMapper.writeValueAsString(Map.of("advancements", advancements));
        return CharacterAdvancementLog.builder()
                .id(logId).fromLevel(toLevel - 1).toLevel(toLevel).tier(2).advancementData(advancementData).build();
    }

    @Test
    void getCharacterSheet_MulticlassWithoutAdvancementLogs_FallsBackToClassIdAscending() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        // Legacy characters predate level-up logging, so acquisition order cannot be recovered
        when(characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(1L)).thenReturn(List.of());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassIds()).containsExactly(20L, 30L);
        assertThat(result.getClassNames()).containsExactly("Druid", "Wizard");
        assertThat(result.getClassId()).isEqualTo(20L);
        assertThat(result.getClassName()).isEqualTo("Druid");
    }

    @Test
    void getCharacterSheet_Multiclass_ReturnsOriginalClassFirstThenMulticlassesInLogOrder() throws Exception {
        // Arrange - a Wizard who multiclassed into Warrior first and Druid second, so acquisition
        // order (Wizard, Warrior, Druid) is deliberately unrelated to class ID order (20, 30, 40)
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        Class warrior = Class.builder().id(40L).name("Warrior").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Call of the Brave", 8L, "Call of the Brave", warrior),
                buildSubclassCard(3L, "Warden of Renewal", 6L, "Warden of Renewal", druid));
        List<CharacterAdvancementLog> logs = List.of(buildMulticlassLog(1L, 5, 2L), buildMulticlassLog(2L, 8, 3L));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(1L)).thenReturn(logs);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassNames()).containsExactly("Wizard", "Warrior", "Druid");
        assertThat(result.getClassIds()).containsExactly(30L, 40L, 20L);
        assertThat(result.getClassId()).isEqualTo(30L);
        assertThat(result.getClassName()).isEqualTo("Wizard");
    }

    @Test
    void getCharacterSheet_MulticlassWithExpandClass_ExpandsInAcquisitionOrder() throws Exception {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid));
        List<CharacterAdvancementLog> logs = List.of(buildMulticlassLog(1L, 5, 2L));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(1L)).thenReturn(logs);
        when(classService.toResponse(eq(druid), anySet()))
                .thenReturn(ClassResponse.builder().id(20L).name("Druid").build());
        when(classService.toResponse(eq(wizard), anySet()))
                .thenReturn(ClassResponse.builder().id(30L).name("Wizard").build());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "class");

        // Assert
        assertThat(result.getClasses()).extracting(ClassResponse::getName).containsExactly("Wizard", "Druid");
        assertThat(result.getClassObject().getName()).isEqualTo("Wizard");
    }

    @Test
    void getCharacterSheet_MulticlassWithMalformedAdvancementData_FallsBackToClassIdAscending() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid));
        CharacterAdvancementLog corruptLog = CharacterAdvancementLog.builder()
                .id(1L).fromLevel(4).toLevel(5).tier(2).advancementData("{not valid json").build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(1L))
                .thenReturn(List.of(corruptLog));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassNames()).containsExactly("Druid", "Wizard");
    }

    @Test
    void getCharacterSheet_MulticlassLogNamingAnUnheldSubclassCard_IgnoresThatAdvancement() throws Exception {
        // Arrange - the Warrior multiclass was undone, so its card is gone from the sheet but the
        // Druid multiclass that followed it must still be ordered after the original Wizard
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(3L, "Warden of Renewal", 6L, "Warden of Renewal", druid));
        List<CharacterAdvancementLog> logs = List.of(buildMulticlassLog(1L, 5, 2L), buildMulticlassLog(2L, 8, 3L));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(1L)).thenReturn(logs);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getClassNames()).containsExactly("Wizard", "Druid");
    }

    @Test
    void getCharacterSheet_SingleClass_DoesNotQueryAdvancementLog() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class warrior = Class.builder().id(10L).name("Warrior").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "Call of the Brave", 5L, "Call of the Brave", warrior));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        characterSheetService.getCharacterSheetById(1L, null);

        // Assert - the common single-class case must not pay for an extra query
        verifyNoInteractions(characterAdvancementLogRepository);
    }

    @Test
    void getCharacterSheet_Multiclass_OrderingIsStableAcrossRepeatedCalls() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid));
        // Same character, subclass cards inserted in the opposite order
        CharacterSheet reorderedSheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid),
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet), Optional.of(reorderedSheet));

        // Act
        List<String> firstCall = characterSheetService.getCharacterSheetById(1L, null).getClassNames();
        List<String> secondCall = characterSheetService.getCharacterSheetById(1L, null).getClassNames();

        // Assert
        assertThat(firstCall).isEqualTo(secondCall).containsExactly("Druid", "Wizard");
    }

    @Test
    void getCharacterSheet_MulticlassWithExpandClass_ExpandsEveryClass() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        Class wizard = Class.builder().id(30L).name("Wizard").build();
        Class druid = Class.builder().id(20L).name("Druid").build();
        CharacterSheet sheet = buildSheetWithSubclassCards(owner,
                buildSubclassCard(1L, "School of Knowledge", 7L, "School of Knowledge", wizard),
                buildSubclassCard(2L, "Warden of Renewal", 6L, "Warden of Renewal", druid));

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(classService.toResponse(eq(druid), anySet()))
                .thenReturn(ClassResponse.builder().id(20L).name("Druid").build());
        when(classService.toResponse(eq(wizard), anySet()))
                .thenReturn(ClassResponse.builder().id(30L).name("Wizard").build());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "class");

        // Assert
        assertThat(result.getClasses()).extracting(ClassResponse::getName).containsExactly("Druid", "Wizard");
        assertThat(result.getClassObject().getName()).isEqualTo("Druid");
    }

    // ==================== GET NOTES TESTS ====================

    @Test
    void getNotes_AsOwner_ReturnsNotesAndTimestamp() {
        // Arrange
        LocalDateTime modified = LocalDateTime.of(2026, 5, 11, 10, 0, 0);
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .notes("Some campaign notes")
                .owner(owner)
                .build();
        sheet.setLastModifiedAt(modified);

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetNotesResponse result = characterSheetService.getNotes(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNotes()).isEqualTo("Some campaign notes");
        assertThat(result.getLastModifiedAt()).isEqualTo(modified);
    }

    @Test
    void getNotes_AsModerator_ReturnsNotes() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User moderator = User.builder().id(2L).username("moderator1").role(Role.MODERATOR).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .notes("Some campaign notes")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        CharacterSheetNotesResponse result = characterSheetService.getNotes(1L, authentication);

        // Assert
        assertThat(result.getNotes()).isEqualTo("Some campaign notes");
    }

    @Test
    void getNotes_AsOtherUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User otherUser = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .notes("Some campaign notes")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.getNotes(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to view notes for this character sheet");
    }

    @Test
    void getNotes_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(characterSheetRepository.findActiveById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.getNotes(99L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 99");
    }

    @Test
    void getNotes_SoftDeleted_ThrowsEntityNotFoundException() {
        // Arrange — findActiveById returns empty for soft-deleted sheets
        when(characterSheetRepository.findActiveById(5L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.getNotes(5L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 5");
    }

    // ==================== UPDATE NOTES TESTS ====================

    @Test
    void updateNotes_AsOwner_SavesSanitizedNotes() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateNotes(1L, "My notes", authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("My notes");
    }

    @Test
    void updateNotes_AsModerator_SavesSanitizedNotes() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User moderator = User.builder().id(2L).username("moderator1").role(Role.MODERATOR).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateNotes(1L, "Moderator notes", authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("Moderator notes");
    }

    @Test
    void updateNotes_AsOtherUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User otherUser = User.builder().id(2L).username("player2").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateNotes(1L, "Hacked notes", authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to update notes this character sheet");

        verify(characterSheetRepository, never()).save(any(CharacterSheet.class));
    }

    @Test
    void updateNotes_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(characterSheetRepository.findActiveById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateNotes(99L, "Notes", authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 99");

        verify(characterSheetRepository, never()).save(any(CharacterSheet.class));
    }

    @Test
    void updateNotes_EmptyString_ClearsNotes() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .notes("Old notes")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateNotes(1L, "", authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("");
    }

    @Test
    void updateNotes_XssPayload_Stripped() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateNotes(1L, "<script>x</script>hello", authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).doesNotContain("<script>");
        assertThat(captor.getValue().getNotes()).contains("hello");
    }

    @Test
    void updateNotes_JavascriptUri_Neutralized() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenAnswer(invocation -> {
            CharacterSheet saved = invocation.getArgument(0);
            saved.setCommunityCards(new HashSet<>());
            saved.setAncestryCards(new HashSet<>());
            saved.setSubclassCards(new HashSet<>());
            saved.setCharacterSheetDomainCards(new HashSet<>());
            saved.setCharacterSheetWeapons(new HashSet<>());
            saved.setCharacterSheetArmors(new HashSet<>());
            saved.setCharacterSheetLoot(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        characterSheetService.updateNotes(1L, "[x](javascript:alert(1))", authentication);

        // Assert
        ArgumentCaptor<CharacterSheet> captor = ArgumentCaptor.forClass(CharacterSheet.class);
        verify(characterSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).doesNotContain("javascript:");
        assertThat(captor.getValue().getNotes()).contains("unsafe:");
    }

    // ==================== COMPANIONS TESTS ====================

    @Test
    void getCharacterSheetById_CompanionsEnabledFalse_ReflectsFlag() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .companionsEnabled(false)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.isCompanionsEnabled()).isFalse();
        assertThat(result.getCompanionGrantedHopeSlots()).isZero();
        assertThat(result.getCompanions()).isNull();
    }

    @Test
    void getCharacterSheetById_CompanionsEnabledTrue_ReflectsFlag() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .companionsEnabled(true)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.isCompanionsEnabled()).isTrue();
    }

    @Test
    void getCharacterSheetById_WithActiveCompanions_SumsCompanionGrantedHopeSlots() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        CompanionTraining lightInTheDark = CompanionTraining.builder()
                .option(CompanionTrainingOption.LIGHT_IN_THE_DARK)
                .acquiredAtLevel(2)
                .build();
        Companion companion = Companion.builder()
                .id(7L)
                .characterSheet(sheet)
                .name("Rufus")
                .attackName("Bite")
                .trainings(new HashSet<>(List.of(lightInTheDark)))
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        assertThat(result.getCompanionGrantedHopeSlots()).isEqualTo(1);
        // Not expanded, so the full companion list should not be populated even though active companions exist
        assertThat(result.getCompanions()).isNull();
    }

    @Test
    void getCharacterSheetById_WithCompanionsExpansion_IncludesFullCompanionObjects() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Companion companion = Companion.builder()
                .id(7L)
                .characterSheet(sheet)
                .name("Rufus")
                .attackName("Bite")
                .build();

        CompanionResponse companionResponse = CompanionResponse.builder()
                .id(7L)
                .characterSheetId(1L)
                .name("Rufus")
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(companionService.toResponse(eq(companion), anySet())).thenReturn(companionResponse);

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(1L, "companions");

        // Assert
        assertThat(result.getCompanions()).hasSize(1);
        assertThat(result.getCompanions().get(0).getName()).isEqualTo("Rufus");
        verify(companionService).toResponse(eq(companion), anySet());
    }

    @Test
    void getCharacterSheetById_QueriesOnlyActiveCompanions_SoftDeletedExcludedByRepository() {
        // Arrange -- the repository query itself excludes soft-deleted companions; this test
        // asserts the service calls the "active" overload rather than the unfiltered one.
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        // Act
        characterSheetService.getCharacterSheetById(1L, null);

        // Assert
        verify(companionRepository).findActiveByCharacterSheetId(1L);
        verify(companionRepository, never()).findByCharacterSheetId(anyLong());
    }
}
