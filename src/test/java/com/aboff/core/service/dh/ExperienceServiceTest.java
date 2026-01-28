package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateExperienceRequest;
import com.aboff.core.model.dto.dh.request.UpdateExperienceRequest;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.CharacterSheetRepository;
import com.aboff.core.repository.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExperienceService.
 * Tests all CRUD operations, access control, pagination, filtering, and expansion.
 */
@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExperienceService experienceService;

    // ==================== GET ALL EXPERIENCES TESTS ====================

    @Test
    void getAllExperiences_WithoutFilters_ReturnsPagedExperiences() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp1 = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        Experience exp2 = Experience.builder()
                .id(2L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Negotiated peace treaty")
                .modifier(3)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Page<Experience> experiencePage = new PageImpl<>(List.of(exp1, exp2));
        when(experienceRepository.findAll(any(Pageable.class))).thenReturn(experiencePage);

        // Act
        PagedResponse<ExperienceResponse> result = experienceService.getAllExperiences(0, 20, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Survived dragon attack");
        assertThat(result.getContent().get(1).getDescription()).isEqualTo("Negotiated peace treaty");
    }

    @Test
    void getAllExperiences_WithCharacterSheetFilter_ReturnsFilteredExperiences() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Experience> experiencePage = new PageImpl<>(List.of(exp));
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(experienceRepository.findByCharacterSheetId(eq(1L), any(Pageable.class))).thenReturn(experiencePage);

        // Act
        PagedResponse<ExperienceResponse> result = experienceService.getAllExperiences(0, 20, 1L, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCharacterSheetId()).isEqualTo(1L);
        verify(characterSheetRepository).findActiveById(1L);
        verify(experienceRepository).findByCharacterSheetId(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllExperiences_WithInvalidCharacterSheetFilter_ThrowsEntityNotFoundException() {
        // Arrange
        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.getAllExperiences(0, 20, 999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 999");
    }

    // ==================== GET EXPERIENCE BY ID TESTS ====================

    @Test
    void getExperienceById_WithValidId_ReturnsExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        // Act
        ExperienceResponse result = experienceService.getExperienceById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Survived dragon attack");
        assertThat(result.getModifier()).isEqualTo(2);
    }

    @Test
    void getExperienceById_WithInvalidId_ThrowsEntityNotFoundException() {
        // Arrange
        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.getExperienceById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Experience not found with id: 999");
    }

    // ==================== CREATE EXPERIENCE TESTS ====================

    @Test
    void createExperience_WithValidData_CreatesExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(1L)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        Experience savedExp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(creator));
        when(experienceRepository.save(any(Experience.class))).thenReturn(savedExp);

        // Act
        ExperienceResponse result = experienceService.createExperience(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Survived dragon attack");
        assertThat(result.getModifier()).isEqualTo(2);
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    void createExperience_WithDefaultModifier_UsesDefaultValue() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(1L)
                .description("Survived dragon attack")
                .build();

        Experience savedExp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(creator));
        when(experienceRepository.save(any(Experience.class))).thenReturn(savedExp);

        // Act
        ExperienceResponse result = experienceService.createExperience(request, authentication);

        // Assert
        assertThat(result.getModifier()).isEqualTo(2);
    }

    @Test
    void createExperience_WithInvalidCharacterSheet_ThrowsEntityNotFoundException() {
        // Arrange
        User creator = User.builder().id(2L).username("gm1").build();
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(999L)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.createExperience(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found with id: 999");
    }

    // ==================== UPDATE EXPERIENCE TESTS ====================

    @Test
    void updateExperience_AsOwner_UpdatesExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .modifier(3)
                .build();

        Experience updatedExp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack on Redstone Village")
                .modifier(3)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(experienceRepository.save(any(Experience.class))).thenReturn(updatedExp);

        // Act
        ExperienceResponse result = experienceService.updateExperience(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Survived dragon attack on Redstone Village");
        assertThat(result.getModifier()).isEqualTo(3);
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    void updateExperience_AsModerator_UpdatesExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User moderator = User.builder().id(3L).username("moderator1").role(Role.MODERATOR).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        Experience updatedExp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack on Redstone Village")
                .modifier(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(experienceRepository.save(any(Experience.class))).thenReturn(updatedExp);
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        ExperienceResponse result = experienceService.updateExperience(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Survived dragon attack on Redstone Village");
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    void updateExperience_AsUnauthorizedUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User otherUser = User.builder().id(3L).username("player2").role(Role.USER).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        // Act & Assert
        assertThatThrownBy(() -> experienceService.updateExperience(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to update this experience");
    }

    @Test
    void updateExperience_WithPartialUpdate_OnlyUpdatesProvidedFields() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExperienceResponse result = experienceService.updateExperience(1L, request, authentication);

        // Assert
        assertThat(result.getDescription()).isEqualTo("Survived dragon attack on Redstone Village");
        assertThat(result.getModifier()).isEqualTo(2); // Modifier should remain unchanged
    }

    // ==================== DELETE EXPERIENCE TESTS ====================

    @Test
    void deleteExperience_AsOwner_DeletesExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        // Act
        experienceService.deleteExperience(1L, authentication);

        // Assert
        verify(experienceRepository).delete(exp);
    }

    @Test
    void deleteExperience_AsModerator_DeletesExperience() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User moderator = User.builder().id(3L).username("moderator1").role(Role.MODERATOR).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        // Act
        experienceService.deleteExperience(1L, authentication);

        // Assert
        verify(experienceRepository).delete(exp);
    }

    @Test
    void deleteExperience_AsUnauthorizedUser_ThrowsInsufficientPermissionsException() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        User otherUser = User.builder().id(3L).username("player2").role(Role.USER).build();
        User creator = User.builder().id(2L).username("gm1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        // Act & Assert
        assertThatThrownBy(() -> experienceService.deleteExperience(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to delete this experience");

        verify(experienceRepository, never()).delete(any(Experience.class));
    }

    @Test
    void deleteExperience_WithInvalidId_ThrowsEntityNotFoundException() {
        // Arrange
        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.deleteExperience(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Experience not found with id: 999");
    }

    // ==================== EXPANSION TESTS ====================

    @Test
    void getExperienceById_WithExpansion_IncludesExpandedEntities() {
        // Arrange
        User owner = User.builder()
                .id(1L)
                .username("player1")
                .email("player1@example.com")
                .build();
        User creator = User.builder()
                .id(2L)
                .username("gm1")
                .email("gm1@example.com")
                .build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .pronouns("he/him")
                .level(5)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        Experience exp = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(creator)
                .description("Survived dragon attack")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        // Act
        ExperienceResponse result = experienceService.getExperienceById(1L, "characterSheet,createdBy");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCharacterSheet()).isNotNull();
        assertThat(result.getCharacterSheet().getName()).isEqualTo("Aragorn");
        assertThat(result.getCreatedBy()).isNotNull();
        assertThat(result.getCreatedBy().getUsername()).isEqualTo("gm1");
    }
}
