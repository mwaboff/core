package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.service.dh.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Retrieves a paginated list of features.
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
     *
     * @param id The feature ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return FeatureResponse containing the feature details
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeatureResponse> getFeatureById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        FeatureResponse response = featureService.getFeatureById(id, expand);
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
            @Valid @RequestBody CreateFeatureRequest request) {

        FeatureResponse response = featureService.createFeature(request);
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
            @Valid @RequestBody List<CreateFeatureRequest> requests) {

        List<FeatureResponse> responses = featureService.createFeaturesBulk(requests);
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
            @Valid @RequestBody UpdateFeatureRequest request) {

        FeatureResponse response = featureService.updateFeature(id, request);
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
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
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
    public ResponseEntity<FeatureResponse> restoreFeature(@PathVariable Long id) {
        FeatureResponse response = featureService.restoreFeature(id);
        return ResponseEntity.ok(response);
    }
}
