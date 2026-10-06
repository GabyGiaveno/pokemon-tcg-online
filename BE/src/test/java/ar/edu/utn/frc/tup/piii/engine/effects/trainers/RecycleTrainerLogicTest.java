package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.RecycleTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecycleTrainerLogicTest {

    private RecycleTrainerLogic logic;
    private BoardState board;
    private PlayerField field;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        logic = new RecycleTrainerLogic();
        board = new BoardState();
        field = new PlayerField();
        field.setPlayerId(1L);
        field.setHand(new ArrayList<>(List.of("hand-1", "hand-2", "hand-3")));
        field.setDeck(new ArrayList<>());
        field.setBench(new ArrayList<>());

        // Discard pile: two Pokémon and one Energy
        field.setDiscardPile(new ArrayList<>(List.of("disc-pokemon-1", "disc-energy-1", "disc-pokemon-2")));

        Map<String, String> instanceCardIds = new HashMap<>();
        instanceCardIds.put("hand-1", "xy1-trainer-1");
        instanceCardIds.put("hand-2", "xy1-trainer-2");
        instanceCardIds.put("hand-3", "xy1-trainer-3");
        instanceCardIds.put("disc-pokemon-1", "xy1-1");
        instanceCardIds.put("disc-energy-1", "xy1-energy");
        instanceCardIds.put("disc-pokemon-2", "xy1-2");
        field.setInstanceCardIds(instanceCardIds);

        board.setPlayer1Field(field);
        board.setPlayer2Field(new PlayerField());
        board.getPlayer2Field().setPlayerId(2L);

        cardLookup = cardId -> switch (cardId) {
            case "xy1-1" -> Card.builder().id("xy1-1").supertype("Pokémon")
                    .subtypes(List.of("Basic")).hp(60).build();
            case "xy1-2" -> Card.builder().id("xy1-2").supertype("Pokémon")
                    .subtypes(List.of("Stage 1")).hp(90).build();
            case "xy1-energy" -> Card.builder().id("xy1-energy").supertype("Energy")
                    .subtypes(List.of("Basic")).hp(null).build();
            default -> Card.builder().id(cardId).supertype("Trainer")
                    .subtypes(List.of("Item")).build();
        };
    }

    private TrainerContext ctx() {
        return TrainerContext.builder().board(board).playerId(1L).cardLookup(cardLookup).build();
    }

    @Test
    void discardsHandCards_andCreatesCHOOSE_FROM_DISCARD_pendingSelection() {
        RecycleTrainerEffect effect = new RecycleTrainerEffect();
        effect.setFilter("POKEMON");
        effect.setDiscardCost(2);
        effect.setSourceZone("DISCARD");
        effect.setDestination("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        // 2 CARD_DISCARDED events
        long discardEvents = events.stream()
                .filter(e -> e.getType() == GameEventType.CARD_DISCARDED)
                .count();
        assertEquals(2, discardEvents);

        // Hand shrunk by 2
        assertEquals(1, field.getHand().size());
        // Discard pile grew by 2
        assertEquals(5, field.getDiscardPile().size());

        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        assertEquals(SelectionType.CHOOSE_FROM_DISCARD, pending.getType());
        assertTrue(pending.getValidOptions().contains("disc-pokemon-1"));
        assertTrue(pending.getValidOptions().contains("disc-pokemon-2"));
        assertFalse(pending.getValidOptions().contains("disc-energy-1"));
    }

    @Test
    void insufficientHand_returnsEmptyAndNoSideEffects() {
        field.setHand(new ArrayList<>(List.of("hand-1"))); // only 1 card
        RecycleTrainerEffect effect = new RecycleTrainerEffect();
        effect.setFilter("POKEMON");
        effect.setDiscardCost(2);
        effect.setSourceZone("DISCARD");
        effect.setDestination("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        assertTrue(events.isEmpty());
        assertEquals(1, field.getHand().size()); // hand unchanged
        assertNull(board.getPendingSelection());
    }

    @Test
    void noPokemonInDiscard_returnsOnlyDiscardEventsAndNoPendingSelection() {
        field.setDiscardPile(new ArrayList<>(List.of("disc-energy-1")));
        RecycleTrainerEffect effect = new RecycleTrainerEffect();
        effect.setFilter("POKEMON");
        effect.setDiscardCost(2);
        effect.setSourceZone("DISCARD");
        effect.setDestination("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        // Cost was paid: 2 discard events
        assertEquals(2, events.size());
        assertNull(board.getPendingSelection());
    }
}
