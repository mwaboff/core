package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateClassRequest;
import com.aboff.core.model.dto.dh.request.UpdateClassRequest;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Class resources.
 * Provides endpoints for CRUD operations on Daggerheart character classes.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * Retrieves a paginated list of classes.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted classes (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,associatedDomains")
     * @return Paginated response containing classes
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ClassResponse>> getAllClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) String expand) {

        PagedResponse<ClassResponse> response = classService.getAllClasses(
                page, size, includeDeleted, expansionId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single class by ID.
     *
     * @param id The class ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,associatedDomains")
     * @return ClassResponse containing the class details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getClassById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        ClassResponse response = classService.getClassById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new class.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing class details
     * @return ClassResponse containing the created class
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ClassResponse> createClass(
            @Valid @RequestBody CreateClassRequest request) {

        ClassResponse response = classService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple classes in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created class responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ClassResponse>> createClassesBulk(
            @Valid @RequestBody List<CreateClassRequest> requests) {

        List<ClassResponse> responses = classService.createClassesBulk(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing class.
     * Requires ADMIN or OWNER role.
     *
     * @param id The class ID to update
     * @param request The update request containing new class details
     * @return ClassResponse containing the updated class
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {

        ClassResponse response = classService.updateClass(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a class.
     * Requires ADMIN or OWNER role.
     *
     * @param id The class ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted class.
     * Requires ADMIN or OWNER role.
     *
     * @param id The class ID to restore
     * @return ClassResponse containing the restored class
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ClassResponse> restoreClass(@PathVariable Long id) {
        ClassResponse response = classService.restoreClass(id);
        return ResponseEntity.ok(response);
    }
}
