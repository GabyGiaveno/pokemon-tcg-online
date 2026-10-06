-- =============================================================
-- V010__add_character_to_trainer_skin.sql
-- Adds character discriminator column to trainer_skin and
-- seeds Brock and Misty as new playable trainer characters.
-- PostgreSQL 15+ | Flyway migration V010
-- =============================================================

-- "character" es palabra reservada en PostgreSQL: debe ir entre comillas dobles.
ALTER TABLE trainer_skin
    ADD COLUMN "character" VARCHAR(20) NOT NULL DEFAULT 'ash';

INSERT INTO trainer_skin (id, name, "character", hat_color, shirt_color, pants_color, skin_tone) VALUES
('brock', 'Brock', 'brock', '#2a1a0e', '#4a7c3f', '#3d2b1f', '#c8a165'),
('misty', 'Misty', 'misty', '#ff6600', '#e63946', '#20b2aa', '#f5d0a9')
ON CONFLICT (id) DO NOTHING;
