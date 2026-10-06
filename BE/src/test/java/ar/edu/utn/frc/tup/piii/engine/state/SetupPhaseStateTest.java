package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SetupPhaseStateTest {

    private SetupPhaseState state;
    private BoardState board;
    private PlayerField p1Field;
    private CardLookup lookup;
    private Card basicPokemon;

    @BeforeEach
    void setUp() {
        state = new SetupPhaseState();

        basicPokemon = Card.builder()
                .id("bulbasaur")
                .name("Bulbasaur")
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .build();

        lookup = id -> basicPokemon;

        p1Field = PlayerField.builder()
                .playerId(1L)
                .hand(new ArrayList<>(List.of("bulbasaur")))
                .bench(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .turnFlags(new TurnFlags())
                .build();

        PlayerField p2Field = PlayerField.builder()
                .playerId(2L)
                .hand(new ArrayList<>())
                .bench(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .turnFlags(new TurnFlags())
                .build();

        board = BoardState.builder()
                .matchState(GameStatus.SETUP)
                .currentPhase(TurnPhase.SETUP)
                .currentPlayerId(1L)
                .player1Field(p1Field)
                .player2Field(p2Field)
                .setupCompletedPlayers(new ArrayList<>())
                .build();
    }

    // ── Bug #4: targetPosition must be respected ──────────────────────────

    @Test
    void placePokemon_withTargetActive_noActivePokemon_placesAsActive() {
        ActionRequest action = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur")
                .targetPosition("ACTIVE")
                .build();

        ActionResult result = state.handle(action, board, 1L, lookup);

        assertTrue(result.isSuccess());
        assertNotNull(p1Field.getActivePokemon(), "Pokemon must be placed as Active.");
        assertTrue(p1Field.getBench().isEmpty());
    }

    @Test
    void placePokemon_withTargetBench_noActivePokemon_returnsError() {
        ActionRequest action = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur")
                .targetPosition("BENCH")
                .build();

        ActionResult result = state.handle(action, board, 1L, lookup);

        assertFalse(result.isSuccess(), "Must not allow BENCH without an Active Pokémon first.");
        assertNull(p1Field.getActivePokemon());
        assertTrue(p1Field.getHand().contains("bulbasaur"), "Card must stay in hand after failure.");
    }

    @Test
    void placePokemon_withTargetActive_activeAlreadyOccupied_returnsError() {
        // Place first pokemon as active
        ActionRequest first = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur")
                .targetPosition("ACTIVE")
                .build();
        state.handle(first, board, 1L, lookup);

        // Add a second card to hand and try to place as Active again
        p1Field.getHand().add("bulbasaur2");
        ActionRequest second = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur2")
                .targetPosition("ACTIVE")
                .build();

        ActionResult result = state.handle(second, board, 1L, lookup);

        assertFalse(result.isSuccess(), "Must not allow a second Active Pokémon.");
        assertTrue(p1Field.getBench().isEmpty(), "Second pokemon must not go to bench either.");
    }

    @Test
    void placePokemon_withTargetBench_activeOccupied_placesOnBench() {
        // Place first as active
        ActionRequest first = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur")
                .targetPosition("ACTIVE")
                .build();
        state.handle(first, board, 1L, lookup);

        // Place second explicitly to bench
        p1Field.getHand().add("bulbasaur2");
        ActionRequest bench = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur2")
                .targetPosition("BENCH")
                .build();

        ActionResult result = state.handle(bench, board, 1L, lookup);

        assertTrue(result.isSuccess());
        assertEquals(1, p1Field.getBench().size(), "Pokemon must land on the bench.");
    }

    @Test
    void placePokemon_nullTargetPosition_behavesLikeActive_whenNoActivePokemon() {
        ActionRequest action = ActionRequest.builder()
                .type(ActionType.SETUP_PLACE_POKEMON)
                .cardId("bulbasaur")
                .targetPosition(null)
                .build();

        ActionResult result = state.handle(action, board, 1L, lookup);

        assertTrue(result.isSuccess());
        assertNotNull(p1Field.getActivePokemon(), "Null targetPosition defaults to ACTIVE when slot is empty.");
    }
}
