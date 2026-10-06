package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class KnockoutProcessorTest {

    private KnockoutProcessor knockoutProcessor;
    private PlayerField ownerField;
    private PlayerField opponentField;
    private BoardState board;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        knockoutProcessor = new KnockoutProcessor();

        ownerField = new PlayerField();
        ownerField.setPlayerId(1L);
        ownerField.setDiscardPile(new ArrayList<>());
        ownerField.setPrizeCards(new ArrayList<>(List.of("prize1", "prize2")));
        ownerField.setBench(new ArrayList<>());

        opponentField = new PlayerField();
        opponentField.setPlayerId(2L);
        opponentField.setDiscardPile(new ArrayList<>());
        opponentField.setPrizeCards(new ArrayList<>(List.of("prize3", "prize4", "prize5")));
        opponentField.setHand(new ArrayList<>());

        board = new BoardState();
        board.setPlayer1Field(ownerField);
        board.setPlayer2Field(opponentField);
        board.setMatchState(GameStatus.ACTIVE);

        cardLookup = Mockito.mock(CardLookup.class);
    }

    @Test
    void processIfKnockedOut_pokemonAlive_doesNothing() {
        ActivePokemon active = new ActivePokemon();
        active.setCurrentHp(10);
        ownerField.setActivePokemon(active);

        List<GameEvent> events = knockoutProcessor.processIfKnockedOut(active, ownerField, opponentField, board, cardLookup);

        assertTrue(events.isEmpty());
        assertNotNull(ownerField.getActivePokemon());
    }

    @Test
    void processIfKnockedOut_normalPokemon_awardsOnePrize() {
        ActivePokemon active = new ActivePokemon();
        active.setCardId("normal-poke");
        active.setCurrentHp(0);
        active.setAttachedEnergies(new ArrayList<>());
        ownerField.setActivePokemon(active);
        
        Card mockCard = new Card();
        mockCard.setSubtypes(List.of("Basic"));
        when(cardLookup.findById("normal-poke")).thenReturn(mockCard);

        List<GameEvent> events = knockoutProcessor.processIfKnockedOut(active, ownerField, opponentField, board, cardLookup);

        assertFalse(events.isEmpty());
        assertNull(ownerField.getActivePokemon());
        assertTrue(ownerField.getDiscardPile().contains("normal-poke"));
        assertEquals(2, opponentField.getPrizeCards().size()); // 3 initial - 1 taken
        assertEquals(1, opponentField.getHand().size()); // Added to hand
        assertEquals(GameStatus.ACTIVE, board.getMatchState()); // KO no longer decides the game
        assertNull(board.getPendingSelection()); // empty Bench -> no selection requested
    }

    @Test
    void processIfKnockedOut_exPokemon_awardsTwoPrizes() {
        ActivePokemon active = new ActivePokemon();
        active.setCardId("ex-poke");
        active.setCurrentHp(0);
        active.setAttachedEnergies(new ArrayList<>());
        ownerField.setActivePokemon(active);
        
        // Add bench so game doesn't end due to no bench
        BenchPokemon bench = new BenchPokemon();
        bench.setCardId("bench-poke");
        ownerField.getBench().add(bench);
        
        Card mockCard = new Card();
        mockCard.setSubtypes(List.of("Basic", "EX"));
        when(cardLookup.findById("ex-poke")).thenReturn(mockCard);

        List<GameEvent> events = knockoutProcessor.processIfKnockedOut(active, ownerField, opponentField, board, cardLookup);

        assertFalse(events.isEmpty());
        assertNull(ownerField.getActivePokemon()); // no longer auto-promotes; waits for selection
        PendingSelection sel = board.getPendingSelection();
        assertNotNull(sel);
        assertEquals(SelectionType.CHOOSE_ACTIVE_ON_KO, sel.getType());
        assertEquals(1L, sel.getOwnerPlayerId());
        assertTrue(sel.getValidOptions().contains("bench-poke"));
        assertEquals(1, opponentField.getPrizeCards().size()); // 3 initial - 2 taken
        assertEquals(2, opponentField.getHand().size()); // 2 Added to hand
        assertEquals(GameStatus.ACTIVE, board.getMatchState());
    }

    @Test
    void processIfKnockedOut_megaPokemon_awardsTwoPrizes_noVictoryDecidedHere() {
        ActivePokemon active = new ActivePokemon();
        active.setCardId("mega-poke");
        active.setCurrentHp(0);
        active.setAttachedEnergies(new ArrayList<>());
        ownerField.setActivePokemon(active);
        
        // Modify opponent prizes to 2, so taking 2 means they win
        opponentField.getPrizeCards().remove(0);
        
        BenchPokemon bench = new BenchPokemon();
        bench.setCardId("bench-poke");
        ownerField.getBench().add(bench);
        
        Card mockCard = new Card();
        mockCard.setSubtypes(List.of("MEGA"));
        when(cardLookup.findById("mega-poke")).thenReturn(mockCard);

        List<GameEvent> events = knockoutProcessor.processIfKnockedOut(active, ownerField, opponentField, board, cardLookup);

        assertEquals(0, opponentField.getPrizeCards().size()); // Took remaining 2
        assertEquals(GameStatus.ACTIVE, board.getMatchState()); // KO does not declare victory
        assertNotNull(board.getPendingSelection()); // has Bench -> requests promotion selection
    }
}
