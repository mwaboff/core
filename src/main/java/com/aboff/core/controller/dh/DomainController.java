package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateDomainRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainRequest;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.DomainService;
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
 * REST controller for managing Domain resources.
 * Provides endpoints for CRUD operations on Daggerheart domains.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of domains.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted domains (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return Paginated response containing domains
     */
    @GetMapping
    public ResponseEntity<PagedResponse<DomainResponse>> getAllDomains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) String expand) {

        PagedResponse<DomainResponse> response = domainService.getAllDomains(
                page, size, includeDeleted, expansionId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single domain by ID.
     *
     * @param id The domain ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion")
     * @return DomainResponse containing the domain details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DomainResponse> getDomainById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/domains/" + id);

        DomainResponse response = domainService.getDomainById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/domains/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new domain.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing domain details
     * @param auth Authentication context
     * @return DomainResponse containing the created domain
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainResponse> createDomain(
            @Valid @RequestBody CreateDomainRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/domains");

        DomainResponse response = domainService.createDomain(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/domains", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple domains in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @param auth Authentication context
     * @return List of created domain responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<DomainResponse>> createDomainsBulk(
            @Valid @RequestBody List<CreateDomainRequest> requests,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/domains/bulk");

        List<DomainResponse> responses = domainService.createDomainsBulk(requests, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/domains/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing domain.
     * Requires ADMIN or OWNER role.
     *
     * @param id The domain ID to update
     * @param request The update request containing new domain details
     * @param auth Authentication context
     * @return DomainResponse containing the updated domain
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainResponse> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDomainRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/domains/" + id);

        DomainResponse response = domainService.updateDomain(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/domains/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a domain.
     * Requires ADMIN or OWNER role.
     *
     * @param id The domain ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/domains/" + id);

        domainService.deleteDomain(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/domains/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted domain.
     * Requires ADMIN or OWNER role.
     *
     * @param id The domain ID to restore
     * @param auth Authentication context
     * @return DomainResponse containing the restored domain
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<DomainResponse> restoreDomain(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/domains/" + id + "/restore");

        DomainResponse response = domainService.restoreDomain(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/domains/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}
