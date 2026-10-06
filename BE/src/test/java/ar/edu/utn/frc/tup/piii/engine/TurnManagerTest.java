package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.state.AttackPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.BetweenTurnsState;
import ar.edu.utn.frc.tup.piii.engine.state.DrawPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.MainPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.SetupPhaseState;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import ar.edu.utn.frc.tup.piii.models.game.VictoryReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TurnManagerTest {

    private DrawPhaseState mockDraw;
    private MainPhaseState mockMain;
    private AttackPhaseState mockAttack;
    private BetweenTurnsState mockBetween;
    private SetupPhaseState mockSetup;
    private VictoryConditionChecker mockVictory;
    private SelectionResolver mockSelection;
    private TurnManager turnManager;
    private CardLookup cardLookup;

    private BoardState board;
    private PlayerField p1Field;
    private PlayerField p2Field;

    @BeforeEach
    void setUp() {
        mockDraw    = mock(DrawPhaseState.class);
        mockMain    = mock(MainPhaseState.class);
        mockAttack  = mock(AttackPhaseState.class);
        mockBetween = mock(BetweenTurnsState.class);
        mockSetup   = mock(SetupPhaseState.class);
        mockVictory = mock(VictoryConditionChecker.class);
        mockSelection = mock(SelectionResolver.class);
        cardLookup  = mock(CardLookup.class);

        turnManager = new TurnManager(mockDraw, mockMain, mockAttack, mockBetween,
                mockSetup, mockVictory, mockSelection);

        // By default no victory condition holds, so completeTurn proceeds normally.
        when(mockVictory.checkVictoryConditions(any())).thenReturn(Optional.empty());

        p1Field = new PlayerField();
        p1Field.setPlayerId(1L);
        p1Field.setBench(new ArrayList<>());
        p1Field.setTurnFlags(TurnFlags.builder().build());
        p1Field.setDeck(new ArrayList<>(List.of("card1")));
        p1Field.setHand(new ArrayList<>());

        p2Field = new PlayerField();
        p2Field.setPlayerId(2L);
        p2Field.setBench(new ArrayList<>());
        p2Field.setTurnFlags(TurnFlags.builder().build());
        p2Field.setDeck(new ArrayList<>(List.of("card2")));
        p2Field.setHand(new ArrayList<>());

        board = new BoardState();
        board.setPlayer1Field(p1Field);
        board.setPlayer2Field(p2Field);
        board.setCurrentPlayerId(1L);
        board.setMatchState(GameStatus.ACTIVE);
        board.setCurrentPhase(TurnPhase.MAIN);
        board.setFirstPlayerHasActed(true);

        // Default stubs for automatic phases
        when(mockDraw.execute(any(), any())).thenReturn(List.of());
        when(mockBetween.execute(any(), any())).thenReturn(List.of());
    }

    // ── beginTurn ─────────────────────────────────────────────────────────────

    @Test
    void beginTurn_normalTurn_resetsAndTransitionsToMain() {
        when(mockDraw.execute(any(), any())).thenReturn(
                List.of(GameEvent.of(GameEventType.CARD_DRAWN, "drew", Map.of()))
        );

        List<GameEvent> events = turnManager.beginTurn(board, cardLookup);

        assertEquals(TurnPhase.MAIN, board.getCurrentPhase());
        // CARD_DRAWN + PHASE_CHANGED
        assertEquals(2, events.size());
        assertEquals(GameEventType.CARD_DRAWN, events.get(0).getType());
        assertEquals(GameEventType.PHASE_CHANGED, events.get(1).getType());
    }

    @Test
    void beginTurn_deckOut_stopsBeforeMain() {
        when(mockDraw.execute(any(), any())).thenAnswer(inv -> {
            board.setMatchState(GameStatus.FINISHED); // simulate deck-out
            board.setWinnerId(2L);
            return List.of(GameEvent.of(GameEventType.DECK_OUT, "deck out", Map.of()));
        });

        List<GameEvent> events = turnManager.beginTurn(board, cardLookup);

        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(1, events.size()); // only DECK_OUT, no PHASE_CHANGED
        assertEquals(GameEventType.DECK_OUT, events.get(0).getType());
    }

    // ── processAction — guard conditions ──────────────────────────────────────

    @Test
    void processAction_notPlayersTurn_returnsFailure() {
        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();

        ActionResult result = turnManager.processAction(action, board, 2L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not your turn"));
    }

    @Test
    void processAction_gameAlreadyFinished_returnsFailure() {
        board.setMatchState(GameStatus.FINISHED);
        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("already over"));
    }

    @Test
    void processAction_notMainPhase_returnsFailure() {
        board.setCurrentPhase(TurnPhase.DRAW);
        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("MAIN"));
    }

    // ── processAction — CONCEDE ───────────────────────────────────────────────

    @Test
    void processAction_concede_setsFinishedAndOpponentWins() {
        ActionRequest action = ActionRequest.builder().type(ActionType.CONCEDE).build();

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(2L, board.getWinnerId()); // P2 wins
        assertFalse(result.getEvents().isEmpty());
        assertEquals(GameEventType.PLAYER_CONCEDED, result.getEvents().get(0).getType());
    }

    // ── processAction — END_TURN ──────────────────────────────────────────────

    @Test
    void processAction_endTurn_runsBetweenTurnsThenSwitchesPlayer() {
        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        verify(mockBetween).execute(any(), any());
        verify(mockDraw).execute(any(), any()); // beginTurn for P2
        assertEquals(TurnPhase.MAIN, board.getCurrentPhase());
        assertEquals(2L, board.getCurrentPlayerId()); // switched to P2
    }

    @Test
    void processAction_endTurn_victoryInBetweenTurns_doesNotBeginNextTurn() {
        // A poison/burn KO in between-turns leaves a player with no Pokémon: the checker declares victory.
        when(mockVictory.checkVictoryConditions(any())).thenAnswer(inv -> {
            BoardState b = inv.getArgument(0);
            b.setMatchState(GameStatus.FINISHED);
            b.setWinnerId(1L);
            return Optional.of(VictoryReason.NO_POKEMON_LEFT);
        });

        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();
        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        verify(mockDraw, never()).execute(any(), any()); // no new turn started

        // GAME_FINISHED event narrates the precise reason
        GameEvent finished = result.getEvents().stream()
                .filter(e -> e.getType() == GameEventType.GAME_FINISHED)
                .findFirst().orElseThrow();
        assertEquals("NO_POKEMON_LEFT", finished.getData().get("reason"));
        assertEquals(1L, finished.getData().get("winnerId"));
    }

    // ── processAction — regular MAIN action delegation ────────────────────────

    @Test
    void processAction_mainAction_delegatesToMainPhase() {
        ActionRequest action = ActionRequest.builder()
                .type(ActionType.PLAY_BASIC_POKEMON)
                .cardId("bulbasaur")
                .build();
        when(mockMain.handle(any(), any(), any(), any()))
                .thenReturn(ActionResult.success(List.of()));

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        verify(mockMain).handle(action, board, 1L, cardLookup);
    }

    // ── processAction — pending selection mechanism ───────────────────────────

    @Test
    void processAction_pendingSelection_blocksOtherActions() {
        board.setPendingSelection(pendingChooseActive());
        ActionRequest action = ActionRequest.builder().type(ActionType.END_TURN).build();

        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("pending selection"));
        verify(mockBetween, never()).execute(any(), any()); // turn was not advanced
    }

    @Test
    void processAction_resolveSelection_routesToResolverAndResumes() {
        board.setCurrentPhase(TurnPhase.ATTACK);
        board.setPendingSelection(pendingChooseActive());
        GameEvent promo = GameEvent.of(GameEventType.PHASE_CHANGED, "promoted", Map.of());
        when(mockSelection.resolve(any(), any(), any(), any())).thenAnswer(inv -> {
            board.setPendingSelection(null); // resolver consumes the selection
            return ActionResult.success(List.of(promo));
        });

        ActionRequest action = ActionRequest.builder()
                .type(ActionType.RESOLVE_SELECTION).benchIndex(0).build();
        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        verify(mockSelection).resolve(board, 1L, action, cardLookup);
        verify(mockBetween).execute(any(), any()); // resume = completeTurn ran between-turns
        verify(mockDraw).execute(any(), any());    // and began the next turn
        assertEquals(2L, board.getCurrentPlayerId()); // switched player
    }

    @Test
    void processAction_resolveSelection_validationFails_doesNotResume() {
        board.setPendingSelection(pendingChooseActive());
        when(mockSelection.resolve(any(), any(), any(), any()))
                .thenReturn(ActionResult.failure("Invalid selection index."));

        ActionRequest action = ActionRequest.builder()
                .type(ActionType.RESOLVE_SELECTION).benchIndex(9).build();
        ActionResult result = turnManager.processAction(action, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid selection index"));
        verify(mockBetween, never()).execute(any(), any()); // no resume
    }

    private PendingSelection pendingChooseActive() {
        return PendingSelection.builder()
                .type(SelectionType.CHOOSE_ACTIVE_ON_KO)
                .ownerPlayerId(1L)
                .validOptions(List.of("bench-0"))
                .build();
    }
}
