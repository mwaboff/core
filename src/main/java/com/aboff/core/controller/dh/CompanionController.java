package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.CompanionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Companion resources.
 * <p>
 * Provides endpoints for CRUD operations on character companions in the
 * Daggerheart TTRPG system.
 * </p>
 * <p>
 * Access control:
 * - GET endpoints: All authenticated users
 * - POST/PUT/DELETE endpoints: Character sheet owner OR MODERATOR/ADMIN/OWNER role
 *   (enforced in service layer)
 * </p>
 */
@RestController
@RequestMapping("/api/dh/companions")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;

    /**
     * Retrieves a paginated list of companions.
     * <p>
     * Optionally filters by character sheet ID to show companions for a
     * specific character.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param characterSheetId Optional filter for character sheet ID
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,experiences")
     * @return Paginated response containing companions
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CompanionResponse>> getAllCompanions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long characterSheetId,
            @RequestParam(required = false) String expand) {

        PagedResponse<CompanionResponse> response = companionService.getAllCompanions(
                page, size, characterSheetId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single companion by ID.
     *
     * @param id The companion ID
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,experiences")
     * @return CompanionResponse containing the companion details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanionResponse> getCompanionById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        CompanionResponse response = companionService.getCompanionById(id, expand);
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
            Authentication authentication) {

        CompanionResponse response = companionService.createCompanion(request, authentication);
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
            Authentication authentication) {

        CompanionResponse response = companionService.updateCompanion(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a companion (hard delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a companion. This is a permanent deletion that cascades to
     * associated experiences.
     * </p>
     *
     * @param id The companion ID to delete
     * @param authentication The authentication object containing the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompanion(
            @PathVariable Long id,
            Authentication authentication) {

        companionService.deleteCompanion(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
