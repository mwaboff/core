# Companions WP4 — GM companion-access flag

## Context

Work Package 4 of the Companions feature (full plan:
`dawn/.agents/plans/companions/companions-implementation-plan.md`, §5.3, §3.4, §3.11).
`CharacterSheet.companionsEnabled` and its DB column already exist from WP1. This work
package adds the campaign-scoped GM endpoint that flips it, cloning the existing
transformation-access chain (`CampaignController.updateTransformationAccess` /
`CampaignService.updateTransformationAccess`) exactly.

Task is fully specified by the team lead's brief and the plan doc — no open design
questions, so this doc records the scope rather than proposing alternatives.

## Scope

1. `UpdateCompanionAccessRequest { @NotNull Boolean enabled }` — **done** (pre-existing in
   worktree from an earlier session).
2. `AuditAction.CAMPAIGN_COMPANION_ACCESS_UPDATED` — **done**.
3. `CampaignCharacterSummaryResponse.companionsEnabled` — **done**.
4. `CampaignService.updateCompanionAccess` mirroring `updateTransformationAccess`:
   `validateGameMasterAccess` → `validateNotEnded` → `findCharacterSheetInCampaign` → set
   flag → audit — **done**.
5. `PUT /api/dh/campaigns/{id}/character-sheets/{sheetId}/companions` on
   `CampaignController` — **remaining**, clone `updateTransformationAccess` controller
   method exactly (audit request/response logging, `@Valid @RequestBody`).
6. Update `.api-blueprint/references/campaigns-api.md` with the new endpoint — **remaining**.
7. Tests — **remaining**:
   - `CampaignServiceTest`: happy path, non-GM forbidden, ended-campaign rejected, audit
     record written.
   - `CampaignControllerIntegrationTest`: same cases end-to-end through the real endpoint.

## The one deliberate divergence from transformations

`companionsEnabled` gates only whether *creating a new* companion is offered. It must
never hide, disable, or orphan a companion that already exists. No player-side write gate
is added — this is a GM-only campaign-scoped switch, same authorization primitives as
transformations (`hasGameMasterAccess` takes a `Campaign`, not a sheet — hence the
campaign-scoped endpoint, no new authorization infrastructure).

## Testing strategy

Mirror the existing transformation-access test coverage 1:1 (same fixtures, same
assertion shapes) so review can diff the two side by side. Target 80%+ coverage on the
new service/controller logic per `core/CLAUDE.md`.

## Gate

`./mvnw test` and `./mvnw verify` both green, run inside this worktree only.
