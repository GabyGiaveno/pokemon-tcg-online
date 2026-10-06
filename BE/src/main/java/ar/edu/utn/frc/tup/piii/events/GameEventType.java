package ar.edu.utn.frc.tup.piii.events;

/**
 * All discrete events the GameEngine can emit during a game.
 * Published by GameEngineFacade via GameEventPublisher to connected WebSocket clients.
 */
public enum GameEventType {

    // Turn flow
    PHASE_CHANGED,
    TURN_ENDED,

    // Draw phase
    CARD_DRAWN,
    DECK_OUT,

    // Setup phase
    POKEMON_PLAYED_TO_ACTIVE,

    // Main phase – playing cards
    POKEMON_PLAYED_TO_BENCH,
    POKEMON_EVOLVED,
    ENERGY_ATTACHED,
    ENERGY_DISCARDED,
    TOOL_ATTACHED,
    ITEM_PLAYED,
    SUPPORTER_PLAYED,
    STADIUM_PLAYED,
    POKEMON_RETREATED,

    // Attack phase
    ATTACK_DECLARED,
    COIN_FLIPPED,
    DAMAGE_DEALT,
    DAMAGE_PREVENTED,
    POKEMON_HEALED,
    POKEMON_RESTRICTED,

    // Status effects (between-turns)
    STATUS_EFFECT_APPLIED,
    STATUS_EFFECT_CLEARED,

    // Abilities
    ABILITY_USED,

    // KO and prizes
    POKEMON_KNOCKED_OUT,
    PRIZE_TAKEN,

    // Deck interactions
    DECK_PEEKED,
    DECK_SEARCHED,
    CARD_DISCARDED,
    POKEMON_PLACED,
    POKEMON_SHUFFLED_INTO_DECK,

    // Setup complete
    SETUP_COMPLETE,

    // Game-over
    GAME_FINISHED,
    PLAYER_CONCEDED,

    // Sudden Death
    SUDDEN_DEATH_START
}
