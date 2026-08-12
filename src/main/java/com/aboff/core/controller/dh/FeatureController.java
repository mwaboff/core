package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.FeatureService;
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
 * REST controller for managing Feature resources.
 * Provides endpoints for CRUD operations on Daggerheart features.
 */
@RestController
@RequestMapping("/api/dh/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of features.
     * <p>
     * Restricted to MODERATOR or higher. A {@link com.aboff.core.model.entity.dh.Feature}'s
     * {@code srd}/{@code isOfficial} flags are stamped once at creation and are never re-derived
     * when the parent that granted them (a class, card, item, adversary, ...) is later re-flagged
     * -- a feature is shared M:N across many parent types and has no single owner to inherit from.
     * That means this endpoint's own SRD gating predicate can only ever reflect a feature's
     * possibly-stale row, not any parent's current, correct gating. Restricting the standalone
     * browse surface to MODERATOR+ keeps that staleness from being reachable by users who would
     * otherwise see it as a bypass of a parent's gating; MODERATOR is deliberately included here
     * even though moderators do not get the broader non-SRD content grant elsewhere in this
     * feature -- this is about who may use the standalone browse surface at all, not about which
     * content they may see once granted access to it.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted features (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param featureType Optional filter for feature type
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return Paginated response containing features
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'OWNER')")
    public ResponseEntity<PagedResponse<FeatureResponse>> getAllFeatures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) FeatureType featureType,
            @RequestParam(required = false) String expand) {

        PagedResponse<FeatureResponse> response = featureService.getAllFeatures(
                page, size, includeDeleted, expansionId, featureType, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single feature by ID.
     * <p>
     * Restricted to MODERATOR or higher, for the same reason as {@link #getAllFeatures}: a
     * feature's own {@code srd}/{@code isOfficial} flags can go stale relative to the parent that
     * granted them, and this endpoint has no parent to re-derive from, so it cannot be trusted to
     * reflect a parent's current gating on its own.
     *
     * @param id The feature ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return FeatureResponse containing the feature details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'OWNER')")
    public ResponseEntity<FeatureResponse> getFeatureById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/features/" + id);

        FeatureResponse response = featureService.getFeatureById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/features/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new feature.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing feature details
     * @return FeatureResponse containing the created feature
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FeatureResponse> createFeature(
            @Valid @RequestBody CreateFeatureRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/features");

        FeatureResponse response = featureService.createFeature(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/features", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple features in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests containing feature details
     * @return List of created feature responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<FeatureResponse>> createFeaturesBulk(
            @Valid @RequestBody List<CreateFeatureRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/features/bulk");

        List<FeatureResponse> responses = featureService.createFeaturesBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/features/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing feature.
     * Requires ADMIN or OWNER role.
     *
     * @param id The feature ID to update
     * @param request The update request containing new feature details
     * @return FeatureResponse containing the updated feature
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FeatureResponse> updateFeature(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeatureRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/features/" + id);

        FeatureResponse response = featureService.updateFeature(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/features/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a feature.
     * Requires ADMIN or OWNER role.
     *
     * @param id The feature ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteFeature(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/features/" + id);

        featureService.deleteFeature(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/features/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted feature.
     * Requires ADMIN or OWNER role.
     *
     * @param id The feature ID to restore
     * @return FeatureResponse containing the restored feature
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FeatureResponse> restoreFeature(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/features/" + id + "/restore");

        FeatureResponse response = featureService.restoreFeature(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/features/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
