package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.LootService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Loot resources.
 * Provides endpoints for CRUD operations on Daggerheart loot items.
 * <p>
 * GET endpoints are accessible to all authenticated users.
 * POST/PUT/DELETE endpoints require ADMIN or OWNER role.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/loot")
@RequiredArgsConstructor
public class LootController {

    private final LootService lootService;

    /**
     * Retrieves a paginated list of loot items.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param includeDeleted Whether to include soft-deleted loot (default: false, ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for loot tier (1–4)
     * @param isConsumable Optional filter for consumable status
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalLoot")
     * @return Paginated response containing loot items
     */
    @GetMapping
    public ResponseEntity<PagedResponse<LootResponse>> getAllLoot(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Boolean isConsumable,
            @RequestParam(required = false) String expand) {

        PagedResponse<LootResponse> response = lootService.getAllLoot(
                page, size, includeDeleted, expansionId, isOfficial, tier, isConsumable, expand);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single loot item by ID.
     *
     * @param id The loot ID
     * @param expand Comma-separated list of relationships to expand (e.g., "expansion,originalLoot")
     * @return LootResponse containing the loot details
     */
    @GetMapping("/{id}")
    public ResponseEntity<LootResponse> getLootById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand) {

        LootResponse response = lootService.getLootById(id, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param request The creation request containing loot details
     * @return LootResponse containing the created loot
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<LootResponse> createLoot(
            @Valid @RequestBody CreateLootRequest request) {

        LootResponse response = lootService.createLoot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates multiple loot items in bulk.
     * Requires ADMIN or OWNER role.
     *
     * @param requests List of creation requests
     * @return List of created loot responses
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<LootResponse>> createLootBulk(
            @Valid @RequestBody List<CreateLootRequest> requests) {

        List<LootResponse> responses = lootService.createLootBulk(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Updates an existing loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to update
     * @param request The update request containing new loot details
     * @return LootResponse containing the updated loot
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<LootResponse> updateLoot(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLootRequest request) {

        LootResponse response = lootService.updateLoot(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes a loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteLoot(@PathVariable Long id) {
        lootService.deleteLoot(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted loot item.
     * Requires ADMIN or OWNER role.
     *
     * @param id The loot ID to restore
     * @return LootResponse containing the restored loot
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<LootResponse> restoreLoot(@PathVariable Long id) {
        LootResponse response = lootService.restoreLoot(id);
        return ResponseEntity.ok(response);
    }
}
