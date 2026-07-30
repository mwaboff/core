package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetConditionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.CharacterSheetConditionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing CharacterSheetCondition resources — a character's per-instance
 * conditions, each carrying its own magnitude.
 * <p>
 * Access control:
 * - GET endpoints: All authenticated users
 * - POST endpoint: All authenticated users
 * - PUT/DELETE endpoints: Character sheet owner OR MODERATOR/ADMIN/OWNER role
 *   (enforced in service layer)
 * </p>
 */
@RestController
@RequestMapping("/api/dh/character-sheet-conditions")
@RequiredArgsConstructor
public class CharacterSheetConditionController {

    private final CharacterSheetConditionService characterSheetConditionService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of condition instances for a character sheet.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param characterSheetId The character sheet ID to filter by
     * @param expand Comma-separated list of relationships to expand (e.g., "characterSheet,condition")
     * @return Paginated response containing condition instances
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CharacterSheetConditionResponse>> getConditionsForCharacterSheet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Long characterSheetId,
            @RequestParam(required = false) String expand) {

        PagedResponse<CharacterSheetConditionResponse> response =
                characterSheetConditionService.getConditionsForCharacterSheet(page, size, characterSheetId, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single condition instance by ID.
     *
     * @param id The condition instance ID
     * @param expand Comma-separated list of relationships to expand
     * @return CharacterSheetConditionResponse containing the instance details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CharacterSheetConditionResponse> getConditionInstanceById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/character-sheet-conditions/" + id);

        CharacterSheetConditionResponse response = characterSheetConditionService.getConditionInstanceById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/character-sheet-conditions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Attaches a condition instance to a character sheet.
     * Any authenticated user can attach a condition.
     *
     * @param request The creation request containing character sheet, condition, and magnitude
     * @param authentication The authentication object containing the current user
     * @return CharacterSheetConditionResponse containing the created instance (201 Created)
     */
    @PostMapping
    public ResponseEntity<CharacterSheetConditionResponse> createCharacterSheetCondition(
            @Valid @RequestBody CreateCharacterSheetConditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/character-sheet-conditions");

        CharacterSheetConditionResponse response =
                characterSheetConditionService.createCharacterSheetCondition(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/character-sheet-conditions", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the magnitude of an existing condition instance.
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can update.
     *
     * @param id The condition instance ID to update
     * @param request The update request containing the new magnitude
     * @param authentication The authentication object containing the current user
     * @return CharacterSheetConditionResponse containing the updated instance (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<CharacterSheetConditionResponse> updateCharacterSheetCondition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCharacterSheetConditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/character-sheet-conditions/" + id);

        CharacterSheetConditionResponse response =
                characterSheetConditionService.updateCharacterSheetCondition(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/character-sheet-conditions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a condition instance from a character sheet (hard delete).
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can remove.
     *
     * @param id The condition instance ID to remove
     * @param authentication The authentication object containing the current user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacterSheetCondition(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/character-sheet-conditions/" + id);

        characterSheetConditionService.deleteCharacterSheetCondition(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/character-sheet-conditions/" + id, startTime);
        return ResponseEntity.noContent().build();
    }
}
