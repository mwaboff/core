-- Migration: create_item_campaign_join_tables
--
-- Explicit campaign sharing for custom items: an item tagged to a campaign is
-- visible to everyone involved in that campaign, in any role.
--
-- A join table rather than the single nullable campaign_id that encounters use.
-- An encounter is run once, at one table, so one FK fits it. An item is a reusable
-- library entry -- a GM running two campaigns wants the same homebrew sword in both,
-- and a single FK would force them to duplicate the row.
--
-- Mirrors the weapon_features / armor_features / loot_features shape so BaseItem can
-- pick it up through the same @AssociationOverride mechanism.
--
-- The composite primary key covers lookups by item. The extra single-column index on
-- campaign_id is what the visibility query actually probes ("which items are tagged
-- to any of my campaigns"). Note campaigns are soft-deleted, so ON DELETE CASCADE
-- here will effectively never fire; stale tags are filtered in the query instead.

CREATE TABLE weapon_campaigns (
    weapon_id   BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    PRIMARY KEY (weapon_id, campaign_id),
    CONSTRAINT fk_weapon_campaigns_weapon   FOREIGN KEY (weapon_id)   REFERENCES weapons(id)   ON DELETE CASCADE,
    CONSTRAINT fk_weapon_campaigns_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE armor_campaigns (
    armor_id    BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    PRIMARY KEY (armor_id, campaign_id),
    CONSTRAINT fk_armor_campaigns_armor    FOREIGN KEY (armor_id)    REFERENCES armors(id)    ON DELETE CASCADE,
    CONSTRAINT fk_armor_campaigns_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE loot_campaigns (
    loot_id     BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    PRIMARY KEY (loot_id, campaign_id),
    CONSTRAINT fk_loot_campaigns_loot     FOREIGN KEY (loot_id)     REFERENCES loot(id)      ON DELETE CASCADE,
    CONSTRAINT fk_loot_campaigns_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

-- martial_stances extends BaseItem too, so it inherits the campaigns association and
-- needs its own join table for Hibernate schema validation to pass. Custom stance
-- authoring is not in scope; this table is expected to stay empty for now.
CREATE TABLE martial_stance_campaigns (
    martial_stance_id BIGINT NOT NULL,
    campaign_id       BIGINT NOT NULL,
    PRIMARY KEY (martial_stance_id, campaign_id),
    CONSTRAINT fk_martial_stance_campaigns_stance   FOREIGN KEY (martial_stance_id) REFERENCES martial_stances(id) ON DELETE CASCADE,
    CONSTRAINT fk_martial_stance_campaigns_campaign FOREIGN KEY (campaign_id)       REFERENCES campaigns(id)       ON DELETE CASCADE
);

CREATE INDEX idx_weapon_campaigns_campaign         ON weapon_campaigns(campaign_id);
CREATE INDEX idx_armor_campaigns_campaign          ON armor_campaigns(campaign_id);
CREATE INDEX idx_loot_campaigns_campaign           ON loot_campaigns(campaign_id);
CREATE INDEX idx_martial_stance_campaigns_campaign ON martial_stance_campaigns(campaign_id);
