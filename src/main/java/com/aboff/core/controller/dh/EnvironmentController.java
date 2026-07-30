package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateEnvironmentRequest;
import com.aboff.core.model.dto.dh.request.UpdateEnvironmentRequest;
import com.aboff.core.model.dto.dh.response.EnvironmentResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing Environment resources.
 * Provides endpoints for CRUD operations on Daggerheart environments (GM-facing
 * scene stat blocks -- never selected or equipped by a player).
 * <p>
 * Access control:
 * </p>
 * <ul>
 *   <li>GET endpoints: All authenticated users (filtered by visibility)</li>
 *   <li>POST (single): All authenticated users</li>
 *   <li>POST (batch): MODERATOR+ only</li>
 *   <li>PUT/DELETE: Permission check in service (creator OR moderator+ for non-official,
 *       OWNER only for official)</li>
 *   <li>POST restore: ADMIN/OWNER only</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dh/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of environments.
     * Returns environments that are official, public, or created by the authenticated user.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted environments (ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param environmentType Optional filter for environment type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param expand Comma-separated list of relationships to expand
     *               (expansion, creator, features)
     * @param auth Authentication context
     * @return Paginated response containing environments
     */
    @GetMapping
    public ResponseEntity<PagedResponse<EnvironmentResponse>> getAllEnvironments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) EnvironmentType environmentType,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String expand,
            Authentication auth) {

        PagedResponse<EnvironmentResponse> response = environmentService.getAllEnvironments(
                page, size, includeDeleted, expansionId, tier, environmentType,
                isOfficial, name, expand, auth);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single environment by ID.
     * Access is restricted to official, public, or user's own environments.
     *
     * @param id The environment ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return EnvironmentResponse containing the environment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> getEnvironmentById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/environments/" + id);

        EnvironmentResponse response = environmentService.getEnvironmentById(id, expand, auth);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/environments/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new environment.
     * The authenticated user becomes the creator.
     *
     * @param request The creation request containing environment details
     * @param auth Authentication context
     * @return EnvironmentResponse containing the created environment
     */
    @PostMapping
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @Valid @RequestBody CreateEnvironmentRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/environments");

        EnvironmentResponse response = environmentService.createEnvironment(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/environments", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple environments in a bulk operation.
     * Requires MODERATOR, ADMIN, or OWNER role -- this is the endpoint rulebook
     * content import uses.
     *
     * @param requests List of creation requests containing environment details
     * @param auth Authentication context
     * @return List of created environment responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'OWNER')")
    public ResponseEntity<List<EnvironmentResponse>> createEnvironmentsBulk(
            @Valid @RequestBody List<CreateEnvironmentRequest> requests,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/environments/bulk");

        List<EnvironmentResponse> responses = environmentService.createEnvironmentsBulk(requests, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/environments/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing environment.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official environments: OWNER role only</li>
     *   <li>Non-official environments: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The environment ID to update
     * @param request The update request containing new environment details
     * @param auth Authentication context
     * @return EnvironmentResponse containing the updated environment
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnvironmentRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/environments/" + id);

        EnvironmentResponse response = environmentService.updateEnvironment(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/environments/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an environment.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official environments: OWNER role only</li>
     *   <li>Non-official environments: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The environment ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/environments/" + id);

        environmentService.deleteEnvironment(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/environments/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted environment.
     * Requires ADMIN or OWNER role.
     *
     * @param id The environment ID to restore
     * @param auth Authentication context
     * @return EnvironmentResponse containing the restored environment
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<EnvironmentResponse> restoreEnvironment(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/environments/" + id + "/restore");

        EnvironmentResponse response = environmentService.restoreEnvironment(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/environments/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
