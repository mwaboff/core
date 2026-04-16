# Shared Models & Enums

Models, embeddable types, and enums used across multiple controllers.

## Base Types

### BaseEntity
Fields inherited by all entities.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Auto-generated primary key (database identity strategy) |
| `createdAt` | `LocalDateTime` | Timestamp set automatically on first persist; immutable after creation |
| `lastModifiedAt` | `LocalDateTime` | Timestamp updated automatically on every modification |

## Embeddable Types

### DamageRoll
Represents a damage roll in dice notation, e.g. `2d12+3 phy`. Used by Weapon and Adversary entities.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `diceCount` | `Integer` | Yes | Number of dice to roll. If null, uses the character's proficiency score. |
| `diceType` | `DiceType` | No | The type of die to roll (d4, d6, d8, d10, d12, d20) |
| `modifier` | `Integer` | Yes | Bonus or penalty added to the roll. Can be positive, negative, or null. |
| `damageType` | `DamageType` | No | The type of damage dealt (PHYSICAL or MAGIC) |

**Notation format:** `[count]dX[+/-modifier] [damageType]`
- `d10+3 mag` -- proficiency-based, +3 modifier, magic damage
- `2d12 phy` -- 2 dice, no modifier, physical damage
- `d6-1 phy` -- proficiency-based, -1 modifier, physical damage

## Enums

### Role
User permission roles, ordered from least to most privileged.

| Value | Description |
|-------|-------------|
| `USER` | Standard user |
| `MODERATOR` | Can bypass ownership checks |
| `ADMIN` | Administrative access |
| `OWNER` | Highest privilege; can modify official content |

**Used by:** User entity, ChangeRoleRequest, RoleHierarchyService, AdversaryService, EncounterService

---

### Trait
The six core character traits in Daggerheart. Each has a description and usage examples.

| Value | Description | Examples |
|-------|-------------|----------|
| `AGILITY` | Quick reflexes, nimbleness, and coordination | Dodging attacks, acrobatics, sleight of hand, stealth |
| `STRENGTH` | Raw physical power and endurance | Melee attacks, athletics, breaking objects, carrying heavy loads |
| `FINESSE` | Precision, grace, and careful execution | Ranged attacks, lockpicking, crafting, precise movements |
| `INSTINCT` | Intuition, awareness, and natural understanding | Perception, survival, animal handling, reading situations |
| `PRESENCE` | Force of personality and social influence | Persuasion, intimidation, performance, leadership |
| `KNOWLEDGE` | Learning, reasoning, and mental acuity | Spellcasting, history, investigation, arcana |

**Used by:** Weapon, SubclassPath, Beastform entities; WeaponResponse, SubclassPathResponse; Create/Update Weapon/SubclassPath/SubclassCard requests

---

### CardType
Discriminator for the Card entity inheritance hierarchy (JOINED strategy).

| Value | Description |
|-------|-------------|
| `ANCESTRY` | Ancestry card |
| `COMMUNITY` | Community card |
| `SUBCLASS` | Subclass card |
| `DOMAIN` | Domain card |

**Used by:** Card entity, AncestryCardResponse, CommunityCardResponse, SubclassCardResponse, DomainCardResponse

---

### DomainCardType
Subtypes of domain cards.

| Value | Description |
|-------|-------------|
| `SPELL` | Spell domain card |
| `GRIMOIRE` | Grimoire domain card |
| `ABILITY` | Ability domain card |
| `TRANSFORMATION` | Transformation domain card |
| `WILD` | Wild domain card |

**Used by:** DomainCard entity, DomainCardResponse, Create/UpdateDomainCardRequest, DomainCardRepository

---

### SubclassLevel
Progression levels for subclass cards.

| Value | Description |
|-------|-------------|
| `FOUNDATION` | Foundation level subclass |
| `SPECIALIZATION` | Specialization level subclass |
| `MASTERY` | Mastery level subclass |

**Used by:** SubclassCard entity, SubclassCardResponse, Create/UpdateSubclassCardRequest, SubclassCardRepository

---

### FeatureType
Categories of character features.

| Value | Description |
|-------|-------------|
| `HOPE` | Hope feature |
| `ANCESTRY` | Ancestry feature |
| `CLASS` | Class feature |
| `COMMUNITY` | Community feature |
| `DOMAIN` | Domain feature |
| `ITEM` | Item feature |
| `OTHER` | Other/miscellaneous feature |
| `SUBCLASS` | Subclass feature |

**Used by:** Feature entity, FeatureResponse, Create/UpdateFeatureRequest, FeatureInput, FeatureRepository, FeatureService

---

### QuestionType
Types of character-building questions on cards.

| Value | Description |
|-------|-------------|
| `BACKGROUND` | Background question |
| `CONNECTION` | Connection question |

**Used by:** Question entity, QuestionResponse, Create/UpdateQuestionRequest, QuestionRepository

---

### AdversaryType
Tactical role and combat behavior of adversaries. Each type has a description and battle point value for encounter balancing.

| Value | Description | Battle Points |
|-------|-------------|---------------|
| `BRUISER` | Tough melee combatants with high HP | 4 |
| `HORDE` | Multiple weak enemies that attack together | 2 |
| `LEADER` | Commanders that buff allies | 3 |
| `MINION` | Basic enemies with minimal HP | 1 |
| `RANGED` | Distance attackers | 2 |
| `SKULK` | Stealthy enemies with evasion bonuses | 2 |
| `SOCIAL` | Non-combat focused adversaries | 1 |
| `SOLO` | Single powerful enemy designed to fight alone | 5 |
| `STANDARD` | Balanced general-purpose adversary | 2 |
| `SUPPORT` | Provides utility and healing to allies | 1 |

**Used by:** Adversary entity, AdversaryResponse, Create/UpdateAdversaryRequest, AdversaryRepository

---

### DamageType
Types of damage, each with a short code used in dice notation.

| Value | Code | Description |
|-------|------|-------------|
| `PHYSICAL` | `phy` | Physical damage, typically from weapons and melee attacks |
| `MAGIC` | `mag` | Magic damage, typically from spells and magical abilities |

**Used by:** DamageRoll embeddable, Weapon/Adversary entities, WeaponResponse, AdversaryResponse, Create/Update Weapon/Adversary requests

---

### DiceType
Standard TTRPG dice types, each with a side count and display code.

| Value | Sides | Code | Description |
|-------|-------|------|-------------|
| `D4` | 4 | `d4` | Four-sided die |
| `D6` | 6 | `d6` | Six-sided die |
| `D8` | 8 | `d8` | Eight-sided die |
| `D10` | 10 | `d10` | Ten-sided die |
| `D12` | 12 | `d12` | Twelve-sided die |
| `D20` | 20 | `d20` | Twenty-sided die |

**Used by:** DamageRoll embeddable, Companion entity, WeaponResponse, AdversaryResponse, CompanionResponse, Create/Update Weapon/Adversary/Companion requests

---

### Range
Effective distance categories for weapons and attacks.

| Value | Description |
|-------|-------------|
| `MELEE` | Close-quarters combat, under 5 feet |
| `VERY_CLOSE` | Extended melee or point-blank range, 5-10 feet |
| `CLOSE` | Short throwing distance, 10-30 feet |
| `FAR` | Standard ranged weapon distance, 30-100 feet |
| `VERY_FAR` | Long-range projectile distance, 100-300 feet |
| `OUT_OF_RANGE` | Extreme distance beyond normal weapon effectiveness, beyond 300 feet |

**Used by:** Weapon, Adversary, Companion, Beastform entities; WeaponResponse, AdversaryResponse, CompanionResponse; Create/Update Weapon/Adversary/Companion requests

---

### Burden
How many hands are required to wield a weapon.

| Value | Description |
|-------|-------------|
| `ONE_HANDED` | Weapon can be wielded with one hand, allowing use of a shield or off-hand weapon |
| `TWO_HANDED` | Weapon requires both hands to wield effectively |

**Used by:** Weapon entity, WeaponResponse, Create/UpdateWeaponRequest, WeaponRepository

---

### CostTagCategory
Categories for card cost/limitation badges, used for frontend grouping and styling.

| Value | Description |
|-------|-------------|
| `COST` | Resource expenditure tags (e.g., "3 Hope", "1 Stress") |
| `LIMITATION` | Restriction or requirement tags (e.g., "Close range", "Requires Level 5") |
| `TIMING` | Frequency or action type tags (e.g., "1/session", "Action", "Reaction") |

**Used by:** CardCostTag entity, CardCostTagResponse, Create/UpdateCardCostTagRequest, CostTagInput, CardCostTagRepository

---

### AdvancementType
Character advancement options available during level-up. Each type has a minimum tier requirement.

| Value | Description | Min Tier |
|-------|-------------|----------|
| `BOOST_TRAITS` | +1 to two traits, mark them. Traits must be unmarked unless entering Tier 3 or 4 (levels 5, 8) where marks are cleared first | 2 |
| `GAIN_HP` | +1 hit point max | 2 |
| `GAIN_STRESS` | +1 stress max | 2 |
| `BOOST_EXPERIENCES` | +1 modifier to two experiences | 2 |
| `GAIN_DOMAIN_CARD` | Choose a domain card of appropriate level | 2 |
| `BOOST_EVASION` | +1 evasion | 2 |
| `UPGRADE_SUBCLASS` | Take upgraded subclass card (Tier 3+) | 3 |
| `BOOST_PROFICIENCY` | +1 proficiency (Tier 3+) | 3 |
| `MULTICLASS` | Choose additional class (Tier 3+) | 3 |
| `FEATURE_DOMAIN_CARD` | Bonus domain card granted by a subclass feature's `BONUS_DOMAIN_CARD_SELECTIONS` modifier. Client-injected; not returned by `getLevelUpOptions` and not player-selectable. | 1 |

**Per-tier limits:** BOOST_TRAITS: 3, GAIN_HP: 2, GAIN_STRESS: 2, BOOST_EXPERIENCES: 1, GAIN_DOMAIN_CARD: 1, BOOST_EVASION: 1, UPGRADE_SUBCLASS: 1, BOOST_PROFICIENCY: 2, MULTICLASS: 2. Players may select the same advancement type twice in one level-up if the per-tier limit allows it. `FEATURE_DOMAIN_CARD` has no per-tier cap and does not count toward `GAIN_DOMAIN_CARD`'s limit.

**Mutual exclusion:** UPGRADE_SUBCLASS and MULTICLASS are mutually exclusive within a tier. If one is chosen at any point during a tier, the other becomes unavailable for the remainder of that tier. Both types cannot appear in the same level-up request.

**Cross-validation for duplicates:** When the same type is chosen twice in one request: BOOST_TRAITS requires all traits to be distinct across both choices; MULTICLASS requires each choice to target a different class.

**Player vs. feature entries:** The `advancements` list must contain exactly two player-chosen entries (any type other than `FEATURE_DOMAIN_CARD`). Additional `FEATURE_DOMAIN_CARD` entries may ride along — they are validated per-entry (domain accessibility, tier cap) but are not counted toward the "exactly 2" rule or any tier-usage cap. Cards granted by `FEATURE_DOMAIN_CARD` are always added unequipped.

**Used by:** LevelUpRequest (AdvancementChoice), LevelUpOptionsResponse, CharacterAdvancementLog entity, LevelUpService

---

### ModifierOperation
Mathematical operations applied by feature modifiers. Evaluated in order: SET first, then MULTIPLY, then ADD.

| Value | Description |
|-------|-------------|
| `ADD` | Adds the value to the target attribute |
| `SET` | Sets the target attribute to the specified value |
| `MULTIPLY` | Multiplies the target attribute by the specified value |

**Used by:** FeatureModifier entity, FeatureModifierResponse, CreateFeatureModifierRequest, FeatureModifierInput, FeatureModifierRepository

---

### ModifierTarget
Character attributes that a feature modifier can affect.

| Value | Description |
|-------|-------------|
| `AGILITY` | Modifies the character's Agility trait score |
| `STRENGTH` | Modifies the character's Strength trait score |
| `FINESSE` | Modifies the character's Finesse trait score |
| `INSTINCT` | Modifies the character's Instinct trait score |
| `PRESENCE` | Modifies the character's Presence trait score |
| `KNOWLEDGE` | Modifies the character's Knowledge trait score |
| `EVASION` | Modifies the character's Evasion defense value |
| `MAJOR_DAMAGE_THRESHOLD` | Modifies the character's Major damage threshold |
| `SEVERE_DAMAGE_THRESHOLD` | Modifies the character's Severe damage threshold |
| `HIT_POINT_MAX` | Modifies the character's maximum Hit Points |
| `STRESS_MAX` | Modifies the character's maximum Stress capacity |
| `HOPE_MAX` | Modifies the character's maximum Hope |
| `ARMOR_MAX` | Modifies the character's maximum Armor slots |
| `GOLD` | Modifies the character's starting Gold |
| `ATTACK_ROLL` | Modifies the character's attack roll result |
| `DAMAGE_ROLL` | Modifies the character's damage roll result |
| `PRIMARY_DAMAGE_ROLL` | Modifies the character's primary damage roll result |
| `ARMOR_SCORE` | Modifies the character's armor score |
| `BONUS_DOMAIN_CARD_SELECTIONS` | Declarative marker — subclass features with this modifier grant additional domain card selections at character creation and level-up. The server does not enforce counts from this value; the client reads it to render extra picker slots and injects `FEATURE_DOMAIN_CARD` advancement entries into level-up requests. |
| `BONUS_EXPERIENCE_MODIFIER` | Declarative marker — features with this modifier grant a one-time +N bonus to a player-chosen existing experience at character creation or level-up. The server does not apply the bonus; the client reads it to prompt the player to pick an experience and writes the resulting bonus into the experience itself. |

**Used by:** FeatureModifier entity, FeatureModifierResponse, CreateFeatureModifierRequest, FeatureModifierInput, FeatureModifierRepository
