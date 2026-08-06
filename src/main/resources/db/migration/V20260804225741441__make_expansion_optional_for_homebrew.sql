-- Migration: make_expansion_optional_for_homebrew
--
-- An expansion is a sourcebook ("Daggerheart Core Set", "Hope & Fear"). A weapon a
-- player invented at their own table came from no book, so expansion_id must be
-- nullable. A NULL expansion is what identifies homebrew.
--
-- The constraint runs one direction only: official content must name its sourcebook,
-- but custom content is free to cite one or not. A biconditional
-- ((is_official = TRUE) = (expansion_id IS NOT NULL)) was considered and rejected --
-- the admin card editor sends dirty fields only, so unticking "Official content"
-- submits {"isOfficial": false} alone and the row would fail the check on save.
-- "Custom items carry no expansion" is enforced in ItemAccessService.resolveExpansion
-- instead, where the request context needed to decide it actually exists.
--
-- Every existing row is official with an expansion set, so this validates immediately.
--
-- features.expansion_id follows for the same reason: an inline feature authored
-- alongside a custom item has no sourcebook either. features has no is_official
-- column, so a plain nullable column is the whole story there. created_by_user_id is
-- added because opening item creation to every user means any user can now mint
-- global feature rows, and those rows should carry an author.

ALTER TABLE weapons ALTER COLUMN expansion_id DROP NOT NULL;
ALTER TABLE armors  ALTER COLUMN expansion_id DROP NOT NULL;
ALTER TABLE loot    ALTER COLUMN expansion_id DROP NOT NULL;

ALTER TABLE weapons ADD CONSTRAINT chk_weapons_official_has_expansion
    CHECK (is_official = FALSE OR expansion_id IS NOT NULL);
ALTER TABLE armors ADD CONSTRAINT chk_armors_official_has_expansion
    CHECK (is_official = FALSE OR expansion_id IS NOT NULL);
ALTER TABLE loot ADD CONSTRAINT chk_loot_official_has_expansion
    CHECK (is_official = FALSE OR expansion_id IS NOT NULL);

ALTER TABLE features ALTER COLUMN expansion_id DROP NOT NULL;
ALTER TABLE features ADD COLUMN created_by_user_id BIGINT;
ALTER TABLE features ADD CONSTRAINT fk_features_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users(id);

CREATE INDEX idx_features_created_by ON features(created_by_user_id);
