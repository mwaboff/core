package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.dto.dh.response.CommunityCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.CommunityCardService;
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
 * REST controller for managing CommunityCard resources.
 * Provides endpoints for CRUD operations on Daggerheart community cards.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/cards/community")
@RequiredArgsConstructor
public class CommunityCardController {

    private final CommunityCardService communityCardService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of community cards.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted cards (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @return Paginated response containing community cards
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CommunityCardResponse>> getAllCommunityCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String expand) {

        PagedResponse<CommunityCardResponse> response = communityCardService.getAllCommunityCards(
                page, size, includeDeleted, expansionId, isOfficial, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single community card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,features")
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return CommunityCardResponse containing the card details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityCardResponse> getCommunityCardById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/cards/community/" + id);

        CommunityCardResponse response = communityCardService.getCommunityCardById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/cards/community/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new community card.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return CommunityCardResponse containing the created card
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> createCommunityCard(
            @Valid @RequestBody CreateCommunityCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/community");

        CommunityCardResponse response = communityCardService.createCommunityCard(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/community", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple community cards in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return List of created card responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<CommunityCardResponse>> createCommunityCardsBulk(
            @Valid @RequestBody List<CreateCommunityCardRequest> requests,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/community/bulk");

        List<CommunityCardResponse> responses = communityCardService.createCommunityCardsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/community/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return CommunityCardResponse containing the updated card
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> updateCommunityCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommunityCardRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/cards/community/" + id);

        CommunityCardResponse response = communityCardService.updateCommunityCard(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/cards/community/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to delete
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteCommunityCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/cards/community/" + id);

        communityCardService.deleteCommunityCard(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/cards/community/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted community card.
     * Requires ADMIN or OWNER role.
     *
     * @param id The card ID to restore
     * @param httpRequest The HTTP servlet request for IP extraction
     * @param authentication The authentication context for audit logging
     * @return CommunityCardResponse containing the restored card
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommunityCardResponse> restoreCommunityCard(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/cards/community/" + id + "/restore");

        CommunityCardResponse response = communityCardService.restoreCommunityCard(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/cards/community/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
