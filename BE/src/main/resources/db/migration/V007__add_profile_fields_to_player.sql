-- =============================================================
-- V007__add_profile_fields_to_player.sql
-- Adds profile display fields to player table
-- PostgreSQL 15+ | Flyway migration V007
-- =============================================================

ALTER TABLE player
    ADD COLUMN bio              TEXT      NOT NULL DEFAULT '',
    ADD COLUMN level            INTEGER   NOT NULL DEFAULT 1,
    ADD COLUMN xp               INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN favorite_region  VARCHAR(50) NOT NULL DEFAULT 'Kanto',
    ADD COLUMN favorite_pokemon VARCHAR(50) NOT NULL DEFAULT 'Pikachu',
    ADD COLUMN total_cards      INTEGER   NOT NULL DEFAULT 0;

COMMENT ON COLUMN player.bio              IS 'Short biography shown on the trainer profile page.';
COMMENT ON COLUMN player.level            IS 'Current player level. Starts at 1.';
COMMENT ON COLUMN player.xp              IS 'Cumulative experience points. xp / 100 gives the progress bar percentage.';
COMMENT ON COLUMN player.favorite_region  IS 'The region the player has selected as favourite (e.g. Kanto, Johto).';
COMMENT ON COLUMN player.favorite_pokemon IS 'The player s favourite Pokémon name.';
COMMENT ON COLUMN player.total_cards      IS 'Total number of cards the player has collected.';
