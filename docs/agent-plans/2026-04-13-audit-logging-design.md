# Audit Logging Design

**Date:** 2026-04-13
**Branch:** improved_logging

## Context

The application needs structured, consistent logging for debugging and future New Relic metrics integration. Current state: 28/30 services have `@Slf4j` with ad-hoc `log.info()` calls, but they lack consistent formatting, user identity context, and standardized action names.

## Goals

1. Structured log format with bracketed metadata: `[user_id: 42; username: mwaboff; role: owner; campaign_id: 3] Campaign created: "The Adventures in Ostea" (campaign_id: 3)`
2. Every log includes acting user's ID, username, and role
3. Standardized action names via enum (for future New Relic metric mapping)
4. Controller-level request/completion logging with IP address and request duration
5. All content services receive `Authentication` parameter for user context

## Approach

### New Infrastructure (3 files)

#### 1. `model/enums/AuditAction.java`
Enum defining all auditable actions with display labels. Grouped by domain:
- **Authentication:** USER_LOGIN, USER_LOGOUT, USER_TOKENS_INVALIDATED
- **User Management:** USER_PROFILE_UPDATED, USER_USERNAME_CHOSEN, USER_ACCOUNT_DELETED, USER_PROVISIONED
- **Campaign:** CAMPAIGN_CREATED, CAMPAIGN_UPDATED, CAMPAIGN_DELETED, CAMPAIGN_ENDED, CAMPAIGN_GM_ADDED, CAMPAIGN_GM_REMOVED, CAMPAIGN_PLAYER_ADDED, CAMPAIGN_PLAYER_KICKED, CAMPAIGN_PLAYER_LEFT, CAMPAIGN_INVITE_GENERATED, CAMPAIGN_JOINED_VIA_INVITE, CAMPAIGN_CHARACTER_SUBMITTED, CAMPAIGN_CHARACTER_APPROVED, CAMPAIGN_CHARACTER_REJECTED, CAMPAIGN_NPC_ADDED, CAMPAIGN_CHARACTER_REMOVED
- **Character Sheet:** CHARACTER_CREATED, CHARACTER_UPDATED, CHARACTER_DELETED, CHARACTER_LEVELED_UP, CHARACTER_LEVEL_UNDONE
- **Companion:** COMPANION_CREATED, COMPANION_UPDATED, COMPANION_DELETED
- **Experience:** EXPERIENCE_CREATED, EXPERIENCE_UPDATED, EXPERIENCE_DELETED
- **Adversary:** ADVERSARY_CREATED, ADVERSARY_BATCH_CREATED, ADVERSARY_UPDATED, ADVERSARY_DELETED, ADVERSARY_RESTORED, ADVERSARY_COPIED
- **Encounter:** ENCOUNTER_CREATED, ENCOUNTER_UPDATED, ENCOUNTER_DELETED, ENCOUNTER_RESTORED, ENCOUNTER_COPIED, ENCOUNTER_ADVERSARY_ADDED, ENCOUNTER_ADVERSARY_REMOVED
- **Game Content (shared):** CONTENT_CREATED, CONTENT_BATCH_CREATED, CONTENT_UPDATED, CONTENT_DELETED, CONTENT_RESTORED

#### 2. `model/AuditContext.java`
Builder for structured metadata block. Formats to: `[user_id: 42; username: mwaboff; role: owner; campaign_id: 3]`

API:
```java
AuditContext.forUser(authentication)              // extracts user_id, username, role
    .withCampaignId(3L)                            // optional contextual fields
    .withIp(request.getRemoteAddr())               // for controller layer
    .build();

AuditContext.forIp(request.getRemoteAddr())        // fallback when no auth available
    .build();
```

#### 3. `service/AuditLogger.java`
Spring `@Component` that formats and writes audit logs.

```java
// Service-level audit:
auditLogger.log(AuditAction.CAMPAIGN_CREATED, ctx, "\"The Adventures in Ostea\" (campaign_id: 3)");

// Controller request received:
auditLogger.requestReceived(ctx, "POST", "/api/dh/campaigns");

// Controller request completed (with duration):
auditLogger.requestCompleted(ctx, "POST", "/api/dh/campaigns", startTime, "campaign_id: 3");
```

Duration is calculated from `System.nanoTime()` captured at controller method entry.

### Service Layer Changes

#### Services already with Authentication (7) — replace existing logs:
- `AuthenticationService` — login, logout, token invalidation
- `UserService` — profile update, username choice, account deletion
- `OAuth2UserProvisioningService` — user provisioning
- `CampaignService` — all 16+ campaign operations
- `CharacterSheetService` — create, update, delete
- `LevelUpService` — level up (with advancement choices), undo
- `CompanionService` — create, update, delete
- `ExperienceService` — create, update, delete

#### Services needing Authentication param added (15) — add param + audit logging:
- `AdversaryService` (already has Auth, just replace logs)
- `EncounterService` (already has Auth, just replace logs)
- `WeaponService`, `ArmorService`, `LootService`
- `DomainService`, `ClassService`, `ExpansionService`
- `AncestryCardService`, `CommunityCardService`, `SubclassCardService`, `DomainCardService`
- `SubclassPathService`, `FeatureService`, `FeatureModifierService`
- `CardCostTagService`, `QuestionService`

### Controller Layer Changes

All controllers get request/completion logging:
- Capture `long startTime = System.nanoTime()` at method entry
- Log request received: `[user_id: 42; username: mwaboff; ip: 10.0.0.1] POST /api/dh/campaigns — request received`
- Log completion with duration: `[user_id: 42; username: mwaboff; ip: 10.0.0.1] POST /api/dh/campaigns — completed (campaign_id: 3) in 45ms`
- When no Authentication available (e.g., logout), use IP address: `[ip: 10.0.0.1] POST /api/auth/logout — request received`

Controllers to update:
- `AuthController`, `DevAuthController`
- `UserController`, `AdminController`
- All dh/ controllers: `CharacterSheetController`, `CampaignController`, `AdversaryController`, `EncounterController`, `WeaponController`, `ArmorController`, `LootController`, `DomainController`, `ClassController`, `ExpansionController`, `AncestryCardController`, `CommunityCardController`, `SubclassCardController`, `DomainCardController`, `SubclassPathController`, `FeatureController`, `FeatureModifierController`, `CardCostTagController`, `QuestionController`, `CompanionController`, `ExperienceController`

## Testing Strategy

- **New unit tests:** `AuditLoggerTest`, `AuditContextTest` — verify log format with various context combinations
- **Existing service tests:** Update to pass `Authentication` mock where new params were added
- **Existing integration tests:** Should pass unchanged (controllers already receive Authentication from Spring Security)
- **No new integration tests needed** — logging is observational; existing endpoint tests cover the code paths

## Log Level Guidelines

| Layer | Level | What |
|-------|-------|------|
| Controller | DEBUG | Request received / completed with duration |
| Service | INFO | State-changing actions (create, update, delete, level up, join, etc.) |
| Service | WARN | Partial failures (batch create with some failures) |

## Future: New Relic Integration

The `AuditLogger` service is designed to be the single point where New Relic metrics can be added later. When ready:
1. Inject New Relic agent into `AuditLogger`
2. Map `AuditAction` enum values to metric names
3. Emit metrics alongside log lines — no changes needed in services or controllers
