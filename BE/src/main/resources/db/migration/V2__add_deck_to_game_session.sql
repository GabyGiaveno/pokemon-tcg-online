-- =============================================================
-- V2__add_deck_to_game_session.sql
-- Agrega deck1_id y deck2_id a game_session para asociar
-- los mazos elegidos por cada jugador al crear/unirse.
-- PostgreSQL 15+ | Flyway migration V2
-- =============================================================

ALTER TABLE game_session
    ADD COLUMN deck1_id BIGINT REFERENCES deck(id),
    ADD COLUMN deck2_id BIGINT REFERENCES deck(id);

COMMENT ON COLUMN game_session.deck1_id IS 'Mazo elegido por player1 al crear la partida. Se usa en SETUP para inicializar el tablero.';
COMMENT ON COLUMN game_session.deck2_id IS 'Mazo elegido por player2 al unirse. Se usa en SETUP para inicializar el tablero.';
