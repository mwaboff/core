# Move Features to BaseItem - Design Document

**Date:** 2026-02-28
**Status:** Approved

## Context

Currently, Weapon and Armor entities each have a single `@ManyToOne` relationship to Feature (via `feature_id` column). Loot has no feature relationship. The goal is to:

1. Move the feature relationship to `BaseItem` so all item types have it
2. Change from single feature to **multiple features** (ManyToMany)
3. Add feature support to Loot

## Approach

Since `BaseItem` uses `@MappedSuperclass`, each subclass has its own table. We use `@ManyToMany` on BaseItem with `@AssociationOverride` on each subclass to point to type-specific join tables.

### Database Migration

Create a single migration that:

1. Creates join tables:
   - `weapon_features(weapon_id, feature_id)` - composite PK, FKs to weapons and features
   - `armor_features(armor_id, feature_id)` - composite PK, FKs to armors and features
   - `loot_features(loot_id, feature_id)` - composite PK, FKs to loot and features
2. Migrates existing data:
   - `INSERT INTO weapon_features SELECT id, feature_id FROM weapons WHERE feature_id IS NOT NULL`
   - `INSERT INTO armor_features SELECT id, feature_id FROM armors WHERE feature_id IS NOT NULL`
3. Drops old columns:
   - `ALTER TABLE weapons DROP COLUMN feature_id`
   - `ALTER TABLE armors DROP COLUMN feature_id`

Each join table gets indexes on both FK columns.

### Entity Changes

| File | Change |
|------|--------|
| **BaseItem.java** | Add `@ManyToMany(fetch = FetchType.LAZY) Set<Feature> features = new HashSet<>()` with a default `@JoinTable` |
| **Weapon.java** | Remove `feature` field. Add `@AssociationOverride(name="features", joinTable=@JoinTable(name="weapon_features", joinColumns=@JoinColumn(name="weapon_id"), inverseJoinColumns=@JoinColumn(name="feature_id")))` |
| **Armor.java** | Remove `feature` field. Add `@AssociationOverride(name="features", joinTable=@JoinTable(name="armor_features", joinColumns=@JoinColumn(name="armor_id"), inverseJoinColumns=@JoinColumn(name="feature_id")))` |
| **Loot.java** | Add `@AssociationOverride(name="features", joinTable=@JoinTable(name="loot_features", joinColumns=@JoinColumn(name="loot_id"), inverseJoinColumns=@JoinColumn(name="feature_id")))` |

### DTO Changes

**Request DTOs** - all item types become consistent:

| DTO | Old Fields | New Fields |
|-----|-----------|------------|
| CreateWeaponRequest | `featureId` (Long), `feature` (FeatureInput) | `featureIds` (List<Long>), `features` (List<FeatureInput>) |
| UpdateWeaponRequest | same | same |
| CreateArmorRequest | same | same |
| UpdateArmorRequest | same | same |
| CreateLootRequest | *(none)* | `featureIds` (List<Long>), `features` (List<FeatureInput>) |
| UpdateLootRequest | *(none)* | same |

**Response DTOs:**

| DTO | Old Fields | New Fields |
|-----|-----------|------------|
| WeaponResponse | `featureId` (Long), `feature` (FeatureResponse) | `featureIds` (List<Long>), `features` (List<FeatureResponse>) |
| ArmorResponse | same | same |
| LootResponse | *(none)* | `featureIds` (List<Long>), `features` (List<FeatureResponse>) |

### Service Changes

| Service | Change |
|---------|--------|
| **FeatureService** | Add `resolveFeatures(List<Long> featureIds, List<FeatureInput> features)` method returning `Set<Feature>` |
| **WeaponService** | Update create/update/toResponse to use plural features via resolveFeatures |
| **ArmorService** | Same pattern as WeaponService |
| **LootService** | Add feature handling matching weapon/armor pattern |

### Testing Strategy

| Test File | Change |
|-----------|--------|
| FeatureServiceTest | Add test for resolveFeatures method |
| WeaponServiceTest | Update to use multiple features |
| ArmorServiceTest | Update to use multiple features |
| LootServiceTest | Add feature-related tests |
| Weapon integration test | Update feature assertions |
| Armor integration test | Update feature assertions |
| Loot integration test | Add feature test cases |
