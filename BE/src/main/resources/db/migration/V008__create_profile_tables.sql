-- =============================================================
-- V008__create_profile_tables.sql
-- Creates player_stats, badge, player_badge, achievement,
-- player_achievement, trainer_skin, player_skin,
-- customization_item, player_customization tables
-- PostgreSQL 15+ | Flyway migration V008
-- =============================================================

-- =============================================================
-- player_stats — competitive statistics per player
-- =============================================================

CREATE TABLE player_stats (
    player_id       BIGINT  PRIMARY KEY,
    wins            INTEGER NOT NULL DEFAULT 0,
    losses          INTEGER NOT NULL DEFAULT 0,
    streak          INTEGER NOT NULL DEFAULT 0,
    tournaments_won INTEGER NOT NULL DEFAULT 0,
    packs_opened    INTEGER NOT NULL DEFAULT 0,
    total_cards     INTEGER NOT NULL DEFAULT 0,
    decks_created   INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_player_stats_player FOREIGN KEY (player_id)
        REFERENCES player(id) ON DELETE CASCADE
);

COMMENT ON TABLE  player_stats            IS 'Competitive statistics for each player. 1:1 with player.';
COMMENT ON COLUMN player_stats.wins       IS 'Total number of match wins.';
COMMENT ON COLUMN player_stats.losses     IS 'Total number of match losses.';
COMMENT ON COLUMN player_stats.streak     IS 'Current consecutive win streak. Resets to 0 on loss.';

-- =============================================================
-- badge — catalogue of gym badges available in the system
-- =============================================================

CREATE TABLE badge (
    id            VARCHAR(20)  PRIMARY KEY,
    label         VARCHAR(100) NOT NULL,
    description   TEXT         NOT NULL,
    icon          VARCHAR(10)  NOT NULL,
    how_to_unlock TEXT         NOT NULL
);

COMMENT ON TABLE  badge IS 'Master catalogue of gym badges. Data is seeded on deploy.';

-- =============================================================
-- player_badge — badges earned by each player
-- =============================================================

CREATE TABLE player_badge (
    player_id  BIGINT      NOT NULL,
    badge_id   VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP  NOT NULL DEFAULT NOW(),

    PRIMARY KEY (player_id, badge_id),
    CONSTRAINT fk_player_badge_player FOREIGN KEY (player_id)
        REFERENCES player(id) ON DELETE CASCADE,
    CONSTRAINT fk_player_badge_badge  FOREIGN KEY (badge_id)
        REFERENCES badge(id)
);

COMMENT ON TABLE player_badge IS 'Junction: which badges each player has unlocked.';

-- =============================================================
-- achievement — catalogue of achievements available
-- =============================================================

CREATE TABLE achievement (
    id          VARCHAR(30) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    icon        VARCHAR(10)  NOT NULL
);

COMMENT ON TABLE achievement IS 'Master catalogue of achievements. Data is seeded on deploy.';

-- =============================================================
-- player_achievement — achievements unlocked by each player
-- =============================================================

CREATE TABLE player_achievement (
    player_id     BIGINT      NOT NULL,
    achievement_id VARCHAR(30) NOT NULL,
    unlocked_at   TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (player_id, achievement_id),
    CONSTRAINT fk_player_achievement_player FOREIGN KEY (player_id)
        REFERENCES player(id) ON DELETE CASCADE,
    CONSTRAINT fk_player_achievement_achievement FOREIGN KEY (achievement_id)
        REFERENCES achievement(id)
);

COMMENT ON TABLE player_achievement IS 'Junction: which achievements each player has unlocked.';

-- =============================================================
-- trainer_skin — catalogue of equippable trainer skins
-- =============================================================

CREATE TABLE trainer_skin (
    id          VARCHAR(20) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    hat_color   VARCHAR(7)  NOT NULL,
    shirt_color VARCHAR(7)  NOT NULL,
    pants_color VARCHAR(7)  NOT NULL,
    skin_tone   VARCHAR(7)  NOT NULL
);

COMMENT ON TABLE trainer_skin IS 'Master catalogue of trainer skins with hex colour values for each layer.';

-- =============================================================
-- player_skin — skins owned and equipped by each player
-- =============================================================

CREATE TABLE player_skin (
    player_id BIGINT      NOT NULL,
    skin_id   VARCHAR(20) NOT NULL,
    equipped  BOOLEAN     NOT NULL DEFAULT FALSE,

    PRIMARY KEY (player_id, skin_id),
    CONSTRAINT fk_player_skin_player FOREIGN KEY (player_id)
        REFERENCES player(id) ON DELETE CASCADE,
    CONSTRAINT fk_player_skin_skin   FOREIGN KEY (skin_id)
        REFERENCES trainer_skin(id)
);

COMMENT ON TABLE player_skin IS 'Junction: which skins each player owns, and which one is currently equipped.';

CREATE UNIQUE INDEX uq_player_skin_equipped
    ON player_skin (player_id, equipped)
    WHERE equipped = TRUE;

COMMENT ON INDEX uq_player_skin_equipped IS 'Ensures each player has at most one equipped skin at a time.';

-- =============================================================
-- customization_item — catalogue of equippable items (clothes, accessories, poses, backgrounds)
-- =============================================================

CREATE TABLE customization_item (
    id       VARCHAR(30) PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    category VARCHAR(20)  NOT NULL,
    color    VARCHAR(7),
    icon_url VARCHAR(255),

    CONSTRAINT chk_customization_category
        CHECK (category IN ('clothes', 'accessory', 'pose', 'background'))
);

COMMENT ON TABLE customization_item IS 'Master catalogue of equippable items for the trainer profile (clothes, accessories, poses, backgrounds).';

-- =============================================================
-- player_customization — items owned by each player
-- =============================================================

CREATE TABLE player_customization (
    player_id BIGINT      NOT NULL,
    item_id   VARCHAR(30) NOT NULL,

    PRIMARY KEY (player_id, item_id),
    CONSTRAINT fk_player_customization_player FOREIGN KEY (player_id)
        REFERENCES player(id) ON DELETE CASCADE,
    CONSTRAINT fk_player_customization_item   FOREIGN KEY (item_id)
        REFERENCES customization_item(id)
);

COMMENT ON TABLE player_customization IS 'Junction: which customization items each player has unlocked.';
