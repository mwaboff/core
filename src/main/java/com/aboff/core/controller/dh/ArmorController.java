package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.response.PagedResponse;
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
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
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
     * @param includeDeleted Whether to include soft-deleted armors (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for armor tier (1–4)
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
            @RequestParam(required = false) String expand) {

        PagedResponse<ArmorResponse> response = armorService.getAllArmors(
                page, size, includeDeleted, expansionId, isOfficial, tier, expand);

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
     * Updates an existing armor.
     * Requires ADMIN or OWNER role.
     *
     * @param id The armor ID to update
     * @param request The update request containing new armor details
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the updated armor
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
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
