package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateBeastformRequest;
import com.aboff.core.model.dto.dh.request.UpdateBeastformRequest;
import com.aboff.core.model.dto.dh.response.BeastformResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.BeastformService;
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
 * REST controller for managing Beastform resources.
 * Provides endpoints for CRUD operations on Daggerheart beastform stat blocks.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role, matching the Weapon/Armor/Loot
 * catalog-content pattern — beastform stat blocks are bulk-imported rulebook content.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/beastforms")
@RequiredArgsConstructor
public class BeastformController {

    private final BeastformService beastformService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of beastforms.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted beastforms (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isPublic Optional filter for public visibility
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,originalBeastform")
     * @return Paginated response containing beastforms
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BeastformResponse>> getAllBeastforms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String expand) {

        PagedResponse<BeastformResponse> response = beastformService.getAllBeastforms(
                page, size, includeDeleted, expansionId, isOfficial, isPublic, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single beastform by ID.
     *
     * @param id The beastform ID
     * @param expand Comma-separated list of relationships to expand
     * @return BeastformResponse containing the beastform details
     */
    @GetMapping("/{id}")
    public ResponseEntity<BeastformResponse> getBeastformById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        BeastformResponse response = beastformService.getBeastformById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new beastform.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing beastform details
     * @param auth Authentication context
     * @return BeastformResponse containing the created beastform
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<BeastformResponse> createBeastform(
            @Valid @RequestBody CreateBeastformRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/beastforms");

        BeastformResponse response = beastformService.createBeastform(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/beastforms", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple beastforms in a bulk operation.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests containing beastform details
     * @param auth Authentication context
     * @return List of created beastform responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<BeastformResponse>> createBeastformsBulk(
            @Valid @RequestBody List<CreateBeastformRequest> requests,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/beastforms/bulk");

        List<BeastformResponse> responses = beastformService.createBeastformsBulk(requests, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/beastforms/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing beastform.
     * Requires ADMIN or OWNER role.
     *
     * @param id The beastform ID to update
     * @param request The update request containing new beastform details
     * @param auth Authentication context
     * @return BeastformResponse containing the updated beastform
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<BeastformResponse> updateBeastform(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBeastformRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/beastforms/" + id);

        BeastformResponse response = beastformService.updateBeastform(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/beastforms/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a beastform.
     * Requires ADMIN or OWNER role.
     *
     * @param id The beastform ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteBeastform(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/beastforms/" + id);

        beastformService.deleteBeastform(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/beastforms/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted beastform.
     * Requires ADMIN or OWNER role.
     *
     * @param id The beastform ID to restore
     * @param auth Authentication context
     * @return BeastformResponse containing the restored beastform
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<BeastformResponse> restoreBeastform(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/beastforms/" + id + "/restore");

        BeastformResponse response = beastformService.restoreBeastform(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/beastforms/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
