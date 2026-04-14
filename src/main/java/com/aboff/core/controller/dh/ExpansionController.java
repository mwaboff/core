package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateExpansionRequest;
import com.aboff.core.model.dto.dh.request.UpdateExpansionRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.ExpansionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final AuditLogger auditLogger;

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
     * @param authentication The authentication of the current user
     * @param httpRequest The HTTP servlet request
     * @return ExpansionResponse containing the expansion details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpansionResponse> getExpansionById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/expansions/" + id);

        ExpansionResponse response = expansionService.getExpansionById(id);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/expansions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing expansion details
     * @param auth Authentication context
     * @return ExpansionResponse containing the created expansion
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> createExpansion(
            @Valid @RequestBody CreateExpansionRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/expansions");

        ExpansionResponse response = expansionService.createExpansion(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/expansions", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to update
     * @param request The update request containing new expansion details
     * @param auth Authentication context
     * @return ExpansionResponse containing the updated expansion
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> updateExpansion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpansionRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/expansions/" + id);

        ExpansionResponse response = expansionService.updateExpansion(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/expansions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteExpansion(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/expansions/" + id);

        expansionService.deleteExpansion(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/expansions/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted expansion.
     * Requires ADMIN or OWNER role.
     *
     * @param id The expansion ID to restore
     * @param auth Authentication context
     * @return ExpansionResponse containing the restored expansion
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ExpansionResponse> restoreExpansion(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/expansions/" + id + "/restore");

        ExpansionResponse response = expansionService.restoreExpansion(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/expansions/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
