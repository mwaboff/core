package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
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
 * Access control:
 * </p>
 * <ul>
 *   <li>GET endpoints: All authenticated users</li>
 *   <li>POST (single): All authenticated users</li>
 *   <li>POST (bulk): ADMIN/OWNER only</li>
 *   <li>PUT: Permission check in service (creator OR moderator+ for non-official,
 *       ADMIN+ only for official)</li>
 *   <li>DELETE/restore: ADMIN/OWNER only</li>
 * </ul>
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
     * @param includeDeleted Whether to include soft-deleted loot (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for loot tier (1–4)
     * @param isConsumable Optional filter for consumable status
     * @param creatorId Optional filter for the creator's user ID
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
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String expand) {

        PagedResponse<LootResponse> response = lootService.getAllLoot(
                page, size, includeDeleted, expansionId, isOfficial, tier, isConsumable, creatorId, expand);

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
     * Any authenticated user may create a custom loot item; only ADMIN+ may mark it official.
     *
     * @param request The creation request containing loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the created loot
     */
    @PostMapping
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
     * Updates an existing loot item.
     * Permission is enforced in the service layer: the creator or a MODERATOR+ may
     * modify a custom loot item; only ADMIN+ may modify an official loot item.
     *
     * @param id The loot ID to update
     * @param request The update request containing new loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the updated loot
     */
    @PutMapping("/{id}")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
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
