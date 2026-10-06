package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.CoinFlipTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DrawCardsTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Roller Skates: flip a coin, if heads draw 3 (sub-effect dispatched via the registry). */
class CoinFlipTrainerLogicTest {

    private BoardState boardWithDeck(int deckSize) {
        List<String> deck = new ArrayList<>();
        for (int i = 1; i <= deckSize; i++) deck.add("d" + i);
        PlayerField p1 = PlayerField.builder()
                .playerId(1L).hand(new ArrayList<>()).deck(deck).discardPile(new ArrayList<>())
                .bench(new ArrayList<>()).build();
        PlayerField p2 = PlayerField.builder()
                .playerId(2L).hand(new ArrayList<>()).deck(new ArrayList<>()).discardPile(new ArrayList<>())
                .bench(new ArrayList<>()).build();
        return BoardState.builder().currentPlayerId(1L).player1Field(p1).player2Field(p2).build();
    }

    private CoinFlipTrainerEffect rollerSkates() {
        DrawCardsTrainerEffect draw = new DrawCardsTrainerEffect();
        draw.setAmount(3);
        CoinFlipTrainerEffect coin = new CoinFlipTrainerEffect();
        coin.setIfHeads(List.of(draw));
        coin.setIfTails(List.of());
        return coin;
    }

    @Test
    void heads_drawsThree() {
        Random random = mock(Random.class);
        when(random.nextBoolean()).thenReturn(true);

        BoardState board = boardWithDeck(10);
        new CoinFlipTrainerLogic(random).execute(rollerSkates(), board, 1L);

        assertEquals(3, board.getPlayer1Field().getHand().size());
    }

    @Test
    void tails_drawsNothing() {
        Random random = mock(Random.class);
        when(random.nextBoolean()).thenReturn(false);

        BoardState board = boardWithDeck(10);
        new CoinFlipTrainerLogic(random).execute(rollerSkates(), board, 1L);

        assertEquals(0, board.getPlayer1Field().getHand().size());
    }
}
