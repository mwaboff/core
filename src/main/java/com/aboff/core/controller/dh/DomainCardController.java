package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainCardRequest;
import com.aboff.core.model.dto.dh.response.DomainCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.service.dh.DomainCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing DomainCard resources.
 * Provides endpoints for CRUD operations on Daggerheart domain cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/domain")
@RequiredArgsConstructor
public class DomainCardController {

    private final DomainCardService domainCardService;

    /**
     * Retrieves a paginated list of domain cards.
     * Results are sorted by level ascending, then by name alphabetically.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedDomainIds Optional list of associated domain IDs to filter by
     * @param type Optional filter for domain card type
     * @param levels Optional list of levels to filter by
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,associatedDomain")
     * @return Paginated response containing domain cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<DomainCardResponse>> getAllDomainCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) List<Long> associatedDomainIds,
            @RequestParam(required = false) DomainCardType type,
            @RequestParam(required = false) List<Integer> levels,
            @RequestParam(required = false) String expand) {

        PagedResponse<DomainCardResponse> response = domainCardService.getAllDomainCards(
                page, size, includeDeleted, expansionId, isOfficial, associatedDomainIds, type, levels, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single domain card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,associatedDomain")
     * @return DomainCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DomainCardResponse> getDomainCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        DomainCardResponse response = domainCardService.getDomainCardById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new domain card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @return DomainCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainCardResponse> createDomainCard(
            @Valid @RequestBody CreateDomainCardRequest request) {

        DomainCardResponse response = domainCardService.createDomainCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple domain cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<DomainCardResponse>> createDomainCardsBulk(
            @Valid @RequestBody List<CreateDomainCardRequest> requests) {

        List<DomainCardResponse> responses = domainCardService.createDomainCardsBulk(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing domain card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return DomainCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainCardResponse> updateDomainCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDomainCardRequest request) {

        DomainCardResponse response = domainCardService.updateDomainCard(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a domain card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteDomainCard(@PathVariable Long id) {
        domainCardService.deleteDomainCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted domain card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @return DomainCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainCardResponse> restoreDomainCard(@PathVariable Long id) {
        DomainCardResponse response = domainCardService.restoreDomainCard(id);
        return ResponseEntity.ok(response);
    }
}
