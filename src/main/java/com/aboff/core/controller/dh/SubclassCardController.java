package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.SubclassCardService;
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
 * REST controller for managing SubclassCard resources.
 * Provides endpoints for CRUD operations on Daggerheart subclass cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/subclass")
@RequiredArgsConstructor
public class SubclassCardController {

    private final SubclassCardService subclassCardService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of subclass cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID (via subclass path)
     * @param subclassPathId Optional filter for subclass path ID
     * @param level Optional filter for subclass level
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,subclassPath")
     * @return Paginated response containing subclass cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<SubclassCardResponse>> getAllSubclassCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Long associatedClassId,
            @RequestParam(required = false) Long subclassPathId,
            @RequestParam(required = false) SubclassLevel level,
            @RequestParam(required = false) String expand) {

        PagedResponse<SubclassCardResponse> response = subclassCardService.getAllSubclassCards(
                page, size, includeDeleted, expansionId, isOfficial, associatedClassId, subclassPathId, level, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single subclass card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features,subclassPath")
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return SubclassCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubclassCardResponse> getSubclassCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/cards/subclass/" + id);

        SubclassCardResponse response = subclassCardService.getSubclassCardById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/cards/subclass/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return SubclassCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> createSubclassCard(
            @Valid @RequestBody CreateSubclassCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/subclass");

        SubclassCardResponse response = subclassCardService.createSubclassCard(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/subclass", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple subclass cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<SubclassCardResponse>> createSubclassCardsBulk(
            @Valid @RequestBody List<CreateSubclassCardRequest> requests,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/subclass/bulk");

        List<SubclassCardResponse> responses = subclassCardService.createSubclassCardsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/subclass/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return SubclassCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> updateSubclassCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubclassCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/cards/subclass/" + id);

        SubclassCardResponse response = subclassCardService.updateSubclassCard(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/cards/subclass/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteSubclassCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/cards/subclass/" + id);

        subclassCardService.deleteSubclassCard(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/cards/subclass/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted subclass card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return SubclassCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SubclassCardResponse> restoreSubclassCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/subclass/" + id + "/restore");

        SubclassCardResponse response = subclassCardService.restoreSubclassCard(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/subclass/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
