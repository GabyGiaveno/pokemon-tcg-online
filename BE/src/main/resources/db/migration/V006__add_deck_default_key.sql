-- =============================================================
-- V006__add_deck_default_key.sql
-- Default deck marker for starter deck idempotency — PostgreSQL
-- =============================================================

ALTER TABLE deck
    ADD COLUMN default_key VARCHAR(50);

COMMENT ON COLUMN deck.default_key IS 'Stable key identifying a default/starter deck (e.g. starter-fire). NULL for user-created decks. Used for idempotent provisioning.';

CREATE UNIQUE INDEX idx_deck_player_default_key
    ON deck (player_id, default_key)
    WHERE default_key IS NOT NULL;

COMMENT ON INDEX idx_deck_player_default_key IS 'Ensures each player can have at most one deck per default key. Non-null rows only — user-created decks (NULL) do not interact with this constraint.';
