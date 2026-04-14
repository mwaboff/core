package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.WeaponService;
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
 * REST controller for managing Weapon resources.
 * Provides endpoints for CRUD operations on Daggerheart weapons.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/weapons")
@RequiredArgsConstructor
public class WeaponController {

    private final WeaponService weaponService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of weapons.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted weapons (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param tier Optional filter for weapon tier (1–4)
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC)
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,feature,originalWeapon")
     * @return Paginated response containing weapons
     */
    @GetMapping
    public ResponseEntity<PagedResponse<WeaponResponse>> getAllWeapons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Trait trait,
            @RequestParam(required = false) Range range,
            @RequestParam(required = false) Burden burden,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) DamageType damageType,
            @RequestParam(required = false) String expand) {

        PagedResponse<WeaponResponse> response = weaponService.getAllWeapons(
                page, size, includeDeleted, expansionId, isOfficial, trait, range, burden, isPrimary, tier, damageType, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single weapon by ID.
     *
     * @param id The weapon ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,feature,originalWeapon")
     * @param authentication The authentication of the current user
     * @param httpRequest The HTTP servlet request
     * @return WeaponResponse containing the weapon details
     */
    @GetMapping("/{id}")
    public ResponseEntity<WeaponResponse> getWeaponById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/weapons/" + id);

        WeaponResponse response = weaponService.getWeaponById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/weapons/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new weapon.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing weapon details
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the created weapon
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<WeaponResponse> createWeapon(
            @Valid @RequestBody CreateWeaponRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/weapons");

        WeaponResponse response = weaponService.createWeapon(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/weapons", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple weapons in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created weapon responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<WeaponResponse>> createWeaponsBulk(
            @Valid @RequestBody List<CreateWeaponRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/weapons/bulk");

        List<WeaponResponse> responses = weaponService.createWeaponsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/weapons/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing weapon.
     * Requires ADMIN or OWNER role.
     *
     * @param id The weapon ID to update
     * @param request The update request containing new weapon details
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the updated weapon
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<WeaponResponse> updateWeapon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWeaponRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/weapons/" + id);

        WeaponResponse response = weaponService.updateWeapon(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/weapons/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a weapon.
     * Requires ADMIN or OWNER role.
     *
     * @param id The weapon ID to delete
     * @param authentication The authentication of the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteWeapon(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/weapons/" + id);

        weaponService.deleteWeapon(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/weapons/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted weapon.
     * Requires ADMIN or OWNER role.
     *
     * @param id The weapon ID to restore
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the restored weapon
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<WeaponResponse> restoreWeapon(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/weapons/" + id + "/restore");

        WeaponResponse response = weaponService.restoreWeapon(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/weapons/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
