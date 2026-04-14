package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateFeatureModifierRequest;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.FeatureModifierService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing FeatureModifier resources.
 * <p>
 * Provides endpoints for CRUD operations on feature modifiers, which represent
 * structured, machine-readable stat modifications (e.g., +1 Strength, -1 Evasion)
 * that can be associated with Features.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/feature-modifiers")
@RequiredArgsConstructor
public class FeatureModifierController {

    private final FeatureModifierService featureModifierService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of feature modifiers.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted modifiers (default: false, ADMIN+ only)
     * @return Paginated response containing feature modifiers
     */
    @GetMapping
    public ResponseEntity<PagedResponse<FeatureModifierResponse>> getAllModifiers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        PagedResponse<FeatureModifierResponse> response = featureModifierService.getAllModifiers(
                page, size, includeDeleted);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single feature modifier by ID.
     *
     * @param id The feature modifier ID
     * @return FeatureModifierResponse containing the modifier details
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeatureModifierResponse> getModifierById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/feature-modifiers/" + id);

        FeatureModifierResponse response = featureModifierService.getModifier(id);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/feature-modifiers/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new feature modifier.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing modifier details
     * @return FeatureModifierResponse containing the created modifier
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FeatureModifierResponse> createModifier(
            @Valid @RequestBody CreateFeatureModifierRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/feature-modifiers");

        FeatureModifierResponse response = featureModifierService.createModifier(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/feature-modifiers", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Soft deletes a feature modifier.
     * Requires ADMIN or OWNER role.
     *
     * @param id The feature modifier ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteModifier(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/feature-modifiers/" + id);

        featureModifierService.deleteModifier(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/feature-modifiers/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted feature modifier.
     * Requires ADMIN or OWNER role.
     *
     * @param id The feature modifier ID to restore
     * @return FeatureModifierResponse containing the restored modifier
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FeatureModifierResponse> restoreModifier(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/feature-modifiers/" + id + "/restore");

        FeatureModifierResponse response = featureModifierService.restoreModifier(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/feature-modifiers/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
