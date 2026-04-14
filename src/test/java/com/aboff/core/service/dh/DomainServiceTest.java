package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainRequest;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DomainService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DomainService domainService;

    // ==================== GET ALL DOMAINS TESTS ====================

    @Test
    void getAllDomains_WithoutFilters_ReturnsPagedDomains() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain domain1 = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Domain domain2 = Domain.builder()
                .id(2L)
                .name("Blade")
                .iconUrl("https://icon.url/blade")
                .description("Weapons and combat")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Domain> domainPage = new PageImpl<>(List.of(domain1, domain2));
        when(domainRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(domainPage);

        // Act
        PagedResponse<DomainResponse> result = domainService.getAllDomains(0, 20, false, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Arcana");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Blade");
    }

    @Test
    void getAllDomains_WithExpansionFilter_ReturnsFilteredDomains() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Domain> domainPage = new PageImpl<>(List.of(domain));
        when(domainRepository.findByDeletedAtIsNullAndExpansion(eq(1L), any(Pageable.class)))
                .thenReturn(domainPage);

        // Act
        PagedResponse<DomainResponse> result = domainService.getAllDomains(0, 20, false, 1L, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(domainRepository).findByDeletedAtIsNullAndExpansion(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllDomains_WithIncludeDeleted_ReturnsAllDomains() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("Deleted Domain")
                .iconUrl("https://icon.url/deleted")
                .description("Deleted")
                .expansion(expansion)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<Domain> domainPage = new PageImpl<>(List.of(domain));
        when(domainRepository.findAllWithExpansion(isNull(), any(Pageable.class)))
                .thenReturn(domainPage);

        // Act
        PagedResponse<DomainResponse> result = domainService.getAllDomains(0, 20, true, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(domainRepository).findAllWithExpansion(isNull(), any(Pageable.class));
    }

    @Test
    void getAllDomains_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Domain> domainPage = new PageImpl<>(List.of());
        when(domainRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(domainPage);

        // Act
        domainService.getAllDomains(0, 500, false, null, null);

        // Assert
        verify(domainRepository).findByDeletedAtIsNullAndExpansion(
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllDomains_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Domain> domainPage = new PageImpl<>(List.of(domain));
        when(domainRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(domainPage);

        // Act
        PagedResponse<DomainResponse> result = domainService.getAllDomains(0, 20, false, null, "expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    // ==================== GET DOMAIN BY ID TESTS ====================

    @Test
    void getDomainById_ValidId_ReturnsDomain() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(domain));

        // Act
        DomainResponse result = domainService.getDomainById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Arcana");
        assertThat(result.getIconUrl()).isEqualTo("https://icon.url/arcana");
        assertThat(result.getDescription()).isEqualTo("Magic and spells");
    }

    @Test
    void getDomainById_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(domain));

        // Act
        DomainResponse result = domainService.getDomainById(1L, "expansion");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExpansion()).isNotNull();
        assertThat(result.getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    @Test
    void getDomainById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.getDomainById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Domain not found with id: 999");
    }

    // ==================== CREATE DOMAIN TESTS ====================

    @Test
    void createDomain_ValidRequest_CreatesAndReturnsDomain() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansionId(1L)
                .build();

        Domain savedDomain = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(domainRepository.save(any(Domain.class)))
                .thenReturn(savedDomain);

        // Act
        DomainResponse result = domainService.createDomain(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Arcana");
        assertThat(result.getIconUrl()).isEqualTo("https://icon.url/arcana");
        assertThat(result.getDescription()).isEqualTo("Magic and spells");

        verify(domainRepository).save(argThat(domain ->
                domain.getName().equals("Arcana") &&
                        domain.getIconUrl().equals("https://icon.url/arcana") &&
                        domain.getDescription().equals("Magic and spells")
        ));
    }

    @Test
    void createDomain_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansionId(999L)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.createDomain(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(domainRepository, never()).save(any());
    }

    // ==================== CREATE DOMAINS BULK TESTS ====================

    @Test
    void createDomainsBulk_ValidRequests_CreatesAndReturnsDomains() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        CreateDomainRequest request1 = CreateDomainRequest.builder()
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansionId(1L)
                .build();

        CreateDomainRequest request2 = CreateDomainRequest.builder()
                .name("Blade")
                .iconUrl("https://icon.url/blade")
                .description("Weapons and combat")
                .expansionId(1L)
                .build();

        Domain savedDomain1 = Domain.builder()
                .id(1L)
                .name("Arcana")
                .iconUrl("https://icon.url/arcana")
                .description("Magic and spells")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Domain savedDomain2 = Domain.builder()
                .id(2L)
                .name("Blade")
                .iconUrl("https://icon.url/blade")
                .description("Weapons and combat")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(domainRepository.saveAll(anyList()))
                .thenReturn(List.of(savedDomain1, savedDomain2));

        // Act
        List<DomainResponse> results = domainService.createDomainsBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Arcana");
        assertThat(results.get(1).getName()).isEqualTo("Blade");
        verify(domainRepository).saveAll(anyList());
    }

    // ==================== UPDATE DOMAIN TESTS ====================

    @Test
    void updateDomain_ValidRequest_UpdatesAndReturnsDomain() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain existingDomain = Domain.builder()
                .id(1L)
                .name("Old Name")
                .iconUrl("https://icon.url/old")
                .description("Old description")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Updated Name")
                .iconUrl("https://icon.url/updated")
                .description("Updated description")
                .expansionId(1L)
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingDomain));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(domainRepository.save(any(Domain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DomainResponse result = domainService.updateDomain(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getIconUrl()).isEqualTo("https://icon.url/updated");
        assertThat(result.getDescription()).isEqualTo("Updated description");

        verify(domainRepository).save(argThat(domain ->
                domain.getName().equals("Updated Name") &&
                        domain.getIconUrl().equals("https://icon.url/updated") &&
                        domain.getDescription().equals("Updated description")
        ));
    }

    @Test
    void updateDomain_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Updated Name")
                .iconUrl("https://icon.url/updated")
                .description("Updated description")
                .expansionId(1L)
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.updateDomain(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Domain not found with id: 999");

        verify(domainRepository, never()).save(any());
    }

    @Test
    void updateDomain_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain existingDomain = Domain.builder()
                .id(1L)
                .name("Old Name")
                .iconUrl("https://icon.url/old")
                .description("Old description")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Updated Name")
                .iconUrl("https://icon.url/updated")
                .description("Updated description")
                .expansionId(999L)
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingDomain));
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.updateDomain(1L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(domainRepository, never()).save(any());
    }

    // ==================== DELETE DOMAIN TESTS ====================

    @Test
    void deleteDomain_ValidId_SoftDeletesDomain() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain domain = Domain.builder()
                .id(1L)
                .name("To Delete")
                .iconUrl("https://icon.url/delete")
                .description("To be deleted")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(domain));

        // Act
        domainService.deleteDomain(1L, authentication);

        // Assert
        verify(domainRepository).save(argThat(d -> d.getDeletedAt() != null));
    }

    @Test
    void deleteDomain_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.deleteDomain(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Domain not found with id: 999");

        verify(domainRepository, never()).save(any());
    }

    // ==================== RESTORE DOMAIN TESTS ====================

    @Test
    void restoreDomain_DeletedDomain_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain deletedDomain = Domain.builder()
                .id(1L)
                .name("Deleted Domain")
                .iconUrl("https://icon.url/deleted")
                .description("Deleted")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(domainRepository.findById(1L))
                .thenReturn(Optional.of(deletedDomain));
        when(domainRepository.save(any(Domain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DomainResponse result = domainService.restoreDomain(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(domainRepository).save(argThat(d -> d.getDeletedAt() == null));
    }

    @Test
    void restoreDomain_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Domain activeDomain = Domain.builder()
                .id(1L)
                .name("Active Domain")
                .iconUrl("https://icon.url/active")
                .description("Active")
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainRepository.findById(1L))
                .thenReturn(Optional.of(activeDomain));

        // Act & Assert
        assertThatThrownBy(() -> domainService.restoreDomain(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Domain with id 1 is not deleted");

        verify(domainRepository, never()).save(any());
    }

    @Test
    void restoreDomain_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainService.restoreDomain(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Domain not found with id: 999");
    }
}
