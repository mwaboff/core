package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.CreateCompanionTrainingRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.CompanionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Companion resources.
 * <p>
 * Provides endpoints for CRUD operations on character companions, and on a companion's
 * Training selections, in the Daggerheart TTRPG system.
 * </p>
 * <p>
 * Access control: every endpoint, including GET, requires the caller to be the owning
 * character sheet's owner OR hold MODERATOR/ADMIN/OWNER role (enforced in the service layer).
 * {@code GET /api/dh/companions} requires a {@code characterSheetId} filter -- there is no
 * unfiltered listing.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/companions")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of a character sheet's active companions.
     * <p>
     * {@code characterSheetId} is required -- this endpoint is always scoped to one character
     * sheet and access-checked against it, never an unfiltered global listing.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param characterSheetId The character sheet to list companions for (required)
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,experiences")
     * @param authentication The authentication object containing the current user
     * @return Paginated response containing the character sheet's active companions
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CompanionResponse>> getAllCompanions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long characterSheetId,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        PagedResponse<CompanionResponse> response = companionService.getAllCompanions(
                page, size, characterSheetId, expand, authentication);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single active companion by ID.
     *
     * @param id The companion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,experiences")
     * @param authentication The authentication object containing the current user
     * @return CompanionResponse containing the companion details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanionResponse> getCompanionById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/companions/" + id);

        CompanionResponse response = companionService.getCompanionById(id, expand, authentication);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/companions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new companion for a character.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can create a companion.
     * </p>
     *
     * @param request The creation request containing companion details
     * @param authentication The authentication object containing the current user
     * @return CompanionResponse containing the created companion (201 Created)
     */
    @PostMapping
    public ResponseEntity<CompanionResponse> createCompanion(
            @Valid @RequestBody CreateCompanionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/companions");

        CompanionResponse response = companionService.createCompanion(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/companions", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing companion.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update a companion. Supports partial updates - only provided fields
     * are updated.
     * </p>
     *
     * @param id The companion ID to update
     * @param request The update request containing new companion details
     * @param authentication The authentication object containing the current user
     * @return CompanionResponse containing the updated companion (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompanionResponse> updateCompanion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/companions/" + id);

        CompanionResponse response = companionService.updateCompanion(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/companions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a companion.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a companion. The companion is archived, not permanently removed, so it can be
     * restored later (e.g. by a level-up that re-takes the subclass that granted it).
     * </p>
     *
     * @param id The companion ID to delete
     * @param authentication The authentication object containing the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompanion(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/companions/" + id);

        companionService.deleteCompanion(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/companions/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a single Training selection to a companion via the manual/GM path.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can add Training.
     * The selection's {@code acquiredAtLevel} is set automatically to the character's current
     * level -- this endpoint is outside the level-up advancement log, so Training added here is
     * never reversed by level-down.
     * </p>
     *
     * @param id The companion ID to add the Training selection to
     * @param request The Training selection to add
     * @param authentication The authentication object containing the current user
     * @return CompanionResponse with the updated Training list and derived stats (201 Created)
     */
    @PostMapping("/{id}/trainings")
    public ResponseEntity<CompanionResponse> addTraining(
            @PathVariable Long id,
            @Valid @RequestBody CreateCompanionTrainingRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/companions/" + id + "/trainings");

        CompanionResponse response = companionService.addTraining(id, request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/companions/" + id + "/trainings", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Removes a single Training selection from a companion via the manual/GM path.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can remove
     * Training.
     * </p>
     *
     * @param id The companion ID to remove the Training selection from
     * @param trainingId The Training selection to remove
     * @param authentication The authentication object containing the current user
     * @return CompanionResponse with the updated Training list and derived stats (200 OK)
     */
    @DeleteMapping("/{id}/trainings/{trainingId}")
    public ResponseEntity<CompanionResponse> removeTraining(
            @PathVariable Long id,
            @PathVariable Long trainingId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/companions/" + id + "/trainings/" + trainingId);

        CompanionResponse response = companionService.removeTraining(id, trainingId, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/companions/" + id + "/trainings/" + trainingId, startTime);
        return ResponseEntity.ok(response);
    }
}
