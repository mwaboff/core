-- Migration: add_encounter_id_status_index_to_encounter_runs
-- Created: Mon Aug  3 11:33:10 AM EDT 2026
--
-- EncounterService#deleteEncounter's cascade looks up a source encounter's ACTIVE runs via
-- EncounterRunRepository#findByEncounter_IdAndStatus(encounterId, status) on every encounter
-- delete. encounter_runs already has fk_encounter_run_encounter on encounter_id, but Postgres
-- does not automatically index foreign key columns, and no query against encounter_id existed
-- before that cascade -- so this lookup has been a sequential scan of encounter_runs since it
-- was added. Composite on (encounter_id, status) since every current caller filters on both.

CREATE INDEX idx_encounter_runs_encounter_status ON encounter_runs(encounter_id, status);
