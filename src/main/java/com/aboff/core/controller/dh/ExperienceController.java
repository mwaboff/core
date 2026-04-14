package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateExperienceRequest;
import com.aboff.core.model.dto.dh.request.UpdateExperienceRequest;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.ExperienceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Experience resources.
 * <p>
 * Provides endpoints for CRUD operations on character experiences in the
 * Daggerheart TTRPG system.
 * </p>
 * <p>
 * Access control:
 * - GET endpoints: All authenticated users
 * - POST endpoint: All authenticated users
 * - PUT/DELETE endpoints: Character sheet owner OR MODERATOR/ADMIN/OWNER role
 *   (enforced in service layer)
 * </p>
 */
@RestController
@RequestMapping("/api/dh/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of experiences.
     * <p>
     * Optionally filters by character sheet ID or companion ID to show experiences for a
     * specific character or companion.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param characterSheetId Optional filter for character sheet ID
     * @param companionId Optional filter for companion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,companion,createdBy")
     * @return Paginated response containing experiences
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ExperienceResponse>> getAllExperiences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long characterSheetId,
            @RequestParam(required = false) Long companionId,
            @RequestParam(required = false) String expand) {

        PagedResponse<ExperienceResponse> response = experienceService.getAllExperiences(
                page, size, characterSheetId, companionId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single experience by ID.
     *
     * @param id The experience ID
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,companion,createdBy")
     * @return ExperienceResponse containing the experience details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperienceResponse> getExperienceById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/experiences/" + id);

        ExperienceResponse response = experienceService.getExperienceById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/experiences/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new experience for a character.
     * <p>
     * Any authenticated user can create an experience. The current user will be
     * recorded as the creator.
     * </p>
     *
     * @param request The creation request containing experience details
     * @param authentication The authentication object containing the current user
     * @return ExperienceResponse containing the created experience (201 Created)
     */
    @PostMapping
    public ResponseEntity<ExperienceResponse> createExperience(
            @Valid @RequestBody CreateExperienceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/experiences");

        ExperienceResponse response = experienceService.createExperience(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/experiences", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing experience.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update an experience. Supports partial updates - only provided fields
     * are updated.
     * </p>
     *
     * @param id The experience ID to update
     * @param request The update request containing new experience details
     * @param authentication The authentication object containing the current user
     * @return ExperienceResponse containing the updated experience (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExperienceResponse> updateExperience(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExperienceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/experiences/" + id);

        ExperienceResponse response = experienceService.updateExperience(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/experiences/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an experience (hard delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete an experience. This is a permanent deletion.
     * </p>
     *
     * @param id The experience ID to delete
     * @param authentication The authentication object containing the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperience(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/experiences/" + id);

        experienceService.deleteExperience(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/experiences/" + id, startTime);
        return ResponseEntity.noContent().build();
    }
}
