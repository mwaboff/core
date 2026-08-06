-- Migration: enforce_feature_dedupe_key
--
-- FeatureService.findOrCreate is a read-then-insert: it looks a feature up by
-- name + expansion + type + description and inserts one when the lookup misses. Nothing
-- stood behind that lookup, so two requests carrying the same inline feature could both
-- miss and both insert. Twelve concurrent POSTs to /api/dh/weapons/custom with one
-- identical feature produced ten rows where there should have been one. Item creation is
-- open to every logged-in user and there is no rate limiting, so the features table -- which
-- is global and shared with the official catalogue -- could be grown at will.
--
-- The index below is the actual guarantee. It mirrors the lookup in FeatureRepository
-- exactly, so a row that the query would consider a match is a row the index rejects:
--
--   LOWER(name)                       the lookup is case-insensitive on name
--   COALESCE(expansion_id, -1)        a NULL expansion must still collide with another
--                                     NULL expansion. In a unique index Postgres treats
--                                     NULLs as distinct, which would exempt exactly the
--                                     homebrew rows this is meant to bound. PG15's
--                                     NULLS NOT DISTINCT would also work -- both servers
--                                     are on 16 -- but COALESCE needs no version floor and
--                                     -1 can never be a real expansion id (BIGSERIAL).
--                                     ItemAccessService already uses -1 as its no-match
--                                     sentinel for the same reason.
--   feature_type                      compared exactly
--   MD5(COALESCE(description, ''))    compared exactly, and NULL must collide with NULL as
--                                     above. Hashed because description is unbounded TEXT:
--                                     indexing it raw would make any feature whose text
--                                     exceeds the btree tuple limit unsavable, turning a
--                                     dedupe guard into a new failure mode.
--
-- Scope of the predicate:
--   deleted_at IS NULL   soft-deleted rows are outside the lookup, so they must not block
--                        a new row from taking the same key.
--   name present         findOrCreate skips the lookup entirely for a blank name and always
--                        inserts. 196 such rows already exist. Covering them would make
--                        those inserts fail with no lookup able to find the winner, so the
--                        index stops exactly where the dedupe does.
--
-- What this does NOT do: a request that loses the race now fails instead of quietly
-- creating a duplicate. It surfaces as a 409 (see GlobalExceptionHandler) and is safe to
-- retry -- the retry's lookup finds the winner's row. Recovering in-process without a
-- failed response was rejected: after a constraint violation the Postgres transaction is
-- aborted, so re-reading needs either a REQUIRES_NEW insert (which breaks the
-- @Transactional integration tests, whose parent rows are never committed and so cannot be
-- referenced from a second connection) or a new spring-retry dependency.

-- Defensive: fold any pre-existing duplicates onto the lowest id so the index cannot fail
-- to build. This is a no-op on both the local and production databases today (0 rows) and
-- exists only so a deploy is not blocked if content diverged. Soft-deleting rather than
-- deleting keeps all 13 foreign keys into features intact; items still render the feature
-- they point at, they simply stop being re-selectable by id on a later edit.
WITH duplicates AS (
    SELECT f.id
    FROM features f
    JOIN (
        SELECT LOWER(name) AS lname,
               COALESCE(expansion_id, -1) AS exp,
               feature_type,
               MD5(COALESCE(description, '')) AS descr,
               MIN(id) AS keep_id
        FROM features
        WHERE deleted_at IS NULL AND name IS NOT NULL AND BTRIM(name) <> ''
        GROUP BY 1, 2, 3, 4
        HAVING COUNT(*) > 1
    ) k ON LOWER(f.name) = k.lname
       AND COALESCE(f.expansion_id, -1) = k.exp
       AND f.feature_type = k.feature_type
       AND MD5(COALESCE(f.description, '')) = k.descr
    WHERE f.deleted_at IS NULL AND f.id <> k.keep_id
)
UPDATE features SET deleted_at = CURRENT_TIMESTAMP
WHERE id IN (SELECT id FROM duplicates);

CREATE UNIQUE INDEX uq_features_dedupe_key
    ON features (LOWER(name), COALESCE(expansion_id, -1), feature_type, MD5(COALESCE(description, '')))
    WHERE deleted_at IS NULL AND name IS NOT NULL AND BTRIM(name) <> '';

COMMENT ON INDEX uq_features_dedupe_key IS
    'Backs the find-or-create key in FeatureRepository. Keep the two in step: a change to '
    'either the columns compared or their null-handling must change both, or concurrent '
    'inserts start minting duplicates again.';
