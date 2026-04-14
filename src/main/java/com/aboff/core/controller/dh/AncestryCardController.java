package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.CreateMixedAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.dto.dh.response.AncestryCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.AncestryCardService;
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
 * REST controller for managing AncestryCard resources.
 * Provides endpoints for CRUD operations on Daggerheart ancestry cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/ancestry")
@RequiredArgsConstructor
public class AncestryCardController {

    private final AncestryCardService ancestryCardService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of ancestry cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isMixed Optional filter for mixed ancestry status (defaults to false, hiding mixed ancestries)
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return Paginated response containing ancestry cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AncestryCardResponse>> getAllAncestryCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Boolean isMixed,
            @RequestParam(required = false) String expand) {

        PagedResponse<AncestryCardResponse> response = ancestryCardService.getAllAncestryCards(
                page, size, includeDeleted, expansionId, isOfficial, isMixed, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single ancestry card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return AncestryCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<AncestryCardResponse> getAncestryCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/cards/ancestry/" + id);

        AncestryCardResponse response = ancestryCardService.getAncestryCardById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/cards/ancestry/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new ancestry card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return AncestryCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<AncestryCardResponse> createAncestryCard(
            @Valid @RequestBody CreateAncestryCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/ancestry");

        AncestryCardResponse response = ancestryCardService.createAncestryCard(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/ancestry", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple ancestry cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<AncestryCardResponse>> createAncestryCardsBulk(
            @Valid @RequestBody List<CreateAncestryCardRequest> requests,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/ancestry/bulk");

        List<AncestryCardResponse> responses = ancestryCardService.createAncestryCardsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/ancestry/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Creates a mixed ancestry card combining features from two different ancestries.
     * Any authenticated user can create mixed ancestry cards.
     *
     * @param request The creation request containing mixed ancestry details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return AncestryCardResponse containing the created mixed ancestry card
     */
    @PostMapping("/mixed")
    public ResponseEntity<AncestryCardResponse> createMixedAncestryCard(
            @Valid @RequestBody CreateMixedAncestryCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/ancestry/mixed");

        AncestryCardResponse response = ancestryCardService.createMixedAncestryCard(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/ancestry/mixed", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing ancestry card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return AncestryCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<AncestryCardResponse> updateAncestryCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAncestryCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/cards/ancestry/" + id);

        AncestryCardResponse response = ancestryCardService.updateAncestryCard(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/cards/ancestry/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an ancestry card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteAncestryCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/cards/ancestry/" + id);

        ancestryCardService.deleteAncestryCard(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/cards/ancestry/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted ancestry card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return AncestryCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<AncestryCardResponse> restoreAncestryCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/ancestry/" + id + "/restore");

        AncestryCardResponse response = ancestryCardService.restoreAncestryCard(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/ancestry/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
