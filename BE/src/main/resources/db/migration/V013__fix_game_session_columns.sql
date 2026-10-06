-- Add columns that exist in the GameSession entity but were never migrated.
ALTER TABLE game_session
    ADD COLUMN player1_ready       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN player2_ready       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN coin_flip_winner_id BIGINT;

-- prize_cards_count was defined as SMALLINT in V1 but the entity maps it as int (INTEGER).
ALTER TABLE game_session ALTER COLUMN prize_cards_count TYPE INTEGER;
