package ar.edu.utn.frc.tup.piii.engine.factories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerFieldBuilderTest {

    @Test
    void testBuildStandardField() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cards.add(Card.builder().id("C" + i).build());
        }

        PlayerFieldBuilder builder = new PlayerFieldBuilder();
        PlayerField field = builder
                .withPlayerId(10L)
                .withCards(cards)
                .build();

        assertEquals(10L, field.getPlayerId());
        assertNull(field.getActivePokemon());
        assertEquals(0, field.getBench().size());
        assertEquals(0, field.getDiscardPile().size());
        assertNotNull(field.getTurnFlags());
        
        // Check distributions
        assertEquals(7, field.getHand().size());
        assertEquals(6, field.getPrizeCards().size());
        assertEquals(60 - 7 - 6, field.getDeck().size()); // 47
    }

    @Test
    void testBuildSuddenDeathField() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cards.add(Card.builder().id("C" + i).build());
        }

        PlayerFieldBuilder builder = new PlayerFieldBuilder();
        PlayerField field = builder
                .withPlayerId(20L)
                .withCards(cards)
                .withSuddenDeath() // Only 1 prize card
                .build();

        assertEquals(7, field.getHand().size());
        assertEquals(1, field.getPrizeCards().size());
        assertEquals(60 - 7 - 1, field.getDeck().size()); // 52
    }

    @Test
    void testValidation() {
        PlayerFieldBuilder builder = new PlayerFieldBuilder();
        
        // Missing Player ID
        assertThrows(IllegalStateException.class, builder::build);

        // Missing Cards
        builder.withPlayerId(1L);
        assertThrows(IllegalStateException.class, builder::build);

        // Wrong amount of cards
        List<Card> wrongCards = new ArrayList<>();
        wrongCards.add(new Card());
        assertThrows(IllegalArgumentException.class, () -> builder.withCards(wrongCards));
    }
}
