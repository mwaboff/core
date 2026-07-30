package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateTransformationCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationCardRequest;
import com.aboff.core.model.dto.dh.response.TransformationCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.TransformationCardService;
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
 * REST controller for managing TransformationCard resources.
 * Provides endpoints for CRUD operations on Daggerheart transformation cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/transformation-cards")
@RequiredArgsConstructor
public class TransformationCardController {

    private final TransformationCardService transformationCardService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of transformation cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted transformation cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return Paginated response containing transformation cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<TransformationCardResponse>> getAllTransformationCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) String expand) {

        PagedResponse<TransformationCardResponse> response = transformationCardService.getAllTransformationCards(
                page, size, includeDeleted, expansionId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single transformation card by ID.
     *
     * @param id The transformation card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return TransformationCardResponse containing the transformation card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransformationCardResponse> getTransformationCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/transformation-cards/" + id);

        TransformationCardResponse response = transformationCardService.getTransformationCardById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/transformation-cards/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new transformation card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing transformation card details
     * @param auth Authentication context
     * @return TransformationCardResponse containing the created transformation card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<TransformationCardResponse> createTransformationCard(
            @Valid @RequestBody CreateTransformationCardRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/transformation-cards");

        TransformationCardResponse response = transformationCardService.createTransformationCard(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/transformation-cards", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple transformation cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param auth Authentication context
     * @return List of created transformation card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<TransformationCardResponse>> createTransformationCardsBulk(
            @Valid @RequestBody List<CreateTransformationCardRequest> requests,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/transformation-cards/bulk");

        List<TransformationCardResponse> responses =
                transformationCardService.createTransformationCardsBulk(requests, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/transformation-cards/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing transformation card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The transformation card ID to update
     * @param request The update request containing new transformation card details
     * @param auth Authentication context
     * @return TransformationCardResponse containing the updated transformation card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<TransformationCardResponse> updateTransformationCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransformationCardRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/transformation-cards/" + id);

        TransformationCardResponse response = transformationCardService.updateTransformationCard(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/transformation-cards/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a transformation card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The transformation card ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteTransformationCard(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/transformation-cards/" + id);

        transformationCardService.deleteTransformationCard(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/transformation-cards/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted transformation card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The transformation card ID to restore
     * @param auth Authentication context
     * @return TransformationCardResponse containing the restored transformation card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<TransformationCardResponse> restoreTransformationCard(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/transformation-cards/" + id + "/restore");

        TransformationCardResponse response = transformationCardService.restoreTransformationCard(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/transformation-cards/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
