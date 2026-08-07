-- Migration: add_visibility_to_items
--
-- Users may now author their own weapons, armor, and loot. Until now all three
-- tables held official imports only, so they never needed the is_public flag that
-- adversaries, encounters, environments, and beastforms already carry. Item
-- visibility becomes: official OR public OR mine OR tagged to one of my campaigns.
--
-- is_official's DEFAULT true predates user authoring. It is only reachable from raw
-- SQL (JPA always writes the value), but leaving it would mean a hand-written INSERT
-- silently publishes content as canon. Flipped to false; no existing row changes.
--
-- Deliberately NOT added: a CHECK requiring custom rows to name a creator
-- (is_official = TRUE OR created_by_user_id IS NOT NULL). It reads like a sound
-- invariant and it is not. Every official row has a NULL creator, so an admin
-- unticking "Official content" in the card editor would produce
-- (is_official=false, created_by_user_id=NULL) and fail the check -- breaking a
-- legitimate flow (demoting an import that shouldn't be canon) to enforce a rule the
-- service already guarantees on the path that matters. Creatorless custom rows are
-- treated as system content by the visibility query, exactly like the 661 existing
-- official rows, so they degrade safely rather than becoming invisible.

-- martial_stances is included because it also extends BaseItem. Custom authoring is
-- not being opened for stances, but the column has to exist or Hibernate's schema
-- validation fails at startup -- the field lives on the shared superclass, so every
-- subclass table needs backing storage for it.

ALTER TABLE weapons          ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE armors           ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE loot             ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE martial_stances  ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE weapons          ALTER COLUMN is_official SET DEFAULT FALSE;
ALTER TABLE armors           ALTER COLUMN is_official SET DEFAULT FALSE;
ALTER TABLE loot             ALTER COLUMN is_official SET DEFAULT FALSE;
ALTER TABLE martial_stances  ALTER COLUMN is_official SET DEFAULT FALSE;

CREATE INDEX idx_weapons_is_public ON weapons(is_public);
CREATE INDEX idx_armors_is_public  ON armors(is_public);
CREATE INDEX idx_loot_is_public    ON loot(is_public);

-- The browse hot path: active rows narrowed by the visibility OR-chain.
CREATE INDEX idx_weapons_visibility ON weapons(is_official, is_public, created_by_user_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_armors_visibility ON armors(is_official, is_public, created_by_user_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_loot_visibility ON loot(is_official, is_public, created_by_user_id)
    WHERE deleted_at IS NULL;
