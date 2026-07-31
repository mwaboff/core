-- Migration: add_evasion_and_tier_to_beastforms
-- Adds the printed Evasion bonus and Tier (1-4) values from the beastform stat-block cards.
-- The beastforms table has no rows yet, so NOT NULL columns can be added without a DEFAULT.
--
-- Two of the 24 core-book beastform cards ("Legendary Beast" T3, "Mythic Beast" T4) are
-- "Evolved: upgrade an earlier pick" cards -- they print no stat line at all (no Evasion,
-- attack range/trait, damage, or trait modifiers; their bonuses are relative to whichever
-- base form the player already chose and stay as prose in the feature text, applied
-- manually). Evasion and the combat columns must therefore be nullable so those two
-- records can be persisted with no stat data, matching the "framework block" pattern used
-- for adversaries without their own stats
-- (see V20260730122613435__allow_null_adversary_stats_for_framework_blocks.sql).
-- Tier is still required and bounded -- every beastform card, including the two Evolved
-- ones, prints a tier.
--
-- The six trait modifiers move from NOT NULL DEFAULT 0 to nullable with NO default. A
-- NOT NULL DEFAULT column silently turning "omitted from the import payload" into "the
-- beastform grants +0" is the same defect shape that left 69 of 120 loot rows mis-tiered
-- in prod: an earlier import omitted `tier`, and the NOT NULL DEFAULT 1 column filled it
-- in, indistinguishably from a real tier-1 item. "This card prints no trait bonus" and
-- "this card prints a bonus of zero" are different statements; NULL is the honest encoding
-- of the former. Ordinary cards that do print e.g. "Agility +1" leave the other five
-- traits with no printed line either -- callers that want an explicit, on-the-record zero
-- for an untouched trait must send it themselves; the column no longer manufactures one.

ALTER TABLE beastforms ADD COLUMN evasion INTEGER;
ALTER TABLE beastforms ADD COLUMN tier INTEGER NOT NULL;

ALTER TABLE beastforms ADD CONSTRAINT chk_beastforms_tier CHECK (tier BETWEEN 1 AND 4);

ALTER TABLE beastforms ALTER COLUMN attack_range DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN attack_trait DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN damage_dice_type DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN damage_type DROP NOT NULL;

ALTER TABLE beastforms ALTER COLUMN agility_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN agility_modifier DROP DEFAULT;
ALTER TABLE beastforms ALTER COLUMN strength_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN strength_modifier DROP DEFAULT;
ALTER TABLE beastforms ALTER COLUMN finesse_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN finesse_modifier DROP DEFAULT;
ALTER TABLE beastforms ALTER COLUMN instinct_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN instinct_modifier DROP DEFAULT;
ALTER TABLE beastforms ALTER COLUMN presence_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN presence_modifier DROP DEFAULT;
ALTER TABLE beastforms ALTER COLUMN knowledge_modifier DROP NOT NULL;
ALTER TABLE beastforms ALTER COLUMN knowledge_modifier DROP DEFAULT;
