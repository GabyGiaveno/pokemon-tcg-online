package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.LookAtDeckEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LookAtDeckLogicTest {

    private LookAtDeckLogic logic;
    private AttackContext ctx;
    private PlayerField attackerField;
    private BoardState board;

    @BeforeEach
    void setUp() {
        logic = new LookAtDeckLogic();

        attackerField = new PlayerField();
        attackerField.setPlayerId(1L);
        attackerField.setTurnFlags(new TurnFlags());
        attackerField.setDeck(new ArrayList<>(List.of("inst-A", "inst-B", "inst-C", "inst-D", "inst-E")));

        board = new BoardState();

        ctx = AttackContext.builder()
                .attackerField(attackerField)
                .attackerPlayerId(1L)
                .board(board)
                .build();
    }

    private LookAtDeckEffect effect(int amount, boolean reorder) {
        LookAtDeckEffect e = new LookAtDeckEffect();
        e.setAmount(amount);
        e.setReorder(reorder);
        return e;
    }

    @Test
    void execute_peeksCorrectNumberOfCards() {
        logic.execute(effect(3, false), ctx);

        assertEquals(1, ctx.getEvents().size());
        assertEquals(GameEventType.DECK_PEEKED, ctx.getEvents().get(0).getType());
    }

    @Test
    void execute_peekEventContainsPeekedInstanceIds() {
        logic.execute(effect(3, false), ctx);

        Object ids = ctx.getEvents().get(0).getData().get("instanceIds");
        assertEquals(List.of("inst-A", "inst-B", "inst-C"), ids);
    }

    @Test
    void execute_withoutReorder_doesNotSetPendingSelection() {
        logic.execute(effect(3, false), ctx);

        assertNull(board.getPendingSelection());
    }

    @Test
    void execute_withReorder_setsPendingSelection() {
        logic.execute(effect(3, true), ctx);

        assertNotNull(board.getPendingSelection());
        assertEquals(SelectionType.REORDER_DECK, board.getPendingSelection().getType());
        assertEquals(1L, board.getPendingSelection().getOwnerPlayerId());
        assertEquals(List.of("inst-A", "inst-B", "inst-C"), board.getPendingSelection().getValidOptions());
    }

    @Test
    void execute_deckSmallerThanAmount_peeksWhatIsAvailable() {
        attackerField.setDeck(new ArrayList<>(List.of("inst-X", "inst-Y")));

        logic.execute(effect(5, true), ctx);

        assertEquals(List.of("inst-X", "inst-Y"), board.getPendingSelection().getValidOptions());
    }

    @Test
    void execute_emptyDeck_noEventNoSelection() {
        attackerField.setDeck(new ArrayList<>());

        logic.execute(effect(3, true), ctx);

        assertTrue(ctx.getEvents().isEmpty());
        assertNull(board.getPendingSelection());
    }

    @Test
    void execute_doesNotModifyDeck() {
        logic.execute(effect(3, true), ctx);

        // Deck must remain untouched until the player resolves the reorder.
        assertEquals(List.of("inst-A", "inst-B", "inst-C", "inst-D", "inst-E"), attackerField.getDeck());
    }
}
