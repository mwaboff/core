package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCustomArmorRequest;
import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.ItemSort;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.ArmorService;
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
 * REST controller for managing Armor resources.
 * Provides endpoints for CRUD operations on Daggerheart armor.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * Bulk and admin create endpoints require ADMIN or OWNER. Custom creation and copying are
 * open to any authenticated user, and update/delete/restore are ownership-aware and
 * enforced in the service.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/armors")
@RequiredArgsConstructor
public class ArmorController {

    private final ArmorService armorService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of armors.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted armors (default: false, MODERATOR+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for armor tier (1–4)
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param sort Ordering: ID (default), NAME, TIER, or NEWEST
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,feature,originalArmor")
     * @return Paginated response containing armors
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ArmorResponse>> getAllArmors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Long createdByUserId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ItemSort sort,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        PagedResponse<ArmorResponse> response = armorService.getAllArmors(
                page, size, includeDeleted, expansionId, isOfficial, tier, createdByUserId, name, sort, expand, authentication);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single armor by ID.
     *
     * @param id The armor ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,feature,originalArmor")
     * @return ArmorResponse containing the armor details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArmorResponse> getArmorById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/armors/" + id);

        ArmorResponse response = armorService.getArmorById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/armors/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new armor.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing armor details
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the created armor
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ArmorResponse> createArmor(
            @Valid @RequestBody CreateArmorRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/armors");

        ArmorResponse response = armorService.createArmor(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/armors", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple armors in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created armor responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ArmorResponse>> createArmorsBulk(
            @Valid @RequestBody List<CreateArmorRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/armors/bulk");

        List<ArmorResponse> responses = armorService.createArmorsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/armors/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }


    /**
     * Creates an armor record authored by the calling user.
     * <p>
     * Open to any authenticated user, unlike {@code POST /api/dh/armors}, which remains the admin
     * import path and takes the stricter request type. Ownership and the official, public, and
     * expansion fields are all resolved server-side, so there is nothing here for a caller to
     * escalate through.
     * </p>
     *
     * @param request The custom armor details
     * @param authentication The current authentication
     * @param httpRequest The HTTP request, used for audit context
     * @return The created record with 201 status
     */
    @PostMapping("/custom")
    public ResponseEntity<ArmorResponse> createCustomArmor(
            @Valid @RequestBody CreateCustomArmorRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/armors/custom");

        ArmorResponse response = armorService.createCustomArmor(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/armors/custom", startTime);
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
    public ResponseEntity<ArmorResponse> copyArmor(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/armors/" + id + "/copy");

        ArmorResponse response = armorService.copyArmor(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/armors/" + id + "/copy", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing armor.
     * Requires ADMIN or OWNER role.
     *
     * @param id The armor ID to update
     * @param request The update request containing new armor details
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the updated armor
     */
    @PutMapping("/{id}")
    // Authorisation is ownership-aware (author, or moderator, or admin for official
    // content), so it lives in the service rather than in a flat role gate here.
    public ResponseEntity<ArmorResponse> updateArmor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArmorRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/armors/" + id);

        ArmorResponse response = armorService.updateArmor(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/armors/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an armor.
     * Requires ADMIN or OWNER role.
     *
     * @param id The armor ID to delete
     * @param authentication The authentication of the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    // Authorisation is ownership-aware; see the service.
    public ResponseEntity<Void> deleteArmor(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/armors/" + id);

        armorService.deleteArmor(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/armors/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted armor.
     * Requires ADMIN or OWNER role.
     *
     * @param id The armor ID to restore
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the restored armor
     */
    @PostMapping("/{id}/restore")
    // Authorisation is ownership-aware; an author must be able to undo their own delete.
    public ResponseEntity<ArmorResponse> restoreArmor(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/armors/" + id + "/restore");

        ArmorResponse response = armorService.restoreArmor(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/armors/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
