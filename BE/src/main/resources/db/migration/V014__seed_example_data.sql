-- =============================================================
-- V014__seed_example_data.sql
-- Seed data — jugadores, mazos y partida de ejemplo
-- PostgreSQL 15+ | Flyway migration V014
--
-- Credenciales de los jugadores de ejemplo:
--   ash@pokemon.com   / Pokemon123!
--   misty@pokemon.com / Pokemon123!
--   brock@pokemon.com / Pokemon123!
-- =============================================================

-- =============================================================
-- Jugadores de ejemplo
-- =============================================================
INSERT INTO player (username, email, password_hash, created_at) VALUES
    ('ash',   'ash@pokemon.com',   '$2a$10$NY5WQ8QJZt3SKe3VH2A.WOgKyXgMVb8op4ok6tzy/14RSIXPLG2zC', NOW()),
    ('misty', 'misty@pokemon.com', '$2a$10$MRDEl9zpwbJpckHZTYIzZOc8yLWx7zOYgfCjbrlmzz5yMTOtkHRX2', NOW()),
    ('brock', 'brock@pokemon.com', '$2a$10$rHQij8.sehURSj3zWFQdK.UjvnmDGsOBVUZlMlk6ihgDMP.3hVMJ6', NOW());

-- =============================================================
-- Mazos de ejemplo
-- Deck 1 (ash): mazo Normal/Incoloro centrado en Tauros + Snorlax
-- Deck 2 (misty): mazo Agua centrado en Poliwag + Goldeen
-- =============================================================
INSERT INTO deck (name, player_id, is_valid, created_at)
VALUES
    ('Mazo de Ash — Tauros', (SELECT id FROM player WHERE username = 'ash'),   TRUE, NOW()),
    ('Mazo de Misty — Agua', (SELECT id FROM player WHERE username = 'misty'), TRUE, NOW());

-- =============================================================
-- Composición Mazo de Ash (60 cartas — set XY1 Kalos Starter)
-- 14 Pokémon + 12 Entrenadores + 34 Energías
-- =============================================================
INSERT INTO deck_card (deck_id, card_id, quantity) VALUES
    -- Pokémon (14)
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-100', 4),  -- Tauros
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-96',  4),  -- Kangaskhan
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-3',   4),  -- Weedle
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-4',   2),  -- Kakuna
    -- Entrenadores / Ítems (12)
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-126', 4),  -- Potion
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-127', 4),  -- Poké Ball
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-128', 4),  -- Professor's Letter
    -- Energía (34)
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-132', 17), -- Grass Energy
    ((SELECT id FROM deck WHERE name = 'Mazo de Ash — Tauros'), 'xy1-136', 17); -- Colorless Energy

-- =============================================================
-- Composición Mazo de Misty (60 cartas — set XY1 Kalos Starter)
-- 16 Pokémon + 10 Entrenadores + 34 Energías
-- =============================================================
INSERT INTO deck_card (deck_id, card_id, quantity) VALUES
    -- Pokémon (16)
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-29',  4),  -- Poliwag
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-30',  2),  -- Poliwhirl
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-31',  2),  -- Poliwrath
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-37',  4),  -- Goldeen
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-38',  4),  -- Seaking
    -- Entrenadores / Ítems (10)
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-126', 4),  -- Potion
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-127', 4),  -- Poké Ball
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-128', 2),  -- Professor's Letter
    -- Energía (34)
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-133', 17), -- Water Energy
    ((SELECT id FROM deck WHERE name = 'Mazo de Misty — Agua'), 'xy1-136', 17); -- Colorless Energy

-- =============================================================
-- Partida de ejemplo — finalizada, ash ganó
-- =============================================================
INSERT INTO game_session (
    id, player1_id, player2_id, status,
    current_player_id, prize_cards_count, winner_id,
    player1_ready, player2_ready, coin_flip_winner_id,
    created_at, finished_at
) VALUES (
    'aaaaaaaa-0000-0000-0000-000000000001',
    (SELECT id FROM player WHERE username = 'ash'),
    (SELECT id FROM player WHERE username = 'misty'),
    'FINISHED',
    NULL,
    6,
    (SELECT id FROM player WHERE username = 'ash'),
    TRUE, TRUE,
    (SELECT id FROM player WHERE username = 'ash'),
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour'
);

-- Algunas acciones del log de la partida de ejemplo
INSERT INTO game_action (game_session_id, turn_number, player_id, action_type, payload, result, timestamp) VALUES
    (
        'aaaaaaaa-0000-0000-0000-000000000001',
        1,
        (SELECT id FROM player WHERE username = 'ash'),
        'SETUP_PLACE_POKEMON',
        '{"type":"SETUP_PLACE_POKEMON","cardId":"xy1-100","targetPosition":"ACTIVE"}',
        '{"status":"SUCCESS","events":[{"type":"POKEMON_PLACED"}]}',
        NOW() - INTERVAL '115 minutes'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000001',
        1,
        (SELECT id FROM player WHERE username = 'misty'),
        'SETUP_PLACE_POKEMON',
        '{"type":"SETUP_PLACE_POKEMON","cardId":"xy1-29","targetPosition":"ACTIVE"}',
        '{"status":"SUCCESS","events":[{"type":"POKEMON_PLACED"}]}',
        NOW() - INTERVAL '114 minutes'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000001',
        2,
        (SELECT id FROM player WHERE username = 'ash'),
        'ATTACH_ENERGY',
        '{"type":"ATTACH_ENERGY","cardId":"xy1-132","targetPosition":"ACTIVE"}',
        '{"status":"SUCCESS","events":[{"type":"ENERGY_ATTACHED"}]}',
        NOW() - INTERVAL '110 minutes'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000001',
        3,
        (SELECT id FROM player WHERE username = 'ash'),
        'USE_ATTACK',
        '{"type":"USE_ATTACK","attackIndex":0}',
        '{"status":"SUCCESS","events":[{"type":"DAMAGE_DEALT","attacker":"xy1-100","defender":"xy1-29","finalDamage":30},{"type":"DAMAGE_DEALT","attacker":"xy1-100","recoilDamage":10,"hpRemaining":100}]}',
        NOW() - INTERVAL '105 minutes'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000001',
        3,
        (SELECT id FROM player WHERE username = 'misty'),
        'ATTACH_ENERGY',
        '{"type":"ATTACH_ENERGY","cardId":"xy1-133","targetPosition":"ACTIVE"}',
        '{"status":"FAILURE","message":"You have already attached an energy card this turn."}',
        NOW() - INTERVAL '104 minutes'
    );
