-- Migration: add_prayer_dice_to_character_sheets
-- Created: Thu Aug 13 10:24:59 PM EDT 2026
-- Description: Adds the Seraph "Prayer Dice" resource to character sheets. The rolled d4s are
-- stored as one nullable comma-separated string ("3,1*,4,2" — a "*" suffix marks a spent die)
-- rather than a child table: the list is bounded at 16 small integers and every other sheet
-- resource (focus_marked, favor, combo_die) is likewise a flat column. NULL means no dice have
-- been rolled this session, which is the correct state for every non-Seraph character.

ALTER TABLE character_sheets
    ADD COLUMN prayer_dice VARCHAR(64) NULL;
