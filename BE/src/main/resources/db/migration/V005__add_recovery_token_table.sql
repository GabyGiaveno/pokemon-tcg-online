-- =============================================================
-- V005__add_recovery_token_table.sql
-- Token de recuperación de contraseña — PostgreSQL
-- =============================================================

CREATE TABLE recovery_token (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    player_id  BIGINT       NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_recovery_token     UNIQUE (token),
    CONSTRAINT fk_recovery_token_player FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE
);

CREATE INDEX idx_recovery_token_player_id ON recovery_token (player_id);

COMMENT ON TABLE  recovery_token                IS 'Token de un solo uso para restablecimiento de contraseña. Se invalida post-uso o al expirar.';
COMMENT ON COLUMN recovery_token.token          IS 'UUID generado como token seguro.';
COMMENT ON COLUMN recovery_token.player_id      IS 'Jugador dueño del token.';
COMMENT ON COLUMN recovery_token.expires_at     IS 'Fecha de expiración del token (30 min desde creación).';
COMMENT ON COLUMN recovery_token.used           IS 'TRUE si el token ya fue utilizado. Los tokens usados se rechazan.';
