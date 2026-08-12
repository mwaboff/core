# Workstream I — Admin + Codex UI for SRD vs. Paid-Expansion Content Gating

## Context

Part of the larger "SRD vs. Paid-Expansion Content Gating" feature. Content only
in paid Daggerheart books (Hope & Fear, future expansions) is gated behind
ADMIN/OWNER or a per-user "Access All Expansions" grant. This workstream builds
the three human-facing controls:

1. An `srd` checkbox in the admin card editor (dawn) so admins can flag a card
   as SRD content.
2. An "Access All Expansions" per-user grant, plumbed end-to-end: backend
   request DTO → service → audit log → `UserResponse` (4 builder sites) →
   frontend `AuthService` → admin user-edit UI.
3. A tri-state SRD filter (`All content` / `SRD only` / `Expansion only`) in
   the Codex `FilterRail`, auto-injected and hidden for non-privileged users.

Full detailed spec was provided by the team lead (workstream owner of the
overall feature); this doc records it for the record per the planning skill's
Phase 4 requirement. The spec is already fully scoped down to exact files,
field names, and line numbers, so there is no open design decision requiring
interactive clarification — proceeding straight to implementation referencing
this document.

## Scope boundaries

- Branch `feat/srd-content-gating` in both `core/` and `dawn/`. No commits, no
  branch switches, no rebases — other agents are concurrently editing both
  repos.
- Stay inside the files enumerated below. Do not touch
  `features/admin/card-search/` or `card-table/` (owned by Workstream J —
  bulk SRD flagging). Do not edit `codex-registration.spec.ts`.
- Do not touch `character-sheet/`, `shared/components/daggerheart-card/`, or
  `shared/mappers/` even if `test:only` surfaces type errors there — those
  belong to other in-flight workstreams.

## Approach

### Part 1 — dawn: admin card-edit schema
- `features/admin/card-edit/schema/card-edit-schema.ts`: add an `srd`
  checkbox field to `BASICS_FIELDS_FULL` and to each of the 13 other inline
  `fields` arrays, except `expansion` (a sourcebook is not content — same
  reasoning as the existing comment near line 638).
- For `subclass`, render the checkbox disabled with a hint pointing at the
  path editor, since `subclass_paths.srd` is the single source of truth for
  the path + its 3 cards.
- `admin-card-types.consistency.spec.ts`: assert every schema key has an
  `srd` field or is listed in a new `SRD_FIELD_ALLOWED_OMISSIONS` allowlist
  with a reason, following the file's existing allowlist pattern.

### Part 2 — Access All Expansions grant
Backend (core):
- `UpdateAdminUserRequest.accessAllExpansions`: `Boolean` (nullable — null
  means "leave unchanged", never defaulted to false).
- `AdminUserService.updateUser`: apply the change when non-null, write an
  audit row using the existing `USER_EXPANSION_ACCESS_CHANGED` enum value (no
  migration needed — column, field, and CHECK constraint already exist).
- `UserResponse.accessAllExpansions`: set at all `UserResponse.builder()`
  call sites (expected: `AuthController`, `AuthenticationService`,
  `UserService`, `AdminUserService` — verify by grep, not by trusting stale
  line numbers).

Frontend (dawn):
- `AdminUserRecord` / `AdminUserPatchRequest`: add the field.
- `user-edit.ts`: add a control to the `fb.nonNullable.group`.
- `user-edit-identity-panel.html`: checkbox + hint copy noting the grant
  takes effect on the user's next `/api/auth/me` (no session revocation,
  unlike role changes).
- `AuthService.canSeeNonSrd = computed(() => isAdmin() || user()?.accessAllExpansions === true)`.

### Part 3 — Codex SRD filter (dawn)
- `SelectFilter` gains an optional `booleanValues?: true` marker;
  `onSelectChange` coerces to boolean when set instead of always doing
  `Number(value)`. Add a spec for the coercion.
- Auto-inject the tri-state SRD select into `FilterRail.activeControls`
  alongside the Expansion select (one shared code path, not per-type
  registration — leaves `codex-registration.spec.ts` untouched).
- `FilterRail` gains `canSeeNonSrd = input<boolean>(false)`; the control is
  omitted entirely when false.
- `reference.ts` passes `canSeeNonSrd` from `AuthService` into `FilterRail`,
  keeping `FilterRail` presentational.
- `shared/models/search.model.ts`: add `srd` to `SearchParams` /
  `SearchFilters`.
- `shared/services/search.service.ts` and the per-type services under
  `shared/services/` (enumerate by listing the directory, not by assuming a
  count): thread `srd` through to the query params.

## Testing

- `./mvnw -q compile && ./mvnw test -Dtest=AdminUserServiceTest` plus any
  test added/touched.
- dawn: `npm run lint`, `npm run test:only -- src/app/features/admin/`,
  `npm run test:only -- src/app/features/reference/`,
  `npm run test:only -- src/app/shared/`, `npm run build`.
- New/updated specs: consistency spec allowlist assertion, `SelectFilter`
  boolean coercion spec, any service-level spec for the new `srd` param.
- Do not run the full suite — other agents do that at the end.
