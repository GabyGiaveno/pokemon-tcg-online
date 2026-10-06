package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.SearchDeckTrainerEffect;
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

class SearchDeckTrainerLogicTest {

    private SearchDeckTrainerLogic logic;
    private BoardState board;
    private PlayerField field;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        logic = new SearchDeckTrainerLogic();
        board = new BoardState();
        field = new PlayerField();
        field.setPlayerId(1L);
        field.setHand(new ArrayList<>());
        field.setDeck(new ArrayList<>(List.of("inst-1", "inst-2", "inst-3", "inst-4", "inst-5")));
        field.setDiscardPile(new ArrayList<>());

        Map<String, String> instanceCardIds = new HashMap<>();
        instanceCardIds.put("inst-1", "xy1-1");
        instanceCardIds.put("inst-2", "xy1-2");
        instanceCardIds.put("inst-3", "xy1-3");
        instanceCardIds.put("inst-4", "xy1-4");
        instanceCardIds.put("inst-5", "xy1-5");
        field.setInstanceCardIds(instanceCardIds);

        board.setPlayer1Field(field);
        board.setPlayer2Field(new PlayerField());
        board.getPlayer2Field().setPlayerId(2L);

        cardLookup = cardId -> switch (cardId) {
            case "xy1-1" -> Card.builder().id("xy1-1").supertype("Pokémon")
                    .subtypes(List.of("Basic")).hp(60).build();
            case "xy1-2" -> Card.builder().id("xy1-2").supertype("Pokémon")
                    .subtypes(List.of("Stage 1")).hp(90).build();
            case "xy1-3" -> Card.builder().id("xy1-3").supertype("Energy")
                    .subtypes(List.of("Basic")).hp(null).build();
            case "xy1-4" -> Card.builder().id("xy1-4").supertype("Pokémon")
                    .subtypes(List.of("Stage 2")).hp(150).build();
            case "xy1-5" -> Card.builder().id("xy1-5").supertype("Trainer")
                    .subtypes(List.of("Item")).hp(null).build();
            default -> throw new IllegalArgumentException("Unknown card: " + cardId);
        };
    }

    private TrainerContext ctx() {
        return TrainerContext.builder()
                .board(board)
                .playerId(1L)
                .cardLookup(cardLookup)
                .build();
    }

    @Test
    void handDestination_createsSEARCH_DECK_pendingSelection() {
        SearchDeckTrainerEffect effect = new SearchDeckTrainerEffect();
        effect.setFilter("POKEMON");
        effect.setLookAtCount(5);
        effect.setTakeCount(1);
        effect.setDestination("HAND");

        List<GameEvent> events = logic.execute(effect, ctx());

        assertFalse(events.isEmpty());
        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        assertEquals(SelectionType.SEARCH_DECK, pending.getType());
        assertTrue(pending.getValidOptions().contains("inst-1"));
        assertTrue(pending.getValidOptions().contains("inst-2"));
        assertFalse(pending.getValidOptions().contains("inst-3")); // Energy, not Pokémon
    }

    @Test
    void benchDestination_createsPLACE_ON_BENCH_pendingSelection() {
        SearchDeckTrainerEffect effect = new SearchDeckTrainerEffect();
        effect.setFilter("POKEMON_EVOLUTION");
        effect.setLookAtCount(8);
        effect.setTakeCount(1);
        effect.setDestination("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        assertFalse(events.isEmpty());
        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        assertEquals(SelectionType.PLACE_ON_BENCH, pending.getType());
        // inst-2 is Stage 1, inst-4 is Stage 2
        assertTrue(pending.getValidOptions().contains("inst-2"));
        assertTrue(pending.getValidOptions().contains("inst-4"));
        assertFalse(pending.getValidOptions().contains("inst-1")); // Basic Pokémon
    }

    @Test
    void lookAtCount_limitsRevealedCards() {
        SearchDeckTrainerEffect effect = new SearchDeckTrainerEffect();
        effect.setFilter("POKEMON");
        effect.setLookAtCount(2); // only look at top 2: inst-1 (Basic Pokémon), inst-2 (Stage 1)
        effect.setTakeCount(1);
        effect.setDestination("HAND");

        logic.execute(effect, ctx());

        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        // Only cards from the top 2 slots
        assertFalse(pending.getValidOptions().contains("inst-4")); // inst-4 is at position 4
    }

    @Test
    void noMatch_returnsEmptyAndNoPendingSelection() {
        SearchDeckTrainerEffect effect = new SearchDeckTrainerEffect();
        effect.setFilter("ENERGY_BASIC");
        effect.setLookAtCount(3); // top 3: inst-1 (Pokémon), inst-2 (Pokémon), inst-3 (Energy Basic)
        effect.setTakeCount(1);
        effect.setDestination("HAND");

        // Deck has exactly inst-3 as Energy Basic, but limit it so it's NOT in top 2
        field.setDeck(new ArrayList<>(List.of("inst-1", "inst-2")));

        List<GameEvent> events = logic.execute(effect, ctx());

        assertTrue(events.isEmpty());
        assertNull(board.getPendingSelection());
    }

    @Test
    void filterPOKEMON_matchesOnlyPokemon() {
        Card pokemon = Card.builder().supertype("Pokémon").subtypes(List.of("Basic")).build();
        Card energy = Card.builder().supertype("Energy").subtypes(List.of("Basic")).build();
        Card trainer = Card.builder().supertype("Trainer").subtypes(List.of("Item")).build();

        assertTrue(SearchDeckTrainerLogic.matches(pokemon, "POKEMON"));
        assertFalse(SearchDeckTrainerLogic.matches(energy, "POKEMON"));
        assertFalse(SearchDeckTrainerLogic.matches(trainer, "POKEMON"));
    }

    @Test
    void filterPOKEMON_EVOLUTION_matchesStage1AndStage2Only() {
        Card basic = Card.builder().supertype("Pokémon").subtypes(List.of("Basic")).build();
        Card stage1 = Card.builder().supertype("Pokémon").subtypes(List.of("Stage 1")).build();
        Card stage2 = Card.builder().supertype("Pokémon").subtypes(List.of("Stage 2")).build();

        assertFalse(SearchDeckTrainerLogic.matches(basic, "POKEMON_EVOLUTION"));
        assertTrue(SearchDeckTrainerLogic.matches(stage1, "POKEMON_EVOLUTION"));
        assertTrue(SearchDeckTrainerLogic.matches(stage2, "POKEMON_EVOLUTION"));
    }

    @Test
    void filterENERGY_BASIC_matchesOnlyBasicEnergy() {
        Card basicEnergy = Card.builder().supertype("Energy").subtypes(List.of("Basic")).build();
        Card specialEnergy = Card.builder().supertype("Energy").subtypes(List.of("Special")).build();
        Card pokemon = Card.builder().supertype("Pokémon").subtypes(List.of("Basic")).build();

        assertTrue(SearchDeckTrainerLogic.matches(basicEnergy, "ENERGY_BASIC"));
        assertFalse(SearchDeckTrainerLogic.matches(specialEnergy, "ENERGY_BASIC"));
        assertFalse(SearchDeckTrainerLogic.matches(pokemon, "ENERGY_BASIC"));
    }

    @Test
    void lookAtCountMinusOne_searchesEntireDeck() {
        SearchDeckTrainerEffect effect = new SearchDeckTrainerEffect();
        effect.setFilter("ENERGY_BASIC");
        effect.setLookAtCount(-1);
        effect.setTakeCount(2);
        effect.setDestination("HAND");

        logic.execute(effect, ctx());

        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        // inst-3 is the only Energy Basic in the 5-card deck
        assertTrue(pending.getValidOptions().contains("inst-3"));
    }
}
