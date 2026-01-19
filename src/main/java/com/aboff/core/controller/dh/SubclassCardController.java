package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.service.dh.SubclassCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing SubclassCard resources.
 * Provides endpoints for CRUD operations on Daggerheart subclass cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/subclass")
@RequiredArgsConstructor
public class SubclassCardController {

    private final SubclassCardService subclassCardService;

    /**
     * Retrieves a paginated list of subclass cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID
     * @param level Optional filter for subclass level
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,associatedClass")
     * @return Paginated response containing subclass cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<SubclassCardResponse>> getAllSubclassCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Long associatedClassId,
            @RequestParam(required = false) SubclassLevel level,
            @RequestParam(required = false) String expand) {

        PagedResponse<SubclassCardResponse> response = subclassCardService.getAllSubclassCards(
                page, size, includeDeleted, expansionId, isOfficial, associatedClassId, level, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single subclass card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,associatedClass")
     * @return SubclassCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubclassCardResponse> getSubclassCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        SubclassCardResponse response = subclassCardService.getSubclassCardById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @return SubclassCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> createSubclassCard(
            @Valid @RequestBody CreateSubclassCardRequest request) {

        SubclassCardResponse response = subclassCardService.createSubclassCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple subclass cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<SubclassCardResponse>> createSubclassCardsBulk(
            @Valid @RequestBody List<CreateSubclassCardRequest> requests) {

        List<SubclassCardResponse> responses = subclassCardService.createSubclassCardsBulk(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return SubclassCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> updateSubclassCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubclassCardRequest request) {

        SubclassCardResponse response = subclassCardService.updateSubclassCard(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteSubclassCard(@PathVariable Long id) {
        subclassCardService.deleteSubclassCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @return SubclassCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> restoreSubclassCard(@PathVariable Long id) {
        SubclassCardResponse response = subclassCardService.restoreSubclassCard(id);
        return ResponseEntity.ok(response);
    }
}
