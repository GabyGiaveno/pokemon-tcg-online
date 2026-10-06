package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;

import java.util.List;

/**
 * State pattern contract for each turn phase.
 *
 * <p>Two kinds of phases exist:
 * <ul>
 *   <li><b>Interactive</b> (MAIN, ATTACK) – driven by the player; override {@link #handle}.</li>
 *   <li><b>Automatic</b> (DRAW, BETWEEN_TURNS) – driven by the system; override {@link #execute}.</li>
 * </ul>
 *
 * Default implementations throw {@link UnsupportedOperationException} so that
 * calling the wrong method on a phase fails loudly.
 */
public interface GamePhaseState {

    /** Returns the {@link TurnPhase} constant this state represents. */
    TurnPhase getPhase();

    /**
     * Processes a player-driven action (MAIN / ATTACK phases).
     *
     * @param action     the player's requested action
     * @param board      current board state (mutated in place on success)
     * @param playerId   ID of the player sending the action
     * @param cardLookup bridge to resolve card entities by ID
     * @return result containing success flag, error message, and events produced
     */
    default ActionResult handle(ActionRequest action, BoardState board,
                                Long playerId, CardLookup cardLookup) {
        throw new UnsupportedOperationException(
                "Phase " + getPhase() + " does not accept player actions.");
    }

    /**
     * Auto-executes this phase without player input (DRAW / BETWEEN_TURNS phases).
     *
     * @param board      current board state (mutated in place)
     * @param cardLookup bridge to resolve card entities by ID
     * @return ordered list of events produced during execution
     */
    default List<GameEvent> execute(BoardState board, CardLookup cardLookup) {
        throw new UnsupportedOperationException(
                "Phase " + getPhase() + " is not an automatic phase.");
    }
}
