package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DrawCardsTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrawCardsTrainerLogicTest {

    private DrawCardsTrainerLogic logic;
    private BoardState board;
    private PlayerField field;

    @BeforeEach
    void setUp() {
        logic = new DrawCardsTrainerLogic();
        board = new BoardState();
        field = new PlayerField();
        field.setPlayerId(1L);
        board.setPlayer1Field(field);

        // Populate deck with 5 mock cards
        List<String> deck = new ArrayList<>();
        deck.add("card-1");
        deck.add("card-2");
        deck.add("card-3");
        deck.add("card-4");
        deck.add("card-5");
        field.setDeck(deck);
        
        field.setHand(new ArrayList<>());
    }

    @Test
    void testDrawCardsSuccessfully() {
        DrawCardsTrainerEffect effect = new DrawCardsTrainerEffect();
        effect.setAmount(3);

        List<GameEvent> events = logic.execute(effect, board, 1L);

        assertEquals(3, field.getHand().size());
        assertEquals(2, field.getDeck().size());
        assertEquals("card-1", field.getHand().get(0));
        assertEquals("card-3", field.getHand().get(2));
        
        assertEquals(1, events.size());
        assertEquals("Player drew 3 cards.", events.get(0).getDescription());
    }

    @Test
    void testDrawMoreCardsThanDeckHas() {
        DrawCardsTrainerEffect effect = new DrawCardsTrainerEffect();
        effect.setAmount(10); // Trying to draw 10, but only 5 in deck

        List<GameEvent> events = logic.execute(effect, board, 1L);

        assertEquals(5, field.getHand().size());
        assertEquals(0, field.getDeck().size());
        
        assertEquals(1, events.size());
        assertEquals("Player drew 5 cards.", events.get(0).getDescription());
    }
}
