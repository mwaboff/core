# Custom Items (Weapons / Armor / Loot) — Two-Repo Implementation Plan

## Context

Oh Sheet currently only offers official Daggerheart items. This feature lets **any authenticated user create custom weapons, armor, and loot** with full field parity to official items. Custom items are **globally visible** to all users, searchable alongside official content (filterable via the existing `isOfficial` flag), and addable to character sheets. Only the **creator or MODERATOR+** can edit a custom item (official items remain ADMIN/OWNER-only); **deletion stays ADMIN/OWNER-only** via the admin portal.

Two repos are affected:
- **Backend**: `/home/michael/Documents/Projects/core` (Spring Boot, Flyway, JPA)
- **Frontend**: `/home/michael/Documents/Projects/dawn` (Angular 21, signals, Vitest)

### Decisions made with the user
1. **Global visibility** — no campaign scoping.
2. **Full field parity** with official items (same schema, same card rendering).
3. **Auth approach**: loosen `@PreAuthorize` on single-item POST/PUT only; enforcement moves to the service layer. `/bulk`, `/restore`, DELETE stay `hasAnyRole('ADMIN','OWNER')`.
4. **Delete**: keep the existing convention exactly — soft delete, item vanishes from browse/search but stays attached to sheets that already have it. **No delete-related code changes.**
5. **Form reuse**: extract the admin card editor's schema-driven form into a shared component supporting `create`/`edit` modes, used by both admin and the new public page.

### Key discoveries (things NOT to rebuild)
- **No DB migration needed.** `weapons`/`armors`/`loot` tables already have `is_official` and `created_by_user_id` columns + indexes. `BaseItem.java` already maps both.
- **`AdversaryService`/`AdversaryController` is an existing end-to-end precedent** for user-generated content: no `@PreAuthorize` on create, `createdBy` set from `Authentication`, `isOfficial` forced false for non-privileged users, `validateModifyPermission` (official → elevated role; custom → creator OR MODERATOR+). Mirror it. One deliberate deviation: official-item edits require **ADMIN+** (Adversary uses OWNER-only).
- **Search indexing is free.** `Weapon`/`Armor`/`Loot` are `@SearchIndexed`; `SearchIndexEventListener` + `SearchFieldMapping.buildForWeapon/Armor/Loot` already map `isOfficial` and `createdBy` into `search_index`. Once `createdBy` is populated on create (the one real bug: services never set it), custom items index automatically.
- **`isOfficial` query param already works** on all three GET-list endpoints (`true`/`false`/omitted=all).

---

# Part A — Backend plan (core repo)

All changes are made identically ×3 (Weapon / Armor / Loot). Files per type:
- Controller: `src/main/java/com/aboff/core/controller/dh/{Weapon,Armor,Loot}Controller.java`
- Service: `src/main/java/com/aboff/core/service/dh/{Weapon,Armor,Loot}Service.java`
- Tests: `src/test/java/com/aboff/core/controller/dh/{Weapon,Armor,Loot}ControllerIntegrationTest.java`
- Reference implementation to mirror: `AdversaryService.validateModifyPermission` (~lines 460–480) and `AdversaryService`'s create flow.

### A0. Verification spikes (no code)
- Confirm Armor/Loot services/controllers structurally mirror Weapon's.
- Confirm `SecurityConfig.java` doesn't whitelist `/api/dh/{weapons,armors,loot}` POST/PUT as public (removing `@PreAuthorize` must still require authentication).
- Read `SearchService.java` / codex-facing search endpoint: does it expose an `isOfficial` filter param? If not, that's A3.

### A1. Weapon (reference implementation)
**Controller** — remove `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")` from `createWeapon` (POST) and `updateWeapon` (PUT) only. Leave `/bulk`, DELETE, `/{id}/restore` untouched. Update class javadoc (use `AdversaryController` lines 30–43 doc block as template).

**Service** (`WeaponService.java`):
- Inject `RoleHierarchyService` (constructor / Lombok field, as `AdversaryService` does).
- `createWeapon`: derive caller from `Authentication` (`CustomUserDetails`); `isPrivileged = hasRoleOrHigher(user, Role.ADMIN)`; `isOfficial = isPrivileged && Boolean.TRUE.equals(request.getIsOfficial())`; set `.isOfficial(isOfficial)` and `.createdBy(isOfficial ? null : creator)` on the builder. Non-privileged callers sending `isOfficial=true` are **silently coerced to false** (not 400).
- `updateWeapon`: add `validateModifyPermission(weapon, authentication)` before mutation:
  - `isOfficial == true` → require ADMIN+ else `InsufficientPermissionsException`.
  - else → allow if `createdBy.id == callerId` OR `hasModeratorOrHigher`, else throw.
  - Only ADMIN+ may change `isOfficial` on update (silently ignore otherwise).
- `deleteWeapon`/`restoreWeapon`/bulk: **unchanged**.
- No DTO changes (never accept a client-supplied `createdByUserId`). Ensure `WeaponResponse` (and Armor/Loot responses) expose `createdByUserId` — add if missing (frontend needs it for ownership UI).

**Tests** — rewrite the 403-asserting tests:
- `createWeapon_AsUser` → 201; `isOfficial` forced false even when request says true; `createdBy` = caller.
- `updateWeapon`: own custom item → 200; other user's custom item → 403; official item as USER/MODERATOR → 403; other's custom item as MODERATOR → 200; official as ADMIN → 200.
- Bulk/delete/restore 403 tests stay green unchanged.
- Add `getAllWeapons?isOfficial=false` returns only custom items.

Verify: `./mvnw test -Dtest=WeaponControllerIntegrationTest`

### A2. Armor + Loot — replicate A1 exactly (adjusting for per-type fields). Verify with their integration tests.

### A3. Codex search filter (conditional on A0 finding)
If the codex/search endpoint lacks `isOfficial`, thread a `Boolean isOfficial` param through to the `search_index` query using the existing `(:param IS NULL OR field = :param)` JPQL idiom (`search_index.is_official` column already exists and is populated). Add endpoint test.

### A4. Hardening (recommended, small)
- Per-user creation cap (e.g. 200 non-deleted custom items per type): count query in create, new `TooManyCustomItemsException` → 429 in `GlobalExceptionHandler`.
- Enrich existing `auditLogger.log(...)` messages on create/update to include `isOfficial` and `createdBy` id.
- File a follow-up `bd` issue (not in-scope): `created_by_user_id` FK has no `ON DELETE` clause → hard-deleting a `users` row will FK-violate once custom items exist.

### A5. Full regression
`./mvnw test` + manual smoke test.

---

# Part B — Frontend plan (dawn repo)

### B0. Verification spikes (no code)
- Field name for current user id in `auth.model.ts` (`UserResponse.id` vs `userId`).
- `search-result.mapper.ts`: does WEAPON/ARMOR/LOOT `expandedEntity` retain the raw response shape (needed by `inventory-add-panel`) or coerce to `CardData` only?
- `filter-rail.ts` `onSelectChange`/`getSelectValue`: confirm string→boolean coercion for a select-based `isOfficial` needs no special case.
- Backend `createdByUserId` in response DTOs must land first (coordinate with Part A).

### B1. Models & mappers (foundation)
- Add `createdByUserId: number | null` to `WeaponResponse` (`shared/models/weapon-api.model.ts`), `ArmorResponse`, `LootApiResponse`.
- Add `export type ItemVisibilityFilter = 'all' | 'official' | 'custom'` in `search.model.ts`.
- Thread `createdByUserId` into `CardData.metadata` in `weapon/armor/loot.mapper.ts` and `search-result.mapper.ts` (precedent: `subclassPathId` already lives in metadata).
- Add `AdminCardService.createCard(cardType, body)` → `http.post(this.getEndpoint(cardType), body, { withCredentials: true })`. Add a comment noting the service is now used outside `/admin`.

Verify: `npm run lint`, `npm run test:only -- 'src/app/shared/**'`

### B2. All/Official/Custom dropdowns (Codex + Admin Portal)
- `filter-rail.ts`: **replace** the `{ kind: 'checkbox', key: 'isOfficial', ... }` entries (≈10 occurrences across `UNIVERSAL_FILTERS`/`TYPE_FILTERS`) with a select: options All(`''`)/Official(`'true'`)/Custom(`'false'`), reusing existing `SelectFilter` plumbing. `reference.ts` default `{ isOfficial: true }` stays (public default = Official).
- `card-search.ts` (admin): add `visibilityFilter = signal<ItemVisibilityFilter>('all')` + a `<select>` next to search; translate to `isOfficial: true|false|undefined` for `SearchService.search`/`CodexBrowseService.browse`; reset to `'all'` on category switch.

Verify: `npm run lint`, `npm run test:only -- 'src/app/features/reference/**' 'src/app/features/admin/card-search/**'`

### B3. Shared `ItemForm` extraction (highest-risk phase — admin regression)
- `utils/card-edit-form.utils.ts`: allow `buildFormFromSchema(schema, raw: RawCardResponse | null, fb)` (null → per-field defaults); add `mode: 'create'|'edit'` to `buildPayloadFromSchema` (create = full payload, edit = existing dirty-diff behavior unchanged).
- New `src/app/shared/components/item-form/` component: inputs `cardType`, `mode`, `initialData`, `saving`, `submitted`, `showIsOfficialField`; output `saved(payload)`. Composes existing `CardEditField`, `CardEditPreview`, `AddExpansionDialog`. Filters out the `isOfficial` field at render time when `showIsOfficialField` is false. Scope: Basics/Details/Damage sections only — `CardEditFeatures` stays in admin `CardEdit`.
- Refactor `card-edit.ts` to delegate rendering to `ItemForm` (mode='edit'), keeping its toolbar/delete/features wrapper. Admin edit flow must behave identically — run full admin spec suite.

Verify: `npm run lint`, `npm run test:only -- 'src/app/features/admin/**' 'src/app/shared/components/item-form/**'`, `npm run build`

### B4. Create Item page + navbar
- New feature `src/app/features/create-item/`: shell `create-item.ts` (signals `step: 'type'|'form'`, `itemType`), child `components/item-type-selector/` (3 cards, `role="radiogroup"`/`radio`, keyboard accessible), step 2 renders `<app-item-form mode="create">`; on `saved`, call `AdminCardService.createCard`; success state offers "View in Reference" / "Create another".
- Route in `app.routes.ts`: lazy `{ path: 'create-item', loadComponent: ... }` (same guard posture as `create-character`).
- `navbar.ts`/`.html`: `onCreateItem()` + "+ Item" button in desktop dropdown and mobile menu.

Verify: `npm run lint`, `npm run test:only -- 'src/app/features/create-item/**' 'src/app/layout/navbar/**'`

### B5. Ownership edit affordance
- `DaggerheartCard`: new `showEditAffordance` input; renders a real `<button aria-label="Edit {{name}}">`; parent computes `authService.user()?.id === metadata['createdByUserId'] || authService.isPrivileged()`. Client check is UX only; server is the boundary.
- Extend create-item routing with edit mode: `create-item/:itemType/:id` → same shell, `ItemForm` mode='edit', load via `AdminCardService.getCard`, save via `updateCard`.

Verify: `npm run lint`, `npm run test:only -- 'src/app/shared/components/daggerheart-card/**' 'src/app/features/create-item/**' 'src/app/features/reference/**'`

### B6. Inventory-add-panel search + official toggle
- `inventory-add-panel.ts`: add debounced (250ms, min 3 chars) `searchQuery` signal + `officialOnly = signal(true)` checkbox ("Official items only", checked by default).
- Dual-mode: empty query → existing `getXRaw` calls extended with `isOfficial` param; query ≥3 chars → `SearchService.search({ q, types: [type], isOfficial: officialOnly() || undefined })`.
- Split into per-type child list components only if the file passes ~150 lines.

Verify: `npm run lint`, `npm run test:only -- 'src/app/features/character-sheet/**'`, final `npm run build`

---

## End-to-end verification
1. Backend: `./mvnw test` all green.
2. Frontend: `npm run lint`, `npm run test:run`, `npm run build` all green (modulo pre-existing known failures).
3. Manual flow with backend running (`localhost:8080`) + `npm start`:
   - As a plain USER: Create → "+ Item" → pick Weapon → fill form → save → item appears in Reference with visibility filter set to All or Custom.
   - Same user sees an Edit button on their item; another USER does not; a MODERATOR does. Editing an official item as USER/MODERATOR → 403.
   - Character sheet → add item panel → search finds the custom item only after unchecking "Official items only".
   - Admin portal → All/Official/Custom dropdown filters correctly; admin can edit and soft-delete the custom item.
