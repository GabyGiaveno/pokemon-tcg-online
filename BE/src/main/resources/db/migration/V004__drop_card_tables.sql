-- =============================================================
-- V004__drop_card_tables.sql
-- Drop card and card_set tables as they are now in-memory cache
-- =============================================================

-- 1. Eliminar la foreign key de deck_card hacia card
ALTER TABLE deck_card DROP CONSTRAINT fk_deck_card_card;

-- 2. Eliminar las tablas de cache (card y card_set)
DROP TABLE card;
DROP TABLE card_set;
