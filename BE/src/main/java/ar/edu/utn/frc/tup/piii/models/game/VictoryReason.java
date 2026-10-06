package ar.edu.utn.frc.tup.piii.models.game;

/**
 * Why the game ended, as decided by {@link ar.edu.utn.frc.tup.piii.engine.VictoryConditionChecker}.
 *
 * <p>A Knockout only <em>enables</em> a win; the orchestrator runs the checker afterwards and,
 * if a condition holds, the returned reason lets it narrate a precise {@code GAME_FINISHED} event.
 *
 * <ul>
 *   <li>{@code ALL_PRIZES_TAKEN} – a player took their last Prize Card.</li>
 *   <li>{@code NO_POKEMON_LEFT} – a player has no Active and an empty Bench (cannot promote).</li>
 * </ul>
 */
public enum VictoryReason {
    ALL_PRIZES_TAKEN,
    NO_POKEMON_LEFT,
    /** Both active Pokémon were knocked out simultaneously with 1 prize card left each — triggers Sudden Death. */
    SIMULTANEOUS_KO;

    /** Human-readable narration of this victory for a {@code GAME_FINISHED} event. */
    public String describe(Long winnerId) {
        return switch (this) {
            case ALL_PRIZES_TAKEN -> "Player " + winnerId + " took all prize cards and wins!";
            case NO_POKEMON_LEFT  -> "Player " + winnerId + " wins — the opponent has no Pokémon left.";
            case SIMULTANEOUS_KO  -> "Both players were knocked out simultaneously — Sudden Death begins!";
        };
    }
}
