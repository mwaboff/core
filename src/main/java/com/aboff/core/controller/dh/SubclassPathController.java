package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateSubclassPathRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassPathRequest;
import com.aboff.core.model.dto.dh.response.SubclassPathResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.SubclassPathService;
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
 * REST controller for managing SubclassPath resources.
 * Provides endpoints for CRUD operations on Daggerheart subclass paths.
 * <p>
 * Subclass paths group three subclass cards (Foundation, Specialization, Mastery)
 * that share a common theme within a class. Each path has associated domains and
 * an optional spellcasting trait.
 * </p>
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/subclass-paths")
@RequiredArgsConstructor
public class SubclassPathController {

    private final SubclassPathService subclassPathService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of subclass paths.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted paths (default: false, ADMIN+ only)
     * @param classId Optional filter for associated class ID
     * @param expand Comma-separated list of relationships to expand
     *               (e.g., "associatedClass,associatedDomains,expansion")
     * @return Paginated response containing subclass paths
     */
    @GetMapping
    public ResponseEntity<PagedResponse<SubclassPathResponse>> getAllSubclassPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String expand) {

        PagedResponse<SubclassPathResponse> response = subclassPathService.getAllSubclassPaths(
                page, size, includeDeleted, classId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single subclass path by ID.
     *
     * @param id The subclass path ID
     * @param expand Comma-separated list of relationships to expand
     *               (e.g., "associatedClass,associatedDomains,expansion")
     * @return SubclassPathResponse containing the path details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubclassPathResponse> getSubclassPathById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/subclass-paths/" + id);

        SubclassPathResponse response = subclassPathService.getSubclassPathById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/subclass-paths/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new subclass path.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing path details
     * @return SubclassPathResponse containing the created path
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassPathResponse> createSubclassPath(
            @Valid @RequestBody CreateSubclassPathRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/subclass-paths");

        SubclassPathResponse response = subclassPathService.createSubclassPath(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/subclass-paths", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple subclass paths in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created subclass path responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<SubclassPathResponse>> createSubclassPathsBulk(
            @Valid @RequestBody List<CreateSubclassPathRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/subclass-paths/bulk");

        List<SubclassPathResponse> responses = subclassPathService.createSubclassPathsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/subclass-paths/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing subclass path.
     * Requires ADMIN or OWNER role.
     *
     * @param id The subclass path ID to update
     * @param request The update request containing new path details
     * @return SubclassPathResponse containing the updated path
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassPathResponse> updateSubclassPath(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubclassPathRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/subclass-paths/" + id);

        SubclassPathResponse response = subclassPathService.updateSubclassPath(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/subclass-paths/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a subclass path.
     * Requires ADMIN or OWNER role.
     *
     * @param id The subclass path ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteSubclassPath(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/subclass-paths/" + id);

        subclassPathService.deleteSubclassPath(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/subclass-paths/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted subclass path.
     * Requires ADMIN or OWNER role.
     *
     * @param id The subclass path ID to restore
     * @return SubclassPathResponse containing the restored path
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassPathResponse> restoreSubclassPath(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/subclass-paths/" + id + "/restore");

        SubclassPathResponse response = subclassPathService.restoreSubclassPath(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/subclass-paths/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
