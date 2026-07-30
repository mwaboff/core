package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateConditionRequest;
import com.aboff.core.model.dto.dh.response.ConditionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.ConditionService;
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
 * REST controller for managing Condition resources.
 * Provides endpoints for CRUD operations on Daggerheart conditions (e.g., Restrained, Vulnerable).
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role, matching the Weapon/Armor/Loot/Beastform
 * catalog-content pattern — conditions are bulk-imported rulebook content.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionService conditionService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of conditions.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted conditions (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return Paginated response containing conditions
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ConditionResponse>> getAllConditions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String expand) {

        PagedResponse<ConditionResponse> response = conditionService.getAllConditions(
                page, size, includeDeleted, expansionId, isOfficial, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single condition by ID.
     *
     * @param id The condition ID
     * @param expand Comma-separated list of relationships to expand
     * @return ConditionResponse containing the condition details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConditionResponse> getConditionById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        ConditionResponse response = conditionService.getConditionById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new condition.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing condition details
     * @param auth Authentication context
     * @return ConditionResponse containing the created condition
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ConditionResponse> createCondition(
            @Valid @RequestBody CreateConditionRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/conditions");

        ConditionResponse response = conditionService.createCondition(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/conditions", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple conditions in a bulk operation.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests containing condition details
     * @param auth Authentication context
     * @return List of created condition responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ConditionResponse>> createConditionsBulk(
            @Valid @RequestBody List<CreateConditionRequest> requests,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/conditions/bulk");

        List<ConditionResponse> responses = conditionService.createConditionsBulk(requests, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/conditions/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing condition.
     * Requires ADMIN or OWNER role.
     *
     * @param id The condition ID to update
     * @param request The update request containing new condition details
     * @param auth Authentication context
     * @return ConditionResponse containing the updated condition
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ConditionResponse> updateCondition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConditionRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/conditions/" + id);

        ConditionResponse response = conditionService.updateCondition(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/conditions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a condition.
     * Requires ADMIN or OWNER role.
     *
     * @param id The condition ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteCondition(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/conditions/" + id);

        conditionService.deleteCondition(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/conditions/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted condition.
     * Requires ADMIN or OWNER role.
     *
     * @param id The condition ID to restore
     * @param auth Authentication context
     * @return ConditionResponse containing the restored condition
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ConditionResponse> restoreCondition(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/conditions/" + id + "/restore");

        ConditionResponse response = conditionService.restoreCondition(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/conditions/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
