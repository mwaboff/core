package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.security.CustomUserDetails;
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
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CharacterSheetService characterSheetService;

    // ==================== GET ALL CHARACTER SHEETS TESTS ====================

    @Test
    void getAllCharacterSheets_WithoutFilters_ReturnsPagedSheets() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet1 = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
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
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet1, sheet2));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Aragorn");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Legolas");
    }

    @Test
    void getAllCharacterSheets_FilterByOwnerId_ReturnsFiltered() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(1L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, 1L, null, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOwnerId()).isEqualTo(1L);
        verify(characterSheetRepository).findActiveWithFilters(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_FilterByName_ReturnsFiltered() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq("Ara"), eq(null), eq(null), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, "Ara", null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Aragorn");
        verify(characterSheetRepository).findActiveWithFilters(eq(null), eq("Ara"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAllCharacterSheets_FilterByLevelRange_ReturnsFiltered() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .level(5)
                .owner(owner)
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>())
                .build();

        Page<CharacterSheet> sheetPage = new PageImpl<>(List.of(sheet));
        when(characterSheetRepository.findActiveWithFilters(
                eq(null), eq(null), eq(3), eq(7), any(Pageable.class))).thenReturn(sheetPage);

        // Act
        PagedResponse<CharacterSheetResponse> result = characterSheetService.getAllCharacterSheets(
                0, 20, null, null, 3, 7, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLevel()).isEqualTo(5);
        verify(characterSheetRepository).findActiveWithFilters(eq(null), eq(null), eq(3), eq(7), any(Pageable.class));
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
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
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
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>(List.of(exp)))
                .createdAt(LocalDateTime.now())
                .build();

        exp.setCharacterSheet(sheet);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

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
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
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
    void createCharacterSheet_WithArmorMarkedExceedsMax_ThrowsException() {
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

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Armor marked")
                .hasMessageContaining("cannot exceed armor max");
    }

    @Test
    void createCharacterSheet_WithHitPointMarkedExceedsMax_ThrowsException() {
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

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hit point marked")
                .hasMessageContaining("cannot exceed hit point max");
    }

    @Test
    void createCharacterSheet_WithStressMarkedExceedsMax_ThrowsException() {
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

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stress marked")
                .hasMessageContaining("cannot exceed stress max");
    }

    @Test
    void createCharacterSheet_WithHopeMarkedExceedsMax_ThrowsException() {
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

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.createCharacterSheet(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hope marked")
                .hasMessageContaining("cannot exceed hope max");
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

        // Act
        characterSheetService.deleteCharacterSheet(1L, authentication);

        // Assert
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
                .activePrimaryWeaponId(1L)
                .activeSecondaryWeaponId(2L)
                .activeArmorId(1L)
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getActivePrimaryWeaponId()).isEqualTo(1L);
        assertThat(result.getActiveSecondaryWeaponId()).isEqualTo(2L);
        assertThat(result.getActiveArmorId()).isEqualTo(1L);
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
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
                .inventoryWeaponIds(List.of(1L))
                .inventoryArmorIds(List.of(1L))
                .inventoryItemIds(List.of(1L))
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
            saved.setDomainCards(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInventoryWeaponIds()).contains(1L);
        assertThat(result.getInventoryArmorIds()).contains(1L);
        assertThat(result.getInventoryItemIds()).contains(1L);
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
                .activePrimaryWeaponId(999L)
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
                .domainCardIds(List.of(1L))
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
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.createCharacterSheet(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomainCardIds()).contains(1L);
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
                .domainCardIds(List.of(999L))
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
                .activePrimaryWeaponId(1L)
                .activeArmorId(1L)
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getActivePrimaryWeaponId()).isEqualTo(1L);
        assertThat(result.getActiveArmorId()).isEqualTo(1L);
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
                .inventoryWeapons(new HashSet<>())
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
                .inventoryWeaponIds(List.of(1L))
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
            saved.setDomainCards(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
            saved.setExperiences(new HashSet<>());
            return saved;
        });

        // Act
        CharacterSheetResponse result = characterSheetService.updateCharacterSheet(1L, request, authentication);

        // Assert
        assertThat(result.getInventoryWeaponIds()).contains(1L);
    }

    @Test
    void updateCharacterSheet_WithConstraintViolation_ThrowsException() {
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
                .armorMarked(10) // Exceeds armorMax
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> characterSheetService.updateCharacterSheet(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Armor marked")
                .hasMessageContaining("cannot exceed armor max");
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
                .activePrimaryWeapon(primaryWeapon)
                .activeArmor(armor)
                .communityCards(new HashSet<>(List.of(communityCard)))
                .ancestryCards(new HashSet<>())
                .subclassCards(new HashSet<>())
                .domainCards(new HashSet<>())
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>(List.of(exp)))
                .createdAt(LocalDateTime.now())
                .build();

        exp.setCharacterSheet(sheet);

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

        // Act
        CharacterSheetResponse result = characterSheetService.getCharacterSheetById(
                1L, "owner,experiences,activePrimaryWeapon,activeArmor,communityCards");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOwner()).isNotNull();
        assertThat(result.getOwner().getUsername()).isEqualTo("player1");
        assertThat(result.getExperiences()).hasSize(1);
        assertThat(result.getExperiences().get(0).getDescription()).isEqualTo("Survived dragon attack");
        assertThat(result.getActivePrimaryWeapon()).isNotNull();
        assertThat(result.getActivePrimaryWeapon().getName()).isEqualTo("Longsword");
        assertThat(result.getActiveArmor()).isNotNull();
        assertThat(result.getActiveArmor().getName()).isEqualTo("Plate Mail");
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
                .domainCardIds(List.of(1L))
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
            saved.setInventoryWeapons(new HashSet<>());
            saved.setInventoryArmors(new HashSet<>());
            saved.setInventoryItems(new HashSet<>());
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
                .domainCards(new HashSet<>(List.of(domainCard)))
                .inventoryWeapons(new HashSet<>())
                .inventoryArmors(new HashSet<>())
                .inventoryItems(new HashSet<>())
                .experiences(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));

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
}
