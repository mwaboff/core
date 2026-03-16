# Character Level-Up Process

Complete reference for the Daggerheart character leveling system. This document covers the tier/level structure, step-by-step level-up flow, all advancement types and limits, domain card management, and undo mechanics. Written for use by agents planning frontend implementation.

---

## Tier and Level Structure

Characters progress through levels 1-10 across 4 tiers:

| Level | Tier | Notes |
|-------|------|-------|
| 1 | 1 | Starting tier (no level-up from here triggers tier achievements for Tier 1) |
| 2 | 2 | Tier transition -- tier achievements apply |
| 3 | 2 | |
| 4 | 2 | |
| 5 | 3 | Tier transition -- tier achievements apply, marked traits cleared |
| 6 | 3 | |
| 7 | 3 | |
| 8 | 4 | Tier transition -- tier achievements apply, marked traits cleared |
| 9 | 4 | |
| 10 | 4 | Maximum level |

**Tier transitions** occur when leveling up to levels 2, 5, and 8. These trigger additional tier achievement steps before the standard advancement choices.

---

## Level-Up Flow (Step by Step)

Every level-up follows these steps in order:

### Step 1 -- Tier Achievements (Tier Transitions Only)

Only applies when entering a new tier (leveling to level 2, 5, or 8):

1. **Create a new Experience** with a +2 modifier. The player provides a description for this experience (the `newExperienceDescription` field in the request).
2. **Increment proficiency** by 1.
3. **Clear all marked traits** (levels 5 and 8 only). At levels 5 and 8, all six trait `marked` flags are reset to `false`. This does NOT happen at level 2 (entering Tier 2).

### Step 2 -- Choose 2 Advancements

The player selects exactly 2 advancements from the available options for the target tier. See the "Advancement Types" section below for all options, their effects, and per-tier limits.

### Step 3 -- Damage Thresholds

Both damage thresholds are incremented automatically:
- `majorDamageThreshold` += 1
- `severeDamageThreshold` += 1

### Step 4 -- New Domain Card

The player chooses one new domain card to add to their collection:
- The card must be from a domain accessible to the character (determined by their subclass paths).
- The card's level must be within the tier's domain card level cap.
- The player can optionally equip the new card (if equipped count is under 5, or if they also unequip another card).

### Optional -- Domain Card Trades

After the new domain card is added, the player may optionally perform one or more equal-swap trades:
- Each trade removes N cards from the character's collection and adds N different cards.
- Traded-in cards must be from accessible domains and within the level cap.
- The player can specify which traded-in cards to equip.
- Total equipped count must not exceed 5 after all operations.

### Final -- Level Increment

After all steps complete, the character's level is incremented by 1.

---

## Advancement Types

### Base Advancements (Available from Tier 2)

#### BOOST_TRAITS
- **Effect:** +1 modifier to two traits, mark both traits.
- **Required fields:** `boostTraits` (exactly 2 Trait values, both must currently be unmarked)
- **Per-tier limit:** 3

#### GAIN_HP
- **Effect:** +1 to `hitPointMax`.
- **Required fields:** None (type only)
- **Per-tier limit:** 2

#### GAIN_STRESS
- **Effect:** +1 to `stressMax`.
- **Required fields:** None (type only)
- **Per-tier limit:** 2

#### BOOST_EXPERIENCES
- **Effect:** +1 modifier to two existing experiences.
- **Required fields:** `boostExperienceIds` (exactly 2 experience IDs belonging to the character)
- **Per-tier limit:** 1

#### GAIN_DOMAIN_CARD
- **Effect:** Gain an additional domain card from an accessible domain.
- **Required fields:** `domainCardId`, optionally `equipDomainCard`
- **Per-tier limit:** 1

#### BOOST_EVASION
- **Effect:** +1 to `evasion`.
- **Required fields:** None (type only)
- **Per-tier limit:** 1

### Tier 3+ Advancements (Available from Tier 3)

#### UPGRADE_SUBCLASS
- **Effect:** Take the next-level subclass card in a path the character already has (e.g., Foundation to Specialization, Specialization to Mastery).
- **Required fields:** `subclassCardId` (the upgraded card)
- **Per-tier limit:** Effectively 3 (one per level in the tier), but mutually exclusive with MULTICLASS.

#### BOOST_PROFICIENCY
- **Effect:** +1 to `proficiency`.
- **Required fields:** None (type only)
- **Per-tier limit:** 2

#### MULTICLASS
- **Effect:** Choose an additional class by selecting a subclass path from a class the character doesn't already have, and take its Foundation-level card.
- **Required fields:** `multiclassSubclassPathId`, `multiclassFoundationCardId`
- **Per-tier limit:** Effectively 3 (one per level in the tier), but mutually exclusive with UPGRADE_SUBCLASS.

---

## Advancement Limits Per Tier

| Advancement | Tier 2 | Tier 3 | Tier 4 |
|-------------|--------|--------|--------|
| BOOST_TRAITS | 3 | 3 | 3 |
| GAIN_HP | 2 | 2 | 2 |
| GAIN_STRESS | 2 | 2 | 2 |
| BOOST_EXPERIENCES | 1 | 1 | 1 |
| GAIN_DOMAIN_CARD | 1 | 1 | 1 |
| BOOST_EVASION | 1 | 1 | 1 |
| UPGRADE_SUBCLASS | -- | * | * |
| BOOST_PROFICIENCY | -- | 2 | 2 |
| MULTICLASS | -- | * | * |

`--` = Not available in this tier.
`*` = UPGRADE_SUBCLASS and MULTICLASS are mutually exclusive within a tier. The limit for each is effectively the number of level-ups in the tier (3), but the mutual exclusion is the real constraint.

---

## Mutual Exclusion Rules

**UPGRADE_SUBCLASS** and **MULTICLASS** are mutually exclusive within a tier:
- If a character chooses UPGRADE_SUBCLASS at any level within a tier, MULTICLASS becomes unavailable for the rest of that tier (and vice versa).
- This resets at each tier boundary. A character who used UPGRADE_SUBCLASS in Tier 3 can use MULTICLASS in Tier 4.
- Both can appear as available options initially; the exclusion only kicks in once one is used.

---

## Domain Card System

### Equipped vs Vault

Domain cards are divided into two categories:
- **Equipped** (maximum 5): These are the character's active domain cards available for use during play.
- **Vault** (unlimited): These are stored domain cards not currently equipped but owned by the character.

The response always includes:
- `equippedDomainCardIds` -- IDs of equipped cards
- `vaultDomainCardIds` -- IDs of vault cards
- `domainCardIds` -- Union of both (for backward compatibility)

### Domain Card Level Caps

The maximum level of domain cards a character can acquire depends on the tier they are entering:

| Tier | Domain Card Level Cap |
|------|-----------------------|
| 2 | Level 4 or lower |
| 3 | Level 7 or lower |
| 4 | Uncapped |

### Accessible Domains

A character can only acquire domain cards from domains that are accessible to them. Accessible domains are determined by the character's subclass paths -- each subclass path grants access to specific domains.

### Trading Rules

During a level-up, after adding the new domain card (Step 4), the player can perform equal-swap trades:
- The number of cards traded out must exactly equal the number traded in.
- Cards being traded out must currently belong to the character.
- Cards being traded in must be from accessible domains and within the tier's level cap.
- Multiple trades can be performed in a single level-up.
- The player specifies which traded-in cards to equip; the total equipped count must not exceed 5 after all operations.

---

## Undo / Level-Down Mechanics

The system supports undoing level-ups via the `DELETE /api/dh/character-sheets/{id}/level-up` endpoint.

### How Undo Works

1. The system finds the most recent advancement log entry for the character.
2. It verifies the character's current level matches the `toLevel` in the log.
3. It deserializes the JSON snapshot stored in the log entry.
4. All changes from that level-up are reversed:
   - Character level is decremented.
   - Damage thresholds are restored from the snapshot.
   - Each advancement is reversed (e.g., HP decremented, traits unmarked and decremented, experience modifiers restored, gained cards removed).
   - If it was a tier transition: the created experience is deleted, proficiency is decremented, trait marked states are restored from the snapshot.
   - Domain card trades are reversed: traded-in cards are removed, traded-out cards are re-added with their previous equipped state.
   - The new domain card from Step 4 is removed.
5. The advancement log entry is deleted.
6. The character sheet is saved.

### Repeated Undo

The undo endpoint can be called multiple times in sequence to undo multiple level-ups:
- Example: Level 7 -> 6 -> 5 -> 4 (three successive calls)
- Each call pops the most recent log entry and reverses it.
- The only constraint is that at least one advancement log entry must exist (the character cannot be undone below their original starting level).

### What Gets Stored for Undo

Each level-up creates a `CharacterAdvancementLog` entry containing a JSON blob with:
- The advancements chosen (type and parameters)
- Tier achievement details (experience ID created, whether proficiency was incremented, whether traits were cleared, and the previous trait marked states)
- Previous damage thresholds
- The new domain card added and its equipped state
- Full trade details (which cards went out, which came in, and their equipped states)
- Previous values for all modified stats (proficiency, evasion, HP max, stress max, experience modifiers)

---

## API Endpoints

### GET /api/dh/character-sheets/{id}/level-up-options

Returns available advancement options for the character's next level-up.

**Authorization:** Any authenticated user (character owner or MODERATOR+).

**Response:** `LevelUpOptionsResponse`

```json
{
  "currentLevel": 4,
  "nextLevel": 5,
  "currentTier": 2,
  "nextTier": 3,
  "isTierTransition": true,
  "availableAdvancements": [
    {
      "type": "BOOST_TRAITS",
      "description": "+1 to two unmarked traits, mark them",
      "limitPerTier": 3,
      "usedInTier": 1,
      "remaining": 2,
      "mutuallyExclusiveWith": null
    }
  ],
  "domainCardLevelCap": 7,
  "accessibleDomainIds": [1, 3],
  "equippedDomainCardCount": 4,
  "maxEquippedDomainCards": 5
}
```

### POST /api/dh/character-sheets/{id}/level-up

Performs a level-up with the given choices.

**Authorization:** Character owner or MODERATOR+.

**Request:** `LevelUpRequest`

```json
{
  "advancements": [
    {
      "type": "BOOST_TRAITS",
      "boostTraits": ["AGILITY", "STRENGTH"]
    },
    {
      "type": "GAIN_HP"
    }
  ],
  "newExperienceDescription": "Defeated the Shadow King",
  "newDomainCardId": 15,
  "equipNewDomainCard": true,
  "unequipDomainCardId": 8,
  "trades": [
    {
      "tradedOutDomainCardIds": [3],
      "tradedInDomainCardIds": [12],
      "equipTradedInCardIds": []
    }
  ]
}
```

**Response:** `LevelUpResponse`

```json
{
  "characterSheet": { "id": 1, "level": 5, "proficiency": 2, "..." : "full CharacterSheetResponse" },
  "advancementLogId": 4,
  "appliedChanges": [
    "Tier transition: created experience 'Defeated the Shadow King' (+2), proficiency +1, cleared marked traits",
    "Applied BOOST_TRAITS: +1 AGILITY (marked), +1 STRENGTH (marked)",
    "Applied GAIN_HP: +1 hit point max (now 11)",
    "Damage thresholds: +1 major (now 4), +1 severe (now 7)",
    "Added domain card 15 (equipped)",
    "Trade: removed [3], added [12]"
  ]
}
```

**Request field reference:**

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `advancements` | AdvancementChoice[] | Yes | Exactly 2 |
| `newExperienceDescription` | string | At tier transitions | Description for the tier achievement experience |
| `newDomainCardId` | long | Yes | Domain card to add in Step 4 |
| `equipNewDomainCard` | boolean | No | Default false |
| `unequipDomainCardId` | long | No | Unequip a card to make room |
| `trades` | DomainCardTradeRequest[] | No | Equal-swap trades |

### DELETE /api/dh/character-sheets/{id}/level-up

Undoes the most recent level-up.

**Authorization:** Character owner or MODERATOR+.

**Response:** `CharacterSheetResponse` (the character sheet after the undo)

Can be called repeatedly to undo multiple levels. Returns 400 if no advancement log exists.
