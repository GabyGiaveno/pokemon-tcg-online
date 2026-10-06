package ar.edu.utn.frc.tup.piii.models.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory model of one player's side: Active Pokémon, Bench, Hand, Deck, Discard, Prize slots, and turn tracking. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerField {

    private Long playerId;

    private ActivePokemon activePokemon;

    /** Bench slots — maximum 5 Pokémon. */
    private List<BenchPokemon> bench;

    /** Card IDs in the player's hand. */
    private List<String> hand;

    /** Card IDs in the deck (internal order — never exposed to the opponent). */
    private List<String> deck;

    /** Card IDs in the Prize zone (face-down until taken). */
    private List<String> prizeCards;

    /** Card IDs in the discard pile (visible to both players). */
    private List<String> discardPile;

    /** Per-turn boolean flags; reset to all-false at the start of each DRAW phase. */
    private TurnFlags turnFlags;

    /**
     * How many DRAW phases this player has completed (incremented at the start of each draw).
     * Used to enforce: cannot evolve or attack on the player's first turn (playerTurnCount == 1).
     */
    private int playerTurnCount;

    /**
     * Player-level restrictions (e.g. "SUPPORTER") set by RESTRICT effects on the opponent.
     * Cleared at the start of this player's next DRAW phase.
     */
    @Builder.Default
    private Set<String> playerRestrictions = new HashSet<>();

    /**
     * Read-only lookup: instanceId → cardId for every card owned by this player across all zones.
     * Populated by {@code EngineStateMapper} when loading state from the DB.
     * Engine effects that need the real cardId for a given instanceId (e.g. LookAtDeckLogic)
     * use this map without touching the DB.
     */
    @Builder.Default
    private Map<String, String> instanceCardIds = new java.util.HashMap<>();
}
