package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.BatchCreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.CreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.UpdateAdversaryRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.BatchCreateAdversaryResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.service.dh.AdversaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Adversary resources.
 * Provides endpoints for CRUD operations on Daggerheart adversaries (enemies/NPCs).
 * <p>
 * Access control:
 * </p>
 * <ul>
 *   <li>GET endpoints: All authenticated users (filtered by visibility)</li>
 *   <li>POST (single/copy): All authenticated users</li>
 *   <li>POST (batch): MODERATOR+ only</li>
 *   <li>PUT/DELETE: Permission check in service (creator OR moderator+ for non-official,
 *       OWNER only for official)</li>
 *   <li>POST restore: ADMIN/OWNER only</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dh/adversaries")
@RequiredArgsConstructor
public class AdversaryController {

    private final AdversaryService adversaryService;

    /**
     * Retrieves a paginated list of adversaries.
     * Returns adversaries that are official, public, or created by the authenticated user.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted adversaries (ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param adversaryType Optional filter for adversary type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param expand Comma-separated list of relationships to expand
     *               (expansion, creator, originalAdversary, experiences, features)
     * @param auth Authentication context
     * @return Paginated response containing adversaries
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AdversaryResponse>> getAllAdversaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) AdversaryType adversaryType,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String expand,
            Authentication auth) {

        PagedResponse<AdversaryResponse> response = adversaryService.getAllAdversaries(
                page, size, includeDeleted, expansionId, tier, adversaryType,
                isOfficial, name, expand, auth);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single adversary by ID.
     * Access is restricted to official, public, or user's own adversaries.
     *
     * @param id The adversary ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return AdversaryResponse containing the adversary details
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdversaryResponse> getAdversaryById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication auth) {

        AdversaryResponse response = adversaryService.getAdversaryById(id, expand, auth);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new adversary.
     * The authenticated user becomes the creator.
     *
     * @param request The creation request containing adversary details
     * @param auth Authentication context
     * @return AdversaryResponse containing the created adversary
     */
    @PostMapping
    public ResponseEntity<AdversaryResponse> createAdversary(
            @Valid @RequestBody CreateAdversaryRequest request,
            Authentication auth) {

        AdversaryResponse response = adversaryService.createAdversary(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple adversaries in a batch operation.
     * Requires MODERATOR, ADMIN, or OWNER role.
     * Supports partial success - individual failures do not affect other creates.
     *
     * @param request The batch creation request containing multiple adversaries
     * @param auth Authentication context
     * @return BatchCreateAdversaryResponse containing created adversaries and any errors
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'OWNER')")
    public ResponseEntity<BatchCreateAdversaryResponse> batchCreateAdversaries(
            @Valid @RequestBody BatchCreateAdversaryRequest request,
            Authentication auth) {

        BatchCreateAdversaryResponse response = adversaryService.batchCreateAdversaries(request, auth);

        // Return 207 Multi-Status if there are partial failures
        HttpStatus status = response.getTotalFailed() > 0 && response.getTotalCreated() > 0
                ? HttpStatus.MULTI_STATUS
                : response.getTotalFailed() > 0
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Creates a copy of an existing adversary for the authenticated user.
     * The copy is private by default and linked to the original via originalAdversaryId.
     *
     * @param id The ID of the adversary to copy
     * @param auth Authentication context
     * @return AdversaryResponse containing the new copy
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<AdversaryResponse> copyAdversary(
            @PathVariable Long id,
            Authentication auth) {

        AdversaryResponse response = adversaryService.copyAdversary(id, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing adversary.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official adversaries: OWNER role only</li>
     *   <li>Non-official adversaries: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The adversary ID to update
     * @param request The update request containing new adversary details
     * @param auth Authentication context
     * @return AdversaryResponse containing the updated adversary
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdversaryResponse> updateAdversary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdversaryRequest request,
            Authentication auth) {

        AdversaryResponse response = adversaryService.updateAdversary(id, request, auth);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an adversary.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official adversaries: OWNER role only</li>
     *   <li>Non-official adversaries: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The adversary ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdversary(
            @PathVariable Long id,
            Authentication auth) {

        adversaryService.deleteAdversary(id, auth);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted adversary.
     * Requires ADMIN or OWNER role.
     *
     * @param id The adversary ID to restore
     * @param auth Authentication context
     * @return AdversaryResponse containing the restored adversary
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<AdversaryResponse> restoreAdversary(
            @PathVariable Long id,
            Authentication auth) {

        AdversaryResponse response = adversaryService.restoreAdversary(id, auth);
        return ResponseEntity.ok(response);
    }
}
