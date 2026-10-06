package ar.edu.utn.frc.tup.piii.models.game;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** In-memory model of the complete game board: both player fields, turn tracking, and match state. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardState {

    /** Overall match state (WAITING → SETUP → ACTIVE → FINISHED). */
    private GameStatus matchState;

    /** Current turn phase (DRAW / MAIN / ATTACK / BETWEEN_TURNS). */
    private TurnPhase currentPhase;

    /** ID of the player whose turn it currently is. */
    private Long currentPlayerId;

    /**
     * Global turn counter. Incremented at the start of each player's DRAW phase.
     * Player 1's first turn = 1, Player 2's first turn = 2, etc.
     */
    private int turnNumber;

    /**
     * Becomes {@code true} after the first player completes their very first BETWEEN_TURNS phase.
     * Used to enforce the "first player cannot attack on turn 1" rule and to know when
     * Player 2's draw phase should NOT skip drawing.
     */
    private boolean firstPlayerHasActed;

    /** ID of the player who won. Null while the game is in progress. */
    private Long winnerId;
    /** Why the game ended (VictoryReason.name(), "CONCEDE" or "DECK_OUT"); null while running. */
    private String finishedReason;

    private PlayerField player1Field;
    private PlayerField player2Field;

    /** Card ID of the Stadium currently in play. Null if no Stadium is active. */
    private String activeStadiumCardId;

    /**
     * A player choice the engine is waiting on mid-resolution. Null when no selection is pending.
     * While non-null, only {@code RESOLVE_SELECTION} actions are accepted; the rest of the flow
     * (e.g. promoting a Bench Pokémon, completing the turn) is paused until it is resolved.
     */
    private PendingSelection pendingSelection;

    /**
     * Tracks which players have completed their initial setup (placed Active + optional Bench).
     * Used by SetupPhaseState to know when both players are ready to start the game.
     * Empty/non-null during ACTIVE phase; populated during SETUP phase.
     */
    @Builder.Default
    private List<Long> setupCompletedPlayers = new ArrayList<>();
}
