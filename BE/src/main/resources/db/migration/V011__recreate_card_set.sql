-- V004 dropped the card_set table but the CardSet JPA entity still exists.
-- Recreate it to align the schema with the entity model.
CREATE TABLE card_set (
    id            VARCHAR(20)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    series        VARCHAR(100),
    printed_total INTEGER      NOT NULL DEFAULT 0,
    release_date  TIMESTAMP,

    CONSTRAINT pk_card_set PRIMARY KEY (id)
);
