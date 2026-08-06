package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCustomLootRequest;
import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.ItemSort;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.LootService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Loot resources.
 * Provides endpoints for CRUD operations on Daggerheart loot items.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * Bulk and admin create endpoints require ADMIN or OWNER. Custom creation and copying are
 * open to any authenticated user, and update/delete/restore are ownership-aware and
 * enforced in the service.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/loot")
@RequiredArgsConstructor
public class LootController {

    private final LootService lootService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of loot items.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted loot (default: false, MODERATOR+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for loot tier (1–4)
     * @param isConsumable Optional filter for consumable status
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param sort Ordering: ID (default), NAME, TIER, or NEWEST
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalLoot")
     * @return Paginated response containing loot items
     */
    @GetMapping
    public ResponseEntity<PagedResponse<LootResponse>> getAllLoot(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Boolean isConsumable,
            @RequestParam(required = false) Long createdByUserId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ItemSort sort,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        PagedResponse<LootResponse> response = lootService.getAllLoot(
                page, size, includeDeleted, expansionId, isOfficial, tier, isConsumable, createdByUserId, name, sort, expand, authentication);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single loot item by ID.
     *
     * @param id The loot ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalLoot")
     * @return LootResponse containing the loot details
     */
    @GetMapping("/{id}")
    public ResponseEntity<LootResponse> getLootById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/loot/" + id);

        LootResponse response = lootService.getLootById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/loot/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the created loot
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<LootResponse> createLoot(
            @Valid @RequestBody CreateLootRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/loot");

        LootResponse response = lootService.createLoot(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/loot", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple loot items in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created loot responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<LootResponse>> createLootBulk(
            @Valid @RequestBody List<CreateLootRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/loot/bulk");

        List<LootResponse> responses = lootService.createLootBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/loot/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }


    /**
     * Creates a loot record authored by the calling user.
     * <p>
     * Open to any authenticated user, unlike {@code POST /api/dh/loot}, which remains the admin
     * import path and takes the stricter request type. Ownership and the official, public, and
     * expansion fields are all resolved server-side, so there is nothing here for a caller to
     * escalate through.
     * </p>
     *
     * @param request The custom loot details
     * @param authentication The current authentication
     * @param httpRequest The HTTP request, used for audit context
     * @return The created record with 201 status
     */
    @PostMapping("/custom")
    public ResponseEntity<LootResponse> createCustomLoot(
            @Valid @RequestBody CreateCustomLootRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/loot/custom");

        LootResponse response = lootService.createCustomLoot(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/loot/custom", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Copies an existing record into new custom content owned by the calling user.
     * <p>
     * Any authenticated user may copy anything, official content included. Nothing is protected
     * by restricting this: records are already readable by id, and the copy is created private
     * and unofficial regardless of its source.
     * </p>
     *
     * @param id The ID of the record to copy
     * @param authentication The current authentication
     * @param httpRequest The HTTP request, used for audit context
     * @return The newly created copy with 201 status
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<LootResponse> copyLoot(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/loot/" + id + "/copy");

        LootResponse response = lootService.copyLoot(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/loot/" + id + "/copy", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to update
     * @param request The update request containing new loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the updated loot
     */
    @PutMapping("/{id}")
    // Authorisation is ownership-aware (author, or moderator, or admin for official
    // content), so it lives in the service rather than in a flat role gate here.
    public ResponseEntity<LootResponse> updateLoot(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLootRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/loot/" + id);

        LootResponse response = lootService.updateLoot(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/loot/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to delete
     * @param authentication The authentication of the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    // Authorisation is ownership-aware; see the service.
    public ResponseEntity<Void> deleteLoot(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/loot/" + id);

        lootService.deleteLoot(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/loot/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to restore
     * @param authentication The authentication of the current user
     * @return LootResponse containing the restored loot
     */
    @PostMapping("/{id}/restore")
    // Authorisation is ownership-aware; an author must be able to undo their own delete.
    public ResponseEntity<LootResponse> restoreLoot(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/loot/" + id + "/restore");

        LootResponse response = lootService.restoreLoot(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/loot/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
