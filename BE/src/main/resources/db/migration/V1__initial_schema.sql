-- =============================================================
-- V1__initial_schema.sql
-- Schema inicial — Pokémon TCG Digital
-- PostgreSQL 15+ | Flyway migration V1
-- =============================================================

-- =============================================================
-- Enum-backed columns
-- =============================================================
-- Los enums de dominio (game_status, turn_phase, action_type) se almacenan como
-- VARCHAR y son mapeados por Hibernate vía @Enumerated(EnumType.STRING). NO se usan
-- tipos ENUM nativos de PostgreSQL porque Hibernate enviaría los valores como
-- VARCHAR y PostgreSQL no castea VARCHAR -> ENUM implícitamente (rompería los INSERT),
-- y además 'ddl-auto: validate' espera VARCHAR para una columna @Enumerated(STRING).
-- Esto refleja exactamente el esquema que Hibernate genera sobre H2.
--
-- special_condition y energy_type vivían dentro de game_state.state_json (JSONB),
-- nunca fueron columnas, por eso no se declaran aquí.

-- =============================================================
-- card_set — sets de cartas cacheados de pokemontcg.io
-- =============================================================

CREATE TABLE card_set (
    id            VARCHAR(20)  PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    series        VARCHAR(100),
    printed_total SMALLINT,
    release_date  DATE
);

COMMENT ON TABLE  card_set               IS 'Sets de cartas Pokémon TCG cacheados desde pokemontcg.io.';
COMMENT ON COLUMN card_set.id            IS 'ID del set en la API externa (e.g. "xy1", "base1").';
COMMENT ON COLUMN card_set.printed_total IS 'Total de cartas imprimidas en el set según la API.';

-- =============================================================
-- card — catálogo de cartas, caché local de pokemontcg.io
-- =============================================================

CREATE TABLE card (
    id               VARCHAR(20)  PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    supertype        VARCHAR(20)  NOT NULL,
    subtypes         TEXT[]       NOT NULL DEFAULT '{}',
    hp               SMALLINT,
    types            TEXT[]       NOT NULL DEFAULT '{}',
    attacks          JSONB        NOT NULL DEFAULT '[]',
    weaknesses       JSONB        NOT NULL DEFAULT '[]',
    resistances      JSONB        NOT NULL DEFAULT '[]',
    retreat_cost     TEXT[]       NOT NULL DEFAULT '{}',
    evolves_from     VARCHAR(200),
    set_id           VARCHAR(20)  NOT NULL,
    image_url_large  TEXT,
    image_url_small  TEXT,
    is_ace_tactician BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_card_set FOREIGN KEY (set_id) REFERENCES card_set(id)
);

COMMENT ON TABLE  card                   IS 'Catálogo de cartas cacheado desde pokemontcg.io. No se consulta la API externa durante el juego.';
COMMENT ON COLUMN card.id                IS 'ID de la carta en la API externa (e.g. "xy1-1").';
COMMENT ON COLUMN card.supertype         IS 'Supertipo: POKEMON, TRAINER o ENERGY.';
COMMENT ON COLUMN card.subtypes          IS 'Subtipos: Basic, Stage 1, Stage 2, EX, MEGA, Item, Supporter, Stadium, etc.';
COMMENT ON COLUMN card.attacks           IS 'Array JSON: [{name, cost[], damage, text}]. Nullable para Entrenadores y Energía.';
COMMENT ON COLUMN card.weaknesses        IS 'Array JSON: [{type, value}].';
COMMENT ON COLUMN card.resistances       IS 'Array JSON: [{type, value}].';
COMMENT ON COLUMN card.is_ace_tactician  IS 'TRUE para cartas AS TÁCTICO. Solo puede haber 1 por mazo.';

CREATE INDEX idx_card_name   ON card (name);
CREATE INDEX idx_card_set_id ON card (set_id);

-- =============================================================
-- player — jugadores registrados
-- =============================================================

CREATE TABLE player (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_player_username UNIQUE (username),
    CONSTRAINT uq_player_email    UNIQUE (email)
);

COMMENT ON TABLE  player            IS 'Jugadores registrados en el sistema.';
COMMENT ON COLUMN player.username   IS 'Nombre de usuario único. Se muestra en la sala de espera y durante el juego.';
COMMENT ON COLUMN player.created_at IS 'Fecha de registro del jugador.';

-- =============================================================
-- deck — mazos de un jugador
-- =============================================================

CREATE TABLE deck (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    player_id  BIGINT       NOT NULL,
    is_valid   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_deck_player FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE
);

COMMENT ON TABLE  deck          IS 'Mazos de 60 cartas. Las reglas de validación se enforcan en DeckService.';
COMMENT ON COLUMN deck.is_valid IS 'TRUE si cumple: 60 cartas, máx 4 copias por nombre (Energía Básica ilimitada), máx 1 AS TÁCTICO, mínimo 1 Pokémon Básico.';

CREATE INDEX idx_deck_player_id ON deck (player_id);

-- =============================================================
-- deck_card — composición de cada mazo
-- =============================================================

CREATE TABLE deck_card (
    id       BIGSERIAL   PRIMARY KEY,
    deck_id  BIGINT      NOT NULL,
    card_id  VARCHAR(20) NOT NULL,
    quantity SMALLINT    NOT NULL,

    CONSTRAINT fk_deck_card_deck FOREIGN KEY (deck_id) REFERENCES deck(id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_card_card FOREIGN KEY (card_id) REFERENCES card(id),
    CONSTRAINT uq_deck_card      UNIQUE (deck_id, card_id),
    CONSTRAINT chk_quantity      CHECK (quantity >= 1)
);

COMMENT ON TABLE  deck_card          IS 'Composición de un mazo. La suma de quantity para un deck_id debe ser 60 (validado en DeckService).';
COMMENT ON COLUMN deck_card.quantity IS 'Cantidad de copias de esta carta en el mazo. Mínimo 1. Máximo controlado por reglas de negocio.';

-- =============================================================
-- game_session — partidas activas e históricas
-- =============================================================

CREATE TABLE game_session (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    player1_id        BIGINT      NOT NULL,
    player2_id        BIGINT,
    status            VARCHAR(255) NOT NULL DEFAULT 'WAITING',
    current_player_id BIGINT,
    prize_cards_count SMALLINT    NOT NULL DEFAULT 6,
    winner_id         BIGINT,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    finished_at       TIMESTAMP,

    CONSTRAINT fk_session_player1  FOREIGN KEY (player1_id)        REFERENCES player(id),
    CONSTRAINT fk_session_player2  FOREIGN KEY (player2_id)        REFERENCES player(id),
    CONSTRAINT fk_session_winner   FOREIGN KEY (winner_id)         REFERENCES player(id),
    CONSTRAINT chk_prize_count     CHECK (prize_cards_count IN (1, 6))
);

COMMENT ON TABLE  game_session                   IS 'Partida entre dos jugadores. prize_cards_count = 6 en partida normal, 1 en Muerte Súbita.';
COMMENT ON COLUMN game_session.player2_id        IS 'NULL mientras la partida está en WAITING esperando un oponente.';
COMMENT ON COLUMN game_session.current_player_id IS 'ID del jugador cuyo turno está activo. NULL fuera de la fase ACTIVE.';
COMMENT ON COLUMN game_session.prize_cards_count IS 'Solo admite 6 (partida normal) o 1 (Muerte Súbita).';
COMMENT ON COLUMN game_session.finished_at       IS 'NULL mientras la partida está en curso.';

CREATE INDEX idx_game_session_status  ON game_session (status);
CREATE INDEX idx_game_session_player1 ON game_session (player1_id);
CREATE INDEX idx_game_session_player2 ON game_session (player2_id);

-- =============================================================
-- game_state — snapshot del tablero (1:1 con game_session)
-- =============================================================

CREATE TABLE game_state (
    id              BIGSERIAL  PRIMARY KEY,
    game_session_id UUID       NOT NULL,
    state_json      JSONB      NOT NULL,
    turn_number     INT        NOT NULL DEFAULT 0,
    phase           VARCHAR(255) NOT NULL DEFAULT 'DRAW',
    updated_at      TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_state_session FOREIGN KEY (game_session_id) REFERENCES game_session(id) ON DELETE CASCADE,
    CONSTRAINT uq_state_session UNIQUE (game_session_id)
);

COMMENT ON TABLE  game_state                IS 'Snapshot JSONB del tablero completo. Se sobreescribe tras cada acción válida. Relación 1:1 con game_session.';
COMMENT ON COLUMN game_state.state_json     IS 'Estado completo: manos (sin orden), mazos (sin orden visible), bench, Activos, energías adjuntas, condiciones especiales, Prize Cards ocultas.';
COMMENT ON COLUMN game_state.turn_number    IS 'Número de turno actual. Incrementa al inicio de cada turno (fase DRAW).';

-- =============================================================
-- game_action — log de acciones inmutable (solo append)
-- =============================================================

CREATE TABLE game_action (
    id              BIGSERIAL   PRIMARY KEY,
    game_session_id UUID        NOT NULL,
    turn_number     INT         NOT NULL,
    player_id       BIGINT      NOT NULL,
    action_type     VARCHAR(255) NOT NULL,
    payload         JSONB       NOT NULL DEFAULT '{}',
    result          JSONB       NOT NULL DEFAULT '{}',
    timestamp       TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_action_session FOREIGN KEY (game_session_id) REFERENCES game_session(id),
    CONSTRAINT fk_action_player  FOREIGN KEY (player_id)        REFERENCES player(id)
);

COMMENT ON TABLE  game_action                IS 'Log inmutable de todas las acciones de juego. Solo INSERT — nunca UPDATE ni DELETE. Permite replay y auditoría completa.';
COMMENT ON COLUMN game_action.payload        IS 'Input de la acción tal como fue enviada por el jugador.';
COMMENT ON COLUMN game_action.result         IS 'Resultado calculado por el GameEngine tras aplicar la acción al estado.';
COMMENT ON COLUMN game_action.action_type    IS 'Tipo de acción ejecutada. Ver enum action_type.';

CREATE INDEX idx_game_action_session      ON game_action (game_session_id);
CREATE INDEX idx_game_action_session_turn ON game_action (game_session_id, turn_number);
