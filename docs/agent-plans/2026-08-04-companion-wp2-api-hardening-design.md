# Companions WP2: API Hardening — Design

**Date:** 2026-08-04
**bd issue:** core-7zn (P1, blocks core-6oy)
**Depends on:** WP1 (`e4de987`, committed)
**Source of truth:** `dawn/.agents/plans/companions/companions-implementation-plan.md` §3.11, §3.12, §5.1, §10 (§10 wins on conflict)

## 1. Context

WP1 landed the schema/entity/enum/derivation layer for companions (base stats + `CompanionTraining` + `CompanionDerivationService`). The pre-existing `CompanionService`/`CompanionController`/DTOs predate that work and still operate on the old flat model, with a live security hole: `getAllCompanions`/`getCompanionById` have no ownership check and no `Authentication` parameter, so `expand=experiences` currently leaks every user's companions and Experience text to any logged-in visitor. This package closes that hole and brings the API up to the WP1 data model.

## 2. Approach

### 2.1 Access control (the security fix)

- Generalize the existing `private void validateAccess(Companion, Authentication, String)` into a `CharacterSheet`-based helper (`validateSheetAccess(CharacterSheet, Authentication, String)`) that both `createCompanion` and the new `getAllCompanions` can call; the `Companion` overload delegates to it via `companion.getCharacterSheet()`. This reuses the codebase's owner-or-`hasModeratorOrHigher` pattern rather than inventing a new one, per the team's instruction.
- `getAllCompanions(int page, int size, Long characterSheetId, String expand, Authentication auth)`: `characterSheetId` becomes semantically required. The controller keeps it as an optional `@RequestParam` (no Spring-level 400 for a missing param is wired for this repo — `MissingServletRequestParameterException` isn't in `GlobalExceptionHandler` and would fall through to the generic 500 handler), so the service throws `IllegalStateException("characterSheetId is required")` when null, which the existing handler maps to 400. Then `characterSheetRepository.findActiveById(...)`, then `validateSheetAccess`, then list via a new paginated `CompanionRepository.findActiveByCharacterSheetId(Long, Pageable)` (excludes soft-deleted).
- `getCompanionById(Long id, String expand, Authentication auth)`: load via `companionRepository.findById`, treat a soft-deleted companion as not found (`EntityNotFoundException`), then `validateAccess`.
- Both endpoints move from "any authenticated user" to owner-or-MODERATOR+, matching create/update/delete. Convention check confirmed against sibling tests: this codebase already returns 403 for InsufficientPermissionsException and 404 for EntityNotFoundException (`GlobalExceptionHandler`), so the new tests assert 403 for a wrong owner and 404 for soft-deleted/missing rows — matching `CompanionControllerIntegrationTest`'s existing style for create/update/delete.

### 2.2 Soft delete

- `Companion.softDelete()`/`restore()` already exist (WP1). `deleteCompanion` calls `companion.softDelete()` + `companionRepository.save(...)` instead of `companionRepository.delete(...)`.
- `getAllCompanions`, `getCompanionById`, `updateCompanion`, and the new training endpoints all exclude/reject soft-deleted companions (treated as `EntityNotFoundException`). WP5's restore flow reads soft-deleted rows directly via the repository, not through this service, so no "include deleted" flag is added here (YAGNI).

### 2.3 Validation bounds

`CreateCompanionRequest`/`UpdateCompanionRequest` currently accept unbounded `evasion`/`stressMax`/`stressMarked` and never check `stressMarked <= stressMax`. Add:
- `evasion`: `@Min(0) @Max(50)`
- `stressMax`: `@Min(1) @Max(20)`
- `stressMarked`: `@Min(0) @Max(20)`

(Upper bounds are generous sanity ceilings, not derived from a rules cap — there is no printed maximum — chosen to block obviously-bad input without constraining legitimate homebrew.) Bean Validation can't express the cross-field `stressMarked <= stressMax` rule cleanly, so that check lives in the service (both create and update, using the value that will actually be persisted) and throws `IllegalStateException` → 400, consistent with this codebase's existing cross-field pattern (see `reverseAdvancement`'s clamp logic elsewhere).

### 2.4 `CompanionResponse` — base vs. derived

Today's `evasion`/`stressMax`/`damageDice`/`attackRange` fields currently just echo the WP1 base columns directly (a leftover from before derivation existed). Per the contract, `toResponse` must compute them through `CompanionDerivationService` instead, and add:
- `baseEvasion`, `baseStressMax`, `baseDamageDice`, `baseAttackRange` (for the edit modal)
- `trainings: List<CompanionTrainingResponse>` (new small DTO: `id`, `option`, `viciousAxis`, `targetExperienceId`, `acquiredAtLevel`) — always included, not expand-gated, since it's small and core to the entity
- `remainingByOption: Map<CompanionTrainingOption, Integer>` (from `CompanionDerivationService.remainingByOption`)
- `damageType`, `origin`, `advancesOnLevelUp`, `outOfScene` (from `CompanionDerivationService.outOfScene`)
- `attackDiceCount`: `companion.getCharacterSheet().getProficiency()` — read live at response-build time, never snapshotted, per plan §4.3/§10.

### 2.5 Training endpoints

- `POST /api/dh/companions/{id}/trainings` — body `CreateCompanionTrainingRequest { CompanionTrainingOption option (@NotNull); ViciousAxis viciousAxis; Long targetExperienceId }`. `acquiredAtLevel` is not client-supplied — it's set to `companion.getCharacterSheet().getLevel()` automatically, since this manual/GM path is intentionally outside the level-up advancement log (MANUAL/GM_GRANTED companions are never touched by level-down reversal per plan §5.4). Returns the updated `CompanionResponse` (200/201) rather than just the new training row, since derived stats change and callers need the full picture without a second fetch.
- `DELETE /api/dh/companions/{id}/trainings/{trainingId}` — mutates via `companion.getTrainings().removeIf(t -> t.getId().equals(trainingId))` then saves the parent, **never** `companionTrainingRepository.delete(...)` (orphanRemoval resurrection trap, documented on `CompanionTraining`/in `2026-03-15-leveldown-domain-card-fix-design.md`). 404s if no row matched. Returns the updated `CompanionResponse`.
- Both access-checked via the same `validateAccess` helper as update/delete.

**Shared cap-enforcement helper (WP5 depends on this exact signature):**

```java
package com.aboff.core.service.dh;

public final class CompanionTrainingValidator {
    private CompanionTrainingValidator() {}

    /**
     * Validates that a proposed Training pick is legal for the given companion.
     * Pure/static: reads only companion.getTrainings() and companion.getExperiences(),
     * both already loaded on any Companion passed in. Throws IllegalStateException
     * (mapped to 400) on any violation:
     *  - option has no remaining selections (CompanionDerivationService.remainingByOption)
     *  - option == VICIOUS: viciousAxis is null, or that axis's derived ladder value is
     *    already at its cap (D12 for DAMAGE_DIE, VERY_FAR for RANGE)
     *  - option == INTELLIGENT: targetExperienceId is null, or does not belong to this
     *    companion's own experiences collection
     */
    public static void validatePick(
            Companion companion,
            CompanionTrainingOption option,
            ViciousAxis viciousAxis,
            Long targetExperienceId);
}
```

This does **not** enforce "exactly N picks this level-up" — that's a level-up-specific concern WP5 owns; this validator only enforces the per-companion-lifetime cap and the option's own precondition, both of which apply identically to the manual endpoint and to level-up.

### 2.6 Blueprint

Full rewrite of `core/.api-blueprint/references/companions-api.md` in the same change: new required `characterSheetId` on GET, 403/404 semantics, soft-delete note, full new `CompanionResponse` shape, training endpoints, new `CompanionTrainingOption`/`CompanionOrigin`/`ViciousAxis` enum tables.

## 3. File changes

| File | Change |
|---|---|
| `service/dh/CompanionService.java` | Access-control fix, soft delete, derived-value responses, training add/remove methods |
| `service/dh/CompanionTrainingValidator.java` | **New** — shared cap-enforcement helper |
| `controller/dh/CompanionController.java` | `Authentication` on GET endpoints, two new training endpoints |
| `repository/dh/CompanionRepository.java` | `findActiveByCharacterSheetId(Long, Pageable)` |
| `repository/dh/CompanionTrainingRepository.java` | No change (existing `countByCompanionIdAndOption`/`findByCompanionId` unused by WP2 directly — validator reads the in-memory collection instead, kept for WP5/future use) |
| `model/dto/dh/request/CreateCompanionRequest.java`, `UpdateCompanionRequest.java` | Bounds |
| `model/dto/dh/request/CreateCompanionTrainingRequest.java` | **New** |
| `model/dto/dh/response/CompanionResponse.java` | New fields per §2.4 |
| `model/dto/dh/response/CompanionTrainingResponse.java` | **New** |
| `model/enums/AuditAction.java` | `COMPANION_TRAINING_ADDED`, `COMPANION_TRAINING_REMOVED` |
| `.api-blueprint/references/companions-api.md` | Rewrite |
| `CompanionServiceTest.java`, `CompanionControllerIntegrationTest.java` | Rewritten/extended — the leak-closed proof is the priority deliverable |

## 4. Testing strategy

- Unit (`CompanionServiceTest`): access-control branches (owner/moderator/stranger) for all five operations including the two new ones; null-`characterSheetId` rejection; soft-delete excludes from list/get; bounds/cross-field validation; `CompanionTrainingValidator` cap/axis/experience-ownership branches (own test class).
- Integration (`CompanionControllerIntegrationTest`): the priority scenarios per the team's brief — user A cannot list/fetch user B's companions (403/404), MODERATOR+ can, owner can, missing `characterSheetId` rejected (400); soft-delete round-trip; training POST/DELETE cap enforcement end-to-end.
- Gate: `./mvnw test` and `./mvnw verify` green, 80%+ coverage on touched service/controller code.

## 5. Explicit deviations from the literal brief (flagged, not silent)

- `getAllCompanions`/training endpoints return 400 (not a Spring-native 400) for a missing/invalid `characterSheetId`, via `IllegalStateException`, because that's the only "bad request" exception type this codebase already wires up outside bean validation — avoids introducing a new exception-handling pattern for one call site.
- Training endpoints return the full updated `CompanionResponse` rather than 204/the bare training row — judgment call favoring one round trip for consumers, flagged for WP5/WP8 to confirm is what they want.
