package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DrawPhaseState}.
 *
 * NOTA: PlayerField.deck y .hand son List&lt;String&gt; (card IDs), NO List&lt;CardInstanceState&gt;.
 * BoardState NO tiene campo finishedReason (eso es de GameBoardState).
 */
class DrawPhaseStateTest {

    private DrawPhaseState drawPhaseState;
    private BoardState board;
    private PlayerField currentField;
    private PlayerField opponentField;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        drawPhaseState = new DrawPhaseState();
        board = new BoardState();

        currentField = new PlayerField();
        currentField.setPlayerId(1L);
        currentField.setDeck(new ArrayList<>());
        currentField.setHand(new ArrayList<>());

        opponentField = new PlayerField();
        opponentField.setPlayerId(2L);

        board.setPlayer1Field(currentField);
        board.setPlayer2Field(opponentField);
        board.setCurrentPlayerId(1L);
        board.setMatchState(GameStatus.ACTIVE);
        board.setFirstPlayerHasActed(true);

        cardLookup = Mockito.mock(CardLookup.class);
    }

    @Test
    void execute_firstTurn_doesNotDraw() {
        board.setFirstPlayerHasActed(false);
        // playerTurnCount defaults to 0 — code will detect first turn and skip drawing

        List<GameEvent> events = drawPhaseState.execute(board, cardLookup);

        assertTrue(events.isEmpty());
        assertEquals(0, currentField.getHand().size());
        assertEquals(GameStatus.ACTIVE, board.getMatchState());
    }

    @Test
    void execute_normalTurn_drawsCard() {
        currentField.getDeck().add("card1");

        List<GameEvent> events = drawPhaseState.execute(board, cardLookup);

        assertEquals(1, events.size());
        assertEquals(GameEventType.CARD_DRAWN, events.get(0).getType());
        assertEquals(1, currentField.getHand().size());
        assertEquals(0, currentField.getDeck().size());
        assertEquals("card1", currentField.getHand().get(0));
        assertEquals(GameStatus.ACTIVE, board.getMatchState());
    }

    @Test
    void execute_deckOut_playerLoses() {
        // Deck vacío
        List<GameEvent> events = drawPhaseState.execute(board, cardLookup);

        assertEquals(1, events.size());
        assertEquals(GameEventType.DECK_OUT, events.get(0).getType());

        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(2L, board.getWinnerId()); // Player 2 gana porque Player 1 deckeó
    }
}
