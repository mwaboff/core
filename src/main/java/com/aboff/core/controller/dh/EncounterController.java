package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateEncounterRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRequest;
import com.aboff.core.model.dto.dh.response.EncounterResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.EncounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Encounter resources.
 * Provides endpoints for CRUD operations on Daggerheart encounters (groups of adversaries).
 * <p>
 * Access control:
 * </p>
 * <ul>
 *   <li>GET endpoints: All authenticated users (filtered by visibility)</li>
 *   <li>POST (single/copy): All authenticated users</li>
 *   <li>PUT/DELETE: Permission check in service (creator OR moderator+ for non-official,
 *       OWNER only for official)</li>
 *   <li>POST restore: ADMIN/OWNER only</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dh/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of encounters.
     * Returns encounters that are official, public, or created by the authenticated user.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted encounters (ADMIN+ only)
     * @param creatorId Optional filter for the encounter's creator. Narrows the result set to
     *                  that user's encounters, but the usual official/public/own visibility
     *                  rules still apply on top of it -- filtering to another user's ID returns
     *                  only their official/public encounters, never their private ones.
     * @param campaignId Optional filter for campaign ID
     * @param tier Optional filter for tier (1-4)
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param expand Comma-separated list of relationships to expand
     *               (creator, campaign, environment, originalEncounter, adversaryDetails)
     * @param auth Authentication context
     * @return Paginated response containing encounters
     */
    @GetMapping
    public ResponseEntity<PagedResponse<EncounterResponse>> getAllEncounters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String expand,
            Authentication auth) {

        PagedResponse<EncounterResponse> response = encounterService.getAllEncounters(
                page, size, includeDeleted, creatorId, campaignId, tier, isOfficial, name, expand, auth);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single encounter by ID.
     * Access is restricted to official, public, or user's own encounters.
     *
     * @param id The encounter ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return EncounterResponse containing the encounter details
     */
    @GetMapping("/{id}")
    public ResponseEntity<EncounterResponse> getEncounterById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/encounters/" + id);

        EncounterResponse response = encounterService.getEncounterById(id, expand, auth);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/encounters/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new encounter.
     * The authenticated user becomes the creator.
     *
     * @param request The creation request containing encounter details
     * @param auth Authentication context
     * @return EncounterResponse containing the created encounter
     */
    @PostMapping
    public ResponseEntity<EncounterResponse> createEncounter(
            @Valid @RequestBody CreateEncounterRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/encounters");

        EncounterResponse response = encounterService.createEncounter(request, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/encounters", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a copy of an existing encounter for the authenticated user.
     * The copy is private by default and linked to the original via originalEncounterId.
     *
     * @param id The ID of the encounter to copy
     * @param auth Authentication context
     * @return EncounterResponse containing the new copy
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<EncounterResponse> copyEncounter(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/encounters/" + id + "/copy");

        EncounterResponse response = encounterService.copyEncounter(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/encounters/" + id + "/copy", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing encounter.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official encounters: OWNER role only</li>
     *   <li>Non-official encounters: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The encounter ID to update
     * @param request The update request containing new encounter details
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @PutMapping("/{id}")
    public ResponseEntity<EncounterResponse> updateEncounter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEncounterRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/encounters/" + id);

        EncounterResponse response = encounterService.updateEncounter(id, request, auth);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/encounters/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes an encounter.
     * <p>
     * Permission requirements:
     * </p>
     * <ul>
     *   <li>Official encounters: OWNER role only</li>
     *   <li>Non-official encounters: Creator OR MODERATOR+ role</li>
     * </ul>
     *
     * @param id The encounter ID to delete
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEncounter(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/encounters/" + id);

        encounterService.deleteEncounter(id, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/encounters/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted encounter.
     * Requires ADMIN or OWNER role.
     *
     * @param id The encounter ID to restore
     * @param auth Authentication context
     * @return EncounterResponse containing the restored encounter
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<EncounterResponse> restoreEncounter(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/encounters/" + id + "/restore");

        EncounterResponse response = encounterService.restoreEncounter(id, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/encounters/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds an adversary instance to an encounter.
     *
     * @param id The encounter ID
     * @param adversaryId The adversary ID to add
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @PostMapping("/{id}/adversaries")
    public ResponseEntity<EncounterResponse> addAdversaryToEncounter(
            @PathVariable Long id,
            @RequestParam Long adversaryId,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/encounters/" + id + "/adversaries");

        EncounterResponse response = encounterService.addAdversaryToEncounter(id, adversaryId, auth);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/encounters/" + id + "/adversaries", startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes an adversary instance from an encounter.
     *
     * @param id The encounter ID
     * @param encounterAdversaryId The encounter adversary ID to remove
     * @param auth Authentication context
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}/adversaries/{encounterAdversaryId}")
    public ResponseEntity<Void> removeAdversaryFromEncounter(
            @PathVariable Long id,
            @PathVariable Long encounterAdversaryId,
            Authentication auth,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(auth).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/encounters/" + id + "/adversaries/" + encounterAdversaryId);

        encounterService.removeAdversaryFromEncounter(id, encounterAdversaryId, auth);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/encounters/" + id + "/adversaries/" + encounterAdversaryId, startTime);
        return ResponseEntity.noContent().build();
    }
}
