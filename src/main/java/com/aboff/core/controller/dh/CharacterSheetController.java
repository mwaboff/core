package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.request.LevelUpRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.LevelUpOptionsResponse;
import com.aboff.core.model.dto.dh.response.LevelUpResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.CharacterSheetService;
import com.aboff.core.service.dh.LevelUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing CharacterSheet entities.
 * <p>
 * Provides endpoints for CRUD operations on character sheets in the Daggerheart TTRPG system.
 * </p>
 * <p>
 * Access control:
 * - List all: Any authenticated user (regular users scoped to own characters, MODERATOR+ can see all)
 * - View single: Any authenticated user
 * - Create: Any authenticated user (becomes owner)
 * - Update: Character sheet owner OR MODERATOR/ADMIN/OWNER role
 * - Delete: Character sheet owner OR MODERATOR/ADMIN/OWNER role
 * </p>
 */
@RestController
@RequestMapping("/api/dh/character-sheets")
@RequiredArgsConstructor
public class CharacterSheetController {

    private final CharacterSheetService characterSheetService;
    private final LevelUpService levelUpService;

    /**
     * Retrieves a paginated list of character sheets.
     * <p>
     * Supports optional filtering by owner ID, name, and level range.
     * All authenticated users can access this endpoint. Regular users are
     * automatically scoped to only see their own character sheets.
     * MODERATOR+ users can see all character sheets and filter by any owner.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param ownerId Optional filter for character sheet owner ID
     * @param name Optional filter for character name (case-insensitive partial match)
     * @param minLevel Optional filter for minimum character level
     * @param maxLevel Optional filter for maximum character level
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The authentication object containing the current user
     * @return Paginated response containing character sheets with 200 OK status
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CharacterSheetResponse>> getAllCharacterSheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minLevel,
            @RequestParam(required = false) Integer maxLevel,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        PagedResponse<CharacterSheetResponse> response =
                characterSheetService.getAllCharacterSheets(page, size, ownerId, name, minLevel, maxLevel, expand, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single character sheet by ID.
     * <p>
     * All authenticated users can view any character sheet. When authenticated,
     * the response may include campaign info if the viewer has access.
     * </p>
     *
     * @param id The character sheet ID
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The authentication object containing the current user
     * @return Character sheet response with 200 OK status
     */
    @GetMapping("/{id}")
    public ResponseEntity<CharacterSheetResponse> getCharacterSheetById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        CharacterSheetResponse response = characterSheetService.getCharacterSheetById(id, expand, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new character sheet.
     * <p>
     * All authenticated users can create a character sheet. The authenticated user
     * becomes the owner of the newly created character sheet.
     * </p>
     *
     * @param request The character sheet creation request
     * @param authentication The authentication object containing the current user
     * @return Created character sheet response with 201 Created status
     */
    @PostMapping
    public ResponseEntity<CharacterSheetResponse> createCharacterSheet(
            @Valid @RequestBody CreateCharacterSheetRequest request,
            Authentication authentication) {

        CharacterSheetResponse response = characterSheetService.createCharacterSheet(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing character sheet.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update a character sheet. Supports partial updates - only non-null
     * fields in the request are updated.
     * </p>
     *
     * @param id The character sheet ID to update
     * @param request The update request containing new character sheet details
     * @param authentication The authentication object containing the current user
     * @return Updated character sheet response with 200 OK status
     */
    @PutMapping("/{id}")
    public ResponseEntity<CharacterSheetResponse> updateCharacterSheet(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCharacterSheetRequest request,
            Authentication authentication) {

        CharacterSheetResponse response = characterSheetService.updateCharacterSheet(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves available level-up options for a character sheet.
     * <p>
     * Returns information about available advancements, domain card constraints,
     * and tier transition details for the character's next level.
     * </p>
     *
     * @param id The character sheet ID
     * @param authentication The authentication object containing the current user
     * @return Level-up options response with 200 OK status
     */
    @GetMapping("/{id}/level-up-options")
    public ResponseEntity<LevelUpOptionsResponse> getLevelUpOptions(
            @PathVariable Long id,
            Authentication authentication) {

        LevelUpOptionsResponse response = levelUpService.getLevelUpOptions(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs a level-up operation on a character sheet.
     * <p>
     * Applies the selected advancements, tier achievements, domain card changes,
     * and saves an advancement log for undo support.
     * </p>
     *
     * @param id The character sheet ID
     * @param request The level-up request containing advancement choices
     * @param authentication The authentication object containing the current user
     * @return Level-up response with 200 OK status
     */
    @PostMapping("/{id}/level-up")
    public ResponseEntity<LevelUpResponse> levelUp(
            @PathVariable Long id,
            @Valid @RequestBody LevelUpRequest request,
            Authentication authentication) {

        LevelUpResponse response = levelUpService.levelUp(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Undoes the most recent level-up for a character sheet.
     * <p>
     * Reverses all changes from the last level-up and removes the advancement log.
     * </p>
     *
     * @param id The character sheet ID
     * @param authentication The authentication object containing the current user
     * @return Updated character sheet response with 200 OK status
     */
    @DeleteMapping("/{id}/level-up")
    public ResponseEntity<CharacterSheetResponse> undoLevelUp(
            @PathVariable Long id,
            Authentication authentication) {

        CharacterSheetResponse response = levelUpService.undoLevelUp(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a character sheet (soft delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a character sheet. This is a soft deletion that preserves the
     * data but marks it as deleted.
     * </p>
     *
     * @param id The character sheet ID to delete
     * @param authentication The authentication object containing the current user
     * @return Empty response with 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacterSheet(
            @PathVariable Long id,
            Authentication authentication) {

        characterSheetService.deleteCharacterSheet(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
