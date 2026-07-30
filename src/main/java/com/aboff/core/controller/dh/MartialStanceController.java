package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateMartialStanceRequest;
import com.aboff.core.model.dto.dh.request.UpdateMartialStanceRequest;
import com.aboff.core.model.dto.dh.response.MartialStanceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.MartialStanceService;
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
 * REST controller for managing MartialStance resources.
 * Provides endpoints for CRUD operations on Daggerheart martial stances (Hope & Fear's
 * "Stance Fighter" modal combat states).
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/martial-stances")
@RequiredArgsConstructor
public class MartialStanceController {

    private final MartialStanceService martialStanceService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of martial stances.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted martial stances (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for martial stance tier (1–4)
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalMartialStance")
     * @return Paginated response containing martial stances
     */
    @GetMapping
    public ResponseEntity<PagedResponse<MartialStanceResponse>> getAllMartialStances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) String expand) {

        PagedResponse<MartialStanceResponse> response = martialStanceService.getAllMartialStances(
                page, size, includeDeleted, expansionId, isOfficial, tier, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single martial stance by ID.
     *
     * @param id The martial stance ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalMartialStance")
     * @return MartialStanceResponse containing the martial stance details
     */
    @GetMapping("/{id}")
    public ResponseEntity<MartialStanceResponse> getMartialStanceById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/martial-stances/" + id);

        MartialStanceResponse response = martialStanceService.getMartialStanceById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/martial-stances/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new martial stance.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing martial stance details
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the created martial stance
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<MartialStanceResponse> createMartialStance(
            @Valid @RequestBody CreateMartialStanceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/martial-stances");

        MartialStanceResponse response = martialStanceService.createMartialStance(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/martial-stances", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple martial stances in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created martial stance responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<MartialStanceResponse>> createMartialStanceBulk(
            @Valid @RequestBody List<CreateMartialStanceRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/martial-stances/bulk");

        List<MartialStanceResponse> responses = martialStanceService.createMartialStanceBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/martial-stances/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing martial stance.
     * Requires ADMIN or OWNER role.
     *
     * @param id The martial stance ID to update
     * @param request The update request containing new martial stance details
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the updated martial stance
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<MartialStanceResponse> updateMartialStance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMartialStanceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/martial-stances/" + id);

        MartialStanceResponse response = martialStanceService.updateMartialStance(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/martial-stances/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a martial stance.
     * Requires ADMIN or OWNER role.
     *
     * @param id The martial stance ID to delete
     * @param authentication The authentication of the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteMartialStance(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/martial-stances/" + id);

        martialStanceService.deleteMartialStance(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/martial-stances/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted martial stance.
     * Requires ADMIN or OWNER role.
     *
     * @param id The martial stance ID to restore
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the restored martial stance
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<MartialStanceResponse> restoreMartialStance(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/martial-stances/" + id + "/restore");

        MartialStanceResponse response = martialStanceService.restoreMartialStance(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/martial-stances/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
