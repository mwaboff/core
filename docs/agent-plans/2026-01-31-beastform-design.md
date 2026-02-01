# Beastform Entity Design

**Date:** 2026-01-31
**Status:** Approved
**Scope:** Entity model and database schema only (controller/service/repository deferred)

## Overview

Beastforms are creatures that characters can transform into. This design covers the entity model, database schema, and CharacterSheet association.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| CharacterSheet association | ManyToOne reference | Characters reference one active beastform, beastforms exist independently |
| Content management | Full pattern | Includes isOfficial, isPublic, originalBeastform, createdBy, expansion |
| Trait modifier storage | Separate columns | Matches CharacterSheet pattern, type-safe, easy queries |
| Customization approach | Copy pattern | Users copy beastforms to customize; originalBeastform tracks source |

## Entity: Beastform

**Location:** `src/main/java/com/aboff/core/model/entity/dh/Beastform.java`

### Fields

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| **Basic Information** |
| `name` | String(200) | No | - | Primary identifier |
| `example` | TEXT | Yes | - | Flavor text example |
| `advantages` | TEXT | Yes | - | Optional advantages text |
| **Trait Modifiers** |
| `agilityModifier` | Integer | No | 0 | Modifier for AGILITY trait |
| `strengthModifier` | Integer | No | 0 | Modifier for STRENGTH trait |
| `finesseModifier` | Integer | No | 0 | Modifier for FINESSE trait |
| `instinctModifier` | Integer | No | 0 | Modifier for INSTINCT trait |
| `presenceModifier` | Integer | No | 0 | Modifier for PRESENCE trait |
| `knowledgeModifier` | Integer | No | 0 | Modifier for KNOWLEDGE trait |
| **Combat** |
| `attackRange` | Range (enum) | No | - | MELEE, VERY_CLOSE, CLOSE, FAR, VERY_FAR, OUT_OF_RANGE |
| `attackTrait` | Trait (enum) | No | - | Trait used for attacks |
| `damage` | DamageRoll (embedded) | No | - | Reuses existing DamageRoll embeddable |
| **Features** |
| `features` | Set\<Feature\> | - | empty | ManyToMany relationship |
| **Content Management** |
| `isOfficial` | Boolean | No | false | Official game content flag |
| `isPublic` | Boolean | No | false | Publicly visible flag |
| `originalBeastform` | Beastform | Yes | null | Self-reference for custom copies |
| `expansion` | Expansion | No | - | Required expansion reference |
| `createdBy` | User | No | - | Creator/owner |
| **Soft Delete** |
| `deletedAt` | LocalDateTime | Yes | null | Soft delete timestamp |

### Annotations & Patterns

- Extends `BaseEntity` (provides id, createdAt, lastModifiedAt)
- Uses `@SuperBuilder`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- DamageRoll embedded with `@AttributeOverrides` (matches Adversary pattern)
- Features via `@ManyToMany` with join table `beastform_features`
- Soft delete methods: `isDeleted()`, `softDelete()`, `restore()`

## CharacterSheet Modification

**Location:** `src/main/java/com/aboff/core/model/entity/dh/CharacterSheet.java`

### Addition

```java
/**
 * The character's currently active beastform.
 * Represents the creature form the character can transform into.
 * Can be null if the character has no beastform.
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "active_beastform_id")
private Beastform activeBeastform;
```

## Database Migrations

### Migration 1: Create beastforms table

**Filename:** `V{timestamp}__create_beastforms_table.sql`

```sql
-- Create beastforms table
CREATE TABLE beastforms (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Basic Information
    name VARCHAR(200) NOT NULL,
    example TEXT,
    advantages TEXT,

    -- Trait Modifiers
    agility_modifier INTEGER NOT NULL DEFAULT 0,
    strength_modifier INTEGER NOT NULL DEFAULT 0,
    finesse_modifier INTEGER NOT NULL DEFAULT 0,
    instinct_modifier INTEGER NOT NULL DEFAULT 0,
    presence_modifier INTEGER NOT NULL DEFAULT 0,
    knowledge_modifier INTEGER NOT NULL DEFAULT 0,

    -- Combat
    attack_range VARCHAR(20) NOT NULL,
    attack_trait VARCHAR(20) NOT NULL,
    damage_dice_count INTEGER,
    damage_dice_type VARCHAR(10) NOT NULL,
    damage_modifier INTEGER,
    damage_type VARCHAR(10) NOT NULL,

    -- Content Management
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    original_beastform_id BIGINT REFERENCES beastforms(id),
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    creator_id BIGINT NOT NULL REFERENCES users(id),

    -- Soft Delete
    deleted_at TIMESTAMP
);

-- Create beastform_features join table
CREATE TABLE beastform_features (
    beastform_id BIGINT NOT NULL REFERENCES beastforms(id) ON DELETE CASCADE,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    PRIMARY KEY (beastform_id, feature_id)
);

-- Indexes
CREATE INDEX idx_beastforms_expansion_id ON beastforms(expansion_id);
CREATE INDEX idx_beastforms_creator_id ON beastforms(creator_id);
CREATE INDEX idx_beastforms_deleted_at ON beastforms(deleted_at);
CREATE INDEX idx_beastforms_is_official ON beastforms(is_official);
CREATE INDEX idx_beastforms_is_public ON beastforms(is_public);
```

### Migration 2: Add active_beastform_id to character_sheets

**Filename:** `V{timestamp}__add_active_beastform_to_character_sheets.sql`

```sql
-- Add active beastform reference to character sheets
ALTER TABLE character_sheets
ADD COLUMN active_beastform_id BIGINT REFERENCES beastforms(id);

-- Index for the foreign key
CREATE INDEX idx_character_sheets_active_beastform_id ON character_sheets(active_beastform_id);
```

## Files Summary

| File | Action |
|------|--------|
| `src/main/java/com/aboff/core/model/entity/dh/Beastform.java` | Create |
| `src/main/java/com/aboff/core/model/entity/dh/CharacterSheet.java` | Modify (add activeBeastform field) |
| `src/main/resources/db/migration/V{timestamp}__create_beastforms_table.sql` | Create |
| `src/main/resources/db/migration/V{timestamp}__add_active_beastform_to_character_sheets.sql` | Create |

## Out of Scope (Deferred)

- Repository class
- Service class
- Controller class
- DTOs (request/response)
- Tests
