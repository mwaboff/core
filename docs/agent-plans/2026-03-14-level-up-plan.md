# Character Leveling System - Implementation Plan

## Context

The Daggerheart TTRPG application needs a character leveling system. Characters progress through levels 1-10 across 4 tiers (Tier 1: L1, Tier 2: L2-4, Tier 3: L5-7, Tier 4: L8-10). Each level-up involves tier achievements, advancement choices, damage threshold increases, and domain card selection. Players also need the ability to undo their most recent level-up.

Currently the `CharacterSheet` entity has no `proficiency` field, domain cards are a flat ManyToMany with no vault/equipped distinction, and there is no advancement tracking.

---

## Phase 1: Database Migrations

All created via `./scripts/create-migration.sh`.

### Migration 1: Add proficiency to character_sheets

```sql
ALTER TABLE character_sheets ADD COLUMN proficiency INTEGER NOT NULL DEFAULT 1;
ALTER TABLE character_sheets ADD CONSTRAINT check_proficiency_positive CHECK (proficiency >= 1);
```

### Migration 2: Add equipped + entity columns to character_sheet_domain_cards

Converts the existing flat join table into a proper entity table:

```sql
ALTER TABLE character_sheet_domain_cards DROP CONSTRAINT character_sheet_domain_cards_pkey;
ALTER TABLE character_sheet_domain_cards ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_domain_cards ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_domain_cards ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_domain_cards ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_domain_cards ADD CONSTRAINT uq_cs_domain_card UNIQUE (character_sheet_id, domain_card_id);
```

### Migration 3: Create character_advancement_log table

```sql
CREATE TABLE character_advancement_log (
    id BIGSERIAL PRIMARY KEY,
    character_sheet_id BIGINT NOT NULL,
    from_level INTEGER NOT NULL,
    to_level INTEGER NOT NULL,
    tier INTEGER NOT NULL,
    advancement_data TEXT NOT NULL,  -- JSON: full snapshot of choices and previous values for undo
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_advancement_log_cs FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT check_adv_from_level CHECK (from_level >= 1 AND from_level <= 9),
    CONSTRAINT check_adv_to_level CHECK (to_level >= 2 AND to_level <= 10),
    CONSTRAINT check_adv_to_gt_from CHECK (to_level = from_level + 1),
    CONSTRAINT check_adv_tier CHECK (tier >= 2 AND tier <= 4)
);
CREATE INDEX idx_advancement_log_cs ON character_advancement_log(character_sheet_id);
CREATE INDEX idx_advancement_log_tier ON character_advancement_log(character_sheet_id, tier);
```

---

## Phase 2: New Enum

### `AdvancementType.java`
**File:** `src/main/java/com/aboff/core/model/enums/AdvancementType.java`

Values:
- `BOOST_TRAITS` — +1 to two unmarked traits, mark them
- `GAIN_HP` — +1 hit point max
- `GAIN_STRESS` — +1 stress max
- `BOOST_EXPERIENCES` — +1 modifier to two experiences
- `GAIN_DOMAIN_CARD` — Choose a domain card of appropriate level
- `BOOST_EVASION` — +1 evasion
- `UPGRADE_SUBCLASS` — Take upgraded subclass card (Tier 3+ only)
- `BOOST_PROFICIENCY` — +1 proficiency (Tier 3+ only)
- `MULTICLASS` — Choose additional class (Tier 3+ only)

Each value should have a `description` field and a `minTier` field (2 for base advancements, 3 for the last three).

---

## Phase 3: New and Modified Entities

### 3a. New: `CharacterSheetDomainCard`
**File:** `src/main/java/com/aboff/core/model/entity/dh/CharacterSheetDomainCard.java`

Extends `BaseEntity`. Fields:
- `characterSheet` (ManyToOne to CharacterSheet, required)
- `domainCard` (ManyToOne to DomainCard, required)
- `equipped` (Boolean, default false)

### 3b. New: `CharacterAdvancementLog`
**File:** `src/main/java/com/aboff/core/model/entity/dh/CharacterAdvancementLog.java`

Extends `BaseEntity`. Fields:
- `characterSheet` (ManyToOne, required)
- `fromLevel` (Integer, required)
- `toLevel` (Integer, required)
- `tier` (Integer, required)
- `advancementData` (String/TEXT, required) — JSON blob storing both the choices made AND previous values (for undo). Structure described in Phase 6.

### 3c. Modify: `CharacterSheet`
**File:** `src/main/java/com/aboff/core/model/entity/dh/CharacterSheet.java`

Changes:
- **Add** `proficiency` field: `@Column(nullable = false) @Builder.Default private Integer proficiency = 1;`
- **Replace** the `@ManyToMany domainCards` with `@OneToMany(mappedBy = "characterSheet", cascade = ALL, orphanRemoval = true) Set<CharacterSheetDomainCard> characterSheetDomainCards`
- **Add** `@OneToMany(mappedBy = "characterSheet", cascade = ALL, orphanRemoval = true) Set<CharacterAdvancementLog> advancementLogs` (for convenience/cascade)

---

## Phase 4: New Repositories

### `CharacterSheetDomainCardRepository`
**File:** `src/main/java/com/aboff/core/repository/dh/CharacterSheetDomainCardRepository.java`

Methods:
- `findByCharacterSheetId(Long)`
- `findByCharacterSheetIdAndEquipped(Long, Boolean)`
- `findByCharacterSheetIdAndDomainCardId(Long, Long)`
- `countEquippedByCharacterSheetId(Long)` (custom @Query)

### `CharacterAdvancementLogRepository`
**File:** `src/main/java/com/aboff/core/repository/dh/CharacterAdvancementLogRepository.java`

Methods:
- `findByCharacterSheetIdOrderByToLevelAsc(Long)`
- `findByCharacterSheetIdAndTier(Long, Integer)`
- `findTopByCharacterSheetIdOrderByToLevelDesc(Long)` — for undo (get most recent)

---

## Phase 5: DTOs

### 5a. Request: `AdvancementChoice`
**File:** `src/main/java/com/aboff/core/model/dto/dh/request/AdvancementChoice.java`

Fields:
- `type` (AdvancementType, @NotNull)
- `boostTraits` (List<Trait>) — for BOOST_TRAITS: exactly 2 unmarked traits
- `boostExperienceIds` (List<Long>) — for BOOST_EXPERIENCES: exactly 2 experience IDs
- `domainCardId` (Long) — for GAIN_DOMAIN_CARD
- `equipDomainCard` (Boolean, default false) — for GAIN_DOMAIN_CARD
- `subclassCardId` (Long) — for UPGRADE_SUBCLASS
- `multiclassSubclassPathId` (Long) — for MULTICLASS
- `multiclassFoundationCardId` (Long) — for MULTICLASS

### 5b. Request: `DomainCardTradeRequest`
**File:** `src/main/java/com/aboff/core/model/dto/dh/request/DomainCardTradeRequest.java`

Fields:
- `tradedOutDomainCardIds` (List<Long>, @NotEmpty) — cards to give up
- `tradedInDomainCardIds` (List<Long>, @NotEmpty) — cards to receive (must be same count)
- `equipTradedInCardIds` (List<Long>) — subset of tradedIn to equip (optional)

### 5c. Request: `LevelUpRequest`
**File:** `src/main/java/com/aboff/core/model/dto/dh/request/LevelUpRequest.java`

Fields:
- `advancements` (List<AdvancementChoice>, @Size(min=2, max=2), @Valid)
- `newExperienceDescription` (String) — required at tier transitions (levels 2, 5, 8)
- `newDomainCardId` (Long, @NotNull) — Step 4 new card
- `equipNewDomainCard` (Boolean, default false)
- `unequipDomainCardId` (Long) — optional, to make room when at 5 equipped
- `trades` (List<DomainCardTradeRequest>) — optional, equal-swap trades (can be multiple trade pairs)

### 5d. Response: `LevelUpOptionsResponse`
**File:** `src/main/java/com/aboff/core/model/dto/dh/response/LevelUpOptionsResponse.java`

Fields:
- `currentLevel`, `nextLevel`, `currentTier`, `nextTier`
- `isTierTransition` (Boolean)
- `availableAdvancements` (List<AvailableAdvancement>) — each with: type, description, limitPerTier, usedInTier, remaining, mutuallyExclusiveWith
- `domainCardLevelCap` (Integer, null = uncapped)
- `accessibleDomainIds` (List<Long>)
- `equippedDomainCardCount`, `maxEquippedDomainCards` (always 5)

### 5e. Response: `LevelUpResponse`
**File:** `src/main/java/com/aboff/core/model/dto/dh/response/LevelUpResponse.java`

Fields:
- `characterSheet` (CharacterSheetResponse)
- `advancementLogId` (Long)
- `appliedChanges` (List<String>) — human-readable summary

### 5f. Modify: `CharacterSheetResponse`
**File:** `src/main/java/com/aboff/core/model/dto/dh/response/CharacterSheetResponse.java`

Add:
- `proficiency` (Integer)
- `equippedDomainCardIds` (List<Long>) — always included
- `vaultDomainCardIds` (List<Long>) — always included
- Keep existing `domainCardIds` as union of both (backward compatibility)
- `equippedDomainCards` / `vaultDomainCards` — when expanded

### 5g. Modify: `CreateCharacterSheetRequest` and `UpdateCharacterSheetRequest`
- Add `proficiency` field
- On creation, `domainCardIds` default all to equipped (characters start with ≤5 cards)
- Update request may need `domainCardAssignments` (list of {domainCardId, equipped}) for direct management outside level-up

---

## Phase 6: Service Logic

### New: `LevelUpService`
**File:** `src/main/java/com/aboff/core/service/dh/LevelUpService.java`

#### Key Methods:

| Method | Description |
|--------|-------------|
| `getLevelUpOptions(id, auth)` | Returns available options for next level-up |
| `levelUp(id, request, auth)` | Performs level-up atomically |
| `undoLevelUp(id, auth)` | Reverses most recent level-up |

#### Tier Calculation:
```
Level 1 → Tier 1
Levels 2-4 → Tier 2
Levels 5-7 → Tier 3
Levels 8-10 → Tier 4
```

#### Domain Card Level Cap:
- Tier 2: ≤ 4
- Tier 3: ≤ 7
- Tier 4: uncapped

#### Advancement Limits per Tier:

| Advancement | Tier 2 | Tier 3 | Tier 4 |
|------------|--------|--------|--------|
| BOOST_TRAITS | 3 | 3 | 3 |
| GAIN_HP | 2 | 2 | 2 |
| GAIN_STRESS | 2 | 2 | 2 |
| BOOST_EXPERIENCES | 1 | 1 | 1 |
| GAIN_DOMAIN_CARD | 1 | 1 | 1 |
| BOOST_EVASION | 1 | 1 | 1 |
| UPGRADE_SUBCLASS | — | * | * |
| BOOST_PROFICIENCY | — | 2 | 2 |
| MULTICLASS | — | * | * |

\* UPGRADE_SUBCLASS and MULTICLASS are mutually exclusive within a tier. If one is chosen at any point in the tier, the other becomes unavailable for the rest of that tier. Limit for each is effectively the number of level-ups in the tier (3), but mutual exclusion is the real constraint.

#### Level-Up Execution Order:
1. Validate character (exists, not deleted, not level 10, access control)
2. Calculate nextLevel, currentTier, nextTier
3. Load tier advancement usage, validate entire request
4. **Step 1 — Tier Achievements** (if entering Tier 2/3/4):
    - Create new Experience with +2 modifier
    - Increment proficiency by 1
    - At levels 5 and 8: clear all marked traits
5. **Step 2 — Apply 2 Advancements**
6. **Step 3 — Damage Thresholds**: +1 to both major and severe
7. **Step 4 — Domain Card**: Add new card, process trades (equal swaps)
8. Increment character level
9. Save CharacterSheet
10. Save CharacterAdvancementLog (JSON stores choices + previous values for undo)
11. Return LevelUpResponse

#### Advancement Data JSON Structure (for undo support):
```json
{
  "advancements": [
    { "type": "BOOST_TRAITS", "traits": ["AGILITY", "STRENGTH"] },
    { "type": "GAIN_HP" }
  ],
  "tierAchievements": {
    "experienceCreatedId": 42,
    "proficiencyIncremented": true,
    "traitsCleared": true,
    "previousTraitMarks": { "AGILITY": true, "STRENGTH": false, ... }
  },
  "previousDamageThresholds": { "major": 5, "severe": 9 },
  "newDomainCard": { "domainCardId": 10, "equipped": true },
  "trades": [
    { "outIds": [3, 7], "inIds": [5, 8], "inEquipped": [5] }
  ],
  "previousValues": {
    "proficiency": 2,
    "evasion": 3,
    "hitPointMax": 7,
    "stressMax": 6,
    "experienceModifiers": { "15": 2, "16": 2 }
  }
}
```

#### Undo Level-Up Logic (`undoLevelUp`):

**Supports repeated undo:** The endpoint can be called multiple times in a row to undo multiple levels (e.g., level 7 → 6 → 5 → 4). Each call pops the most recent log entry and reverses it. The only constraint is that at least one advancement log entry must exist (cannot undo below the character's original starting level).

1. Find most recent advancement log for the character (error if none exists)
2. Verify character's current level matches `toLevel` in the log
3. Deserialize the JSON to get previous values
4. Reverse all changes:
    - Decrement level
    - Restore damage thresholds from snapshot
    - Reverse each advancement (decrement HP/stress/evasion/proficiency, unmark and decrement traits, remove experience modifier bonuses, remove gained cards/subclass cards)
    - If tier transition: delete the created experience, decrement proficiency, restore trait marked states
    - **Reverse domain card trades** (remove all traded-in cards, re-add all traded-out cards with their previous equipped state — the JSON stores full trade details including which cards were traded out, traded in, and their equipped status)
    - Remove the new domain card added in Step 4
5. Delete the advancement log entry
6. Save character sheet

#### Validation Rules:
1. Character not at max level (10)
2. Exactly 2 advancements provided
3. Each advancement type available in target tier (minTier check)
4. Each advancement type has remaining usage in tier
5. If both advancements same type: must have ≥2 remaining
6. UPGRADE_SUBCLASS and MULTICLASS mutually exclusive within a tier
7. BOOST_TRAITS: exactly 2 traits, both currently unmarked
8. BOOST_EXPERIENCES: exactly 2 experience IDs, belong to character
9. GAIN_DOMAIN_CARD: card from accessible domain, within level cap
10. UPGRADE_SUBCLASS: card is next level in a path character already has
11. MULTICLASS: path belongs to a class character doesn't already have; card is FOUNDATION level from that path
12. Trades: equal count in/out, traded-out cards belong to character, traded-in cards from accessible domains
13. Equipped count ≤5 after all operations
14. Tier transitions require `newExperienceDescription`
15. New domain card (Step 4) from accessible domain, within level cap

### Modify: `CharacterSheetService`
**File:** `src/main/java/com/aboff/core/service/dh/CharacterSheetService.java`

- Make `validateAccess` package-private for reuse by `LevelUpService`
- Update `toResponse()`: add proficiency, map domain cards from `characterSheetDomainCards` (equipped/vault split)
- Update `createCharacterSheet()`: handle proficiency, create `CharacterSheetDomainCard` entities (default equipped)
- Update `updateCharacterSheet()`: handle proficiency, handle domain card assignments if provided

---

## Phase 7: Controller Endpoints

### Modify: `CharacterSheetController`
**File:** `src/main/java/com/aboff/core/controller/dh/CharacterSheetController.java`

Add `LevelUpService` dependency and three new endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{id}/level-up-options` | Returns available advancement options |
| POST | `/{id}/level-up` | Performs level-up |
| DELETE | `/{id}/level-up` | Undoes most recent level-up |

---

## Phase 8: Tests

### Unit Tests

**`LevelUpServiceTest`** — `src/test/java/com/aboff/core/service/dh/LevelUpServiceTest.java`
- Tier calculation for all levels
- Tier transition detection
- Domain card level cap by tier
- Accessible domain resolution via subclass paths
- Advancement usage counting from log entries
- All validation rules (happy path + error cases)
- Each advancement type application
- Tier achievement application (experience creation, proficiency increment, trait clearing)
- Damage threshold increment
- Domain card addition with equipped/vault logic
- Multi-card equal-swap trade validation and application
- Equipped limit enforcement (max 5)
- Undo: full reversal of a level-up at non-tier boundary
- Undo: full reversal of a level-up at tier boundary (experience deleted, proficiency restored, traits re-marked)
- Undo: domain card trades fully reversed (traded-in cards removed, traded-out cards restored with previous equipped state)
- Undo: repeated undo across multiple levels (e.g., 7 → 6 → 5)
- Undo: error when no previous level-up exists
- getLevelUpOptions: correct remaining counts and mutual exclusions

**Update `CharacterSheetServiceTest`** — verify proficiency in toResponse(), domain card mapping changes

### Integration Tests

**`LevelUpControllerIntegrationTest`** — `src/test/java/com/aboff/core/controller/dh/LevelUpControllerIntegrationTest.java`
- GET level-up-options returns correct structure
- POST level-up at non-tier boundary succeeds
- POST level-up at tier boundary succeeds (experience created, proficiency up)
- POST level-up with invalid advancements returns 400
- POST level-up at max level returns 400
- DELETE level-up (undo) succeeds
- DELETE level-up with no history returns 400
- Access control: owner can level up, non-owner non-mod gets 403

**Update `CharacterSheetControllerIntegrationTest`** — ensure existing tests pass with proficiency and domain card entity changes

---

## Phase 9: Update API Blueprint

After implementation is complete, update the `.api-blueprint/` documentation to reflect the new endpoints and models.

### Update: `.api-blueprint/SKILL.md`
- Update total endpoint count (160 → 163)
- Add level-up endpoints to the Character Sheets section table
- Update expand fields to include `equippedDomainCards`, `vaultDomainCards`

### Update: `.api-blueprint/references/character-sheets-api.md`
- Add three new endpoint entries:
    - `GET /api/dh/character-sheets/{id}/level-up-options`
    - `POST /api/dh/character-sheets/{id}/level-up`
    - `DELETE /api/dh/character-sheets/{id}/level-up`
- Update CharacterSheetResponse model to include `proficiency`, `equippedDomainCardIds`, `vaultDomainCardIds`
- Add new request/response model documentation: LevelUpRequest, AdvancementChoice, DomainCardTradeRequest, LevelUpOptionsResponse, LevelUpResponse

### Update: `.api-blueprint/references/shared-models.md`
- Add `AdvancementType` enum documentation with all 9 values and their descriptions/minTier

---

## Phase 10: Documentation

### Level-Up Process Document
**File:** `docs/levelup-process.md`

A standalone document explaining the complete level-up process with all rules, tiers, advancements, limits, and interactions. Written for another agent to use when planning frontend implementation. Includes:
- Tier/level structure
- Step-by-step level-up flow
- Tier achievements at tier transitions
- All advancement types with per-tier limits
- Mutual exclusion rules (UPGRADE_SUBCLASS vs MULTICLASS)
- Domain card vault/equipped system (max 5 equipped)
- Domain card trading rules (equal swaps)
- Damage threshold increases
- Undo/level-down mechanics
- API endpoint reference (request/response shapes)

---

## Implementation Order

1. Migrations (1, 2, 3)
2. AdvancementType enum
3. CharacterSheetDomainCard entity + repository
4. CharacterAdvancementLog entity + repository
5. CharacterSheet entity modifications (proficiency + domain card relationship)
6. DTO changes (request and response)
7. CharacterSheetService updates (toResponse, create, update for proficiency + domain cards)
8. LevelUpService (getLevelUpOptions, levelUp, undoLevelUp)
9. CharacterSheetController (new endpoints)
10. Update existing tests for CharacterSheet changes
11. New tests for LevelUpService and controller
12. Update API Blueprint (SKILL.md, character-sheets-api.md, shared-models.md)
13. Write level-up process document (docs/levelup-process.md)
14. Write finalized design plan (docs/agent-plans/2026-03-14-character-leveling-design.md)

---

## Verification

1. Run `./mvnw test` — all existing tests pass after entity/service refactoring
2. Run new unit tests — all LevelUpService logic covered
3. Run new integration tests — endpoints return correct responses
4. Manual verification: start app (`./mvnw spring-boot:run`), hit GET level-up-options, POST level-up, DELETE level-up via curl/Postman