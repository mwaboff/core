package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.dto.dh.response.CommunityCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.CommunityCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing CommunityCard resources.
 * Provides endpoints for CRUD operations on Daggerheart community cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/community")
@RequiredArgsConstructor
public class CommunityCardController {

    private final CommunityCardService communityCardService;

    /**
     * Retrieves a paginated list of community cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return Paginated response containing community cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CommunityCardResponse>> getAllCommunityCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String expand) {

        PagedResponse<CommunityCardResponse> response = communityCardService.getAllCommunityCards(
                page, size, includeDeleted, expansionId, isOfficial, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single community card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return CommunityCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityCardResponse> getCommunityCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        CommunityCardResponse response = communityCardService.getCommunityCardById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new community card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @return CommunityCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> createCommunityCard(
            @Valid @RequestBody CreateCommunityCardRequest request) {

        CommunityCardResponse response = communityCardService.createCommunityCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple community cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<CommunityCardResponse>> createCommunityCardsBulk(
            @Valid @RequestBody List<CreateCommunityCardRequest> requests) {

        List<CommunityCardResponse> responses = communityCardService.createCommunityCardsBulk(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return CommunityCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> updateCommunityCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommunityCardRequest request) {

        CommunityCardResponse response = communityCardService.updateCommunityCard(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteCommunityCard(@PathVariable Long id) {
        communityCardService.deleteCommunityCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @return CommunityCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> restoreCommunityCard(@PathVariable Long id) {
        CommunityCardResponse response = communityCardService.restoreCommunityCard(id);
        return ResponseEntity.ok(response);
    }
}
