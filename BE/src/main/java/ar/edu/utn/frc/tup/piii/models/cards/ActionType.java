package ar.edu.utn.frc.tup.piii.models.cards;

/** Domain enum: DRAW_CARD, PLAY_BASIC_POKEMON, EVOLVE_POKEMON, ATTACH_ENERGY, USE_ATTACK, RETREAT,
 * PLAY_ITEM, PLAY_SUPPORTER, PLAY_STADIUM, ATTACH_TOOL, TAKE_PRIZE_CARD, SETUP_PLACE_POKEMON,
 * SETUP_SET_PRIZES, END_TURN, CONCEDE, RESOLVE_SELECTION. */
public enum ActionType {
    DRAW_CARD,
    PLAY_BASIC_POKEMON,
    EVOLVE_POKEMON,
    ATTACH_ENERGY,
    USE_ATTACK,
    RETREAT,
    PLAY_ITEM,
    PLAY_SUPPORTER,
    PLAY_STADIUM,
    ATTACH_TOOL,
    TAKE_PRIZE_CARD,
    SETUP_PLACE_POKEMON,
    SETUP_SET_PRIZES,
    END_TURN,
    CONCEDE,

    /** Resolves a {@code PendingSelection} (e.g. choosing which Bench Pokémon to promote after a KO).
     *  Carries the player's choice in {@code ActionRequest.benchIndex}. */
    RESOLVE_SELECTION,

    /** Activates an ability of a Pokémon in play ("Once during your turn…" abilities).
     *  Source in {@code ActionRequest.targetPosition}; ability in {@code abilityIndex} (default 0). */
    USE_ABILITY
}
