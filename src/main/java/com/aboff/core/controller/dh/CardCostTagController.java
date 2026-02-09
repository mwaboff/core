package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.service.dh.CardCostTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing CardCostTag resources.
 * Provides endpoints for CRUD operations on card cost/limitation tags.
 */
@RestController
@RequestMapping("/api/dh/cost-tags")
@RequiredArgsConstructor
public class CardCostTagController {

    private final CardCostTagService cardCostTagService;

    /**
     * Retrieves a paginated list of cost tags.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cost tags (default: false, ADMIN+ only)
     * @param category Optional filter for cost tag category
     * @return Paginated response containing cost tags
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CardCostTagResponse>> getAllCostTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) CostTagCategory category) {

        PagedResponse<CardCostTagResponse> response = cardCostTagService.getAllCostTags(
                page, size, includeDeleted, category);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single cost tag by ID.
     *
     * @param id The cost tag ID
     * @return CardCostTagResponse containing the cost tag details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardCostTagResponse> getCostTagById(@PathVariable Long id) {
        CardCostTagResponse response = cardCostTagService.getCostTagById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new cost tag.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing cost tag details
     * @return CardCostTagResponse containing the created cost tag
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CardCostTagResponse> createCostTag(
            @Valid @RequestBody CreateCardCostTagRequest request) {

        CardCostTagResponse response = cardCostTagService.createCostTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing cost tag.
     * Requires ADMIN or OWNER role.
     *
     * @param id The cost tag ID to update
     * @param request The update request containing new cost tag details
     * @return CardCostTagResponse containing the updated cost tag
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CardCostTagResponse> updateCostTag(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCardCostTagRequest request) {

        CardCostTagResponse response = cardCostTagService.updateCostTag(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a cost tag.
     * Requires ADMIN or OWNER role.
     *
     * @param id The cost tag ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteCostTag(@PathVariable Long id) {
        cardCostTagService.deleteCostTag(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted cost tag.
     * Requires ADMIN or OWNER role.
     *
     * @param id The cost tag ID to restore
     * @return CardCostTagResponse containing the restored cost tag
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CardCostTagResponse> restoreCostTag(@PathVariable Long id) {
        CardCostTagResponse response = cardCostTagService.restoreCostTag(id);
        return ResponseEntity.ok(response);
    }
}
