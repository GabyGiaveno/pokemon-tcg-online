package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auto-executes the DRAW phase for the active player.
 *
 * <p>Rules handled:
 * <ul>
 *   <li>Increments {@code playerTurnCount} and {@code board.turnNumber}.</li>
 *   <li>Skips drawing for the first player on their very first turn (ENGINE_SPEC §1.2).</li>
 *   <li>Sets {@code board.matchState = FINISHED} and declares the opponent winner on deck-out.</li>
 * </ul>
 *
 * <p>Called automatically by {@link ar.edu.utn.frc.tup.piii.engine.TurnManager#beginTurn}.
 */
public class DrawPhaseState implements GamePhaseState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.DRAW;
    }

    @Override
    public List<GameEvent> execute(BoardState board, CardLookup cardLookup) {
        List<GameEvent> events = new ArrayList<>();
        PlayerField activeField = getActiveField(board);

        // First player's first turn: skip draw (cannot attack either — enforced by RuleValidator)
        if (activeField.getPlayerTurnCount() == 0 && !board.isFirstPlayerHasActed()) {
            // Increment counters but do NOT draw a card
            activeField.setPlayerTurnCount(1);
            board.setTurnNumber(board.getTurnNumber() + 1);
            return events;
        }

        // Increment counters at the very start of the draw phase
        activeField.setPlayerTurnCount(activeField.getPlayerTurnCount() + 1);
        board.setTurnNumber(board.getTurnNumber() + 1);

        // Deck-out: the player attempting to draw from an empty deck loses
        if (activeField.getDeck().isEmpty()) {
            Long winnerId = getOpponentField(board).getPlayerId();
            board.setMatchState(GameStatus.FINISHED);
            board.setWinnerId(winnerId);
            board.setFinishedReason("DECK_OUT");
            events.add(GameEvent.of(GameEventType.DECK_OUT,
                    "Player " + activeField.getPlayerId() + " has no cards to draw. "
                            + "Player " + winnerId + " wins!",
                    Map.of("losingPlayerId", activeField.getPlayerId(), "winnerId", winnerId)));
            return events;
        }

        // Draw 1 card from the top of the deck
        String drawnCardId = activeField.getDeck().remove(0);
        activeField.getHand().add(drawnCardId);

        // Note: we intentionally omit the cardId from the event payload to preserve
        // information hiding – the opponent must NOT see what was drawn.
        events.add(GameEvent.of(GameEventType.CARD_DRAWN,
                "Player " + activeField.getPlayerId() + " drew a card.",
                Map.of("playerId", activeField.getPlayerId(),
                        "cardsInDeck", activeField.getDeck().size())));

        return events;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PlayerField getActiveField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }

    private PlayerField getOpponentField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer2Field()
                : board.getPlayer1Field();
    }
}
