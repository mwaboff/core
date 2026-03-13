-- Migration: make_feature_name_nullable
-- Description: Allows features to have a NULL name

ALTER TABLE features ALTER COLUMN name DROP NOT NULL;
