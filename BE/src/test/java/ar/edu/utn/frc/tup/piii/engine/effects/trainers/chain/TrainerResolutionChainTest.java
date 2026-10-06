package ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerResolutionChainTest {

    private final TrainerResolutionChain chain = new TrainerResolutionChain();

    private List<String> cards(String prefix, int n) {
        List<String> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(prefix + i);
        return l;
    }

    private PlayerField field(long id, List<String> hand, List<String> deck) {
        return PlayerField.builder()
                .playerId(id)
                .hand(new ArrayList<>(hand))
                .deck(new ArrayList<>(deck))
                .discardPile(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();
    }

    private Card trainer(String id, String name, String trainerEffectsJson) {
        return Card.builder().id(id).name(name)
                .parsedEffects("{\"trainerEffects\":" + trainerEffectsJson + "}")
                .build();
    }

    private BoardState board(PlayerField p1, PlayerField p2) {
        return BoardState.builder().currentPlayerId(1L).player1Field(p1).player2Field(p2).build();
    }

    @Test
    void professorSycamore_discardsHandAndDraws7() {
        PlayerField p1 = field(1L, cards("h", 3), cards("d", 10));
        BoardState board = board(p1, field(2L, List.of(), List.of()));

        chain.resolve(trainer("xy1-122", "Professor Sycamore",
                "[{\"type\":\"DISCARD_HAND_DRAW\",\"amount\":7}]"), board, 1L, null);

        assertEquals(7, p1.getHand().size());
        assertEquals(3, p1.getDeck().size());        // 10 - 7
        assertEquals(3, p1.getDiscardPile().size()); // mano descartada
    }

    @Test
    void shauna_shufflesHandIntoDeckAndDraws5() {
        PlayerField p1 = field(1L, cards("h", 3), cards("d", 10));
        BoardState board = board(p1, field(2L, List.of(), List.of()));

        chain.resolve(trainer("xy1-127", "Shauna",
                "[{\"type\":\"SHUFFLE_HAND\",\"target\":\"SELF\",\"drawAmount\":5}]"), board, 1L, null);

        assertEquals(5, p1.getHand().size());
        assertEquals(8, p1.getDeck().size()); // (10+3) - 5
    }

    @Test
    void redCard_opponentShufflesAndDraws4() {
        PlayerField p2 = field(2L, cards("oh", 6), cards("od", 10));
        BoardState board = board(field(1L, List.of(), List.of()), p2);

        chain.resolve(trainer("xy1-124", "Red Card",
                "[{\"type\":\"SHUFFLE_HAND\",\"target\":\"OPPONENT\",\"drawAmount\":4}]"), board, 1L, null);

        assertEquals(4, p2.getHand().size());
        assertEquals(12, p2.getDeck().size()); // (10+6) - 4
    }

    @Test
    void teamFlareGrunt_discardsOpponentActiveEnergy() {
        ActivePokemon oppActive = new ActivePokemon();
        oppActive.setCardId("opp-active");
        List<AttachedCard> energies = new ArrayList<>();
        energies.add(AttachedCard.builder().instanceId("en1").cardId("en1").build());
        energies.add(AttachedCard.builder().instanceId("en2").cardId("en2").build());
        oppActive.setAttachedEnergies(energies);

        PlayerField p2 = field(2L, List.of(), List.of());
        p2.setActivePokemon(oppActive);
        BoardState board = board(field(1L, List.of(), List.of()), p2);

        chain.resolve(trainer("xy1-129", "Team Flare Grunt",
                "[{\"type\":\"DISCARD_ENERGY\",\"amount\":1,\"target\":\"OPPONENT_ACTIVE\"}]"), board, 1L, null);

        assertEquals(1, oppActive.getAttachedEnergies().size());
        assertEquals(1, p2.getDiscardPile().size());
    }

    @Test
    void cardWithoutTrainerEffects_isNoOp() {
        PlayerField p1 = field(1L, cards("h", 3), cards("d", 5));
        BoardState board = board(p1, field(2L, List.of(), List.of()));

        Card plain = Card.builder().id("xy1-999").name("Plain").build(); // sin parsedEffects
        List<GameEvent> events = chain.resolve(plain, board, 1L, null);

        assertTrue(events.isEmpty());
        assertEquals(3, p1.getHand().size());
    }
}
