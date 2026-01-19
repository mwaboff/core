package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateExpansionRequest;
import com.aboff.core.model.dto.dh.request.UpdateExpansionRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.ExpansionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Expansion resources.
 * Provides endpoints for CRUD operations on Daggerheart expansions.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/expansions")
@RequiredArgsConstructor
public class ExpansionController {

    private final ExpansionService expansionService;

    /**
     * Retrieves a paginated list of expansions.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted expansions (default: false, ADMIN+ only)
     * @param published Optional filter for published status
     * @return Paginated response containing expansions
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ExpansionResponse>> getAllExpansions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Boolean published) {

        PagedResponse<ExpansionResponse> response = expansionService.getAllExpansions(
                page, size, includeDeleted, published);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single expansion by ID.
     *
     * @param id The expansion ID
     * @return ExpansionResponse containing the expansion details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpansionResponse> getExpansionById(@PathVariable Long id) {
        ExpansionResponse response = expansionService.getExpansionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing expansion details
     * @return ExpansionResponse containing the created expansion
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> createExpansion(
            @Valid @RequestBody CreateExpansionRequest request) {

        ExpansionResponse response = expansionService.createExpansion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to update
     * @param request The update request containing new expansion details
     * @return ExpansionResponse containing the updated expansion
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> updateExpansion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpansionRequest request) {

        ExpansionResponse response = expansionService.updateExpansion(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteExpansion(@PathVariable Long id) {
        expansionService.deleteExpansion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to restore
     * @return ExpansionResponse containing the restored expansion
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> restoreExpansion(@PathVariable Long id) {
        ExpansionResponse response = expansionService.restoreExpansion(id);
        return ResponseEntity.ok(response);
    }
}
