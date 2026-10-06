package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration of the player-selection mechanism through the real {@link TurnManager}
 * and its real phase states (no mocks). Covers the two KO sites that produce a CHOOSE_ACTIVE_ON_KO
 * selection: a poison KO during BETWEEN_TURNS, and a KO caused by an attack.
 */
class TurnManagerSelectionIntegrationTest {

    private final TurnManager turnManager = new TurnManager(); // real phases + checker + resolver

    // ── KO by poison (between-turns) ──────────────────────────────────────────

    @Test
    void poisonKo_pausesForSelection_thenResumesNextTurn() {
        ActivePokemon poisoned = ActivePokemon.builder()
                .cardId("active-1").currentHp(10).isPoisoned(true)
                .attachedEnergies(new ArrayList<>()).condition(SpecialCondition.NONE).build();

        PlayerField p1 = PlayerField.builder()
                .playerId(1L).activePokemon(poisoned)
                .bench(new ArrayList<>(List.of(bench("bench-1"))))
                .deck(new ArrayList<>(List.of("p1-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pa", "pb")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        PlayerField p2 = PlayerField.builder()
                .playerId(2L).activePokemon(activeMon("active-2", 60))
                .bench(new ArrayList<>())
                .deck(new ArrayList<>(List.of("p2-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pc", "pd")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        BoardState board = board(p1, p2);
        CardLookup lookup = id -> null; // non-EX KO → 1 prize; draw needs no lookup

        // 1) End turn → poison ticks, KO happens, the engine pauses for p1 to choose its new Active
        ActionResult paused = turnManager.processAction(endTurn(), board, 1L, lookup);

        assertTrue(paused.isSuccess());
        assertNotNull(board.getPendingSelection());
        assertEquals(1L, board.getPendingSelection().getOwnerPlayerId());
        assertEquals(1L, board.getCurrentPlayerId());       // NOT switched: paused mid between-turns
        assertNull(p1.getActivePokemon());

        // 2) p1 resolves → bench-1 promoted, turn finishes, switches to p2's MAIN
        ActionResult resumed = turnManager.processAction(resolve(0), board, 1L, lookup);

        assertTrue(resumed.isSuccess());
        assertNull(board.getPendingSelection());
        assertNotNull(p1.getActivePokemon());
        assertEquals("bench-1", p1.getActivePokemon().getCardId());
        assertEquals(2L, board.getCurrentPlayerId());        // now switched
        assertEquals(TurnPhase.MAIN, board.getCurrentPhase());
    }

    // ── KO by attack ──────────────────────────────────────────────────────────

    @Test
    void attackKo_pausesForDefenderSelection() {
        Card attackerCard = Card.builder().id("mewtwo1").types(List.of("Psychic"))
                .attacks("[{\"name\":\"Psystrike\", \"cost\":[], \"convertedEnergyCost\":0, \"damage\":\"100\", \"text\":\"\"}]")
                .build();
        Card defenderCard = Card.builder().id("rattata1").types(List.of("Colorless")).build();
        CardLookup lookup = id -> switch (id) {
            case "mewtwo1" -> attackerCard;
            case "rattata1" -> defenderCard;
            default -> null;
        };

        PlayerField p1 = PlayerField.builder()
                .playerId(1L).activePokemon(activeMon("mewtwo1", 120))
                .bench(new ArrayList<>())
                .deck(new ArrayList<>(List.of("p1-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pa", "pb")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        PlayerField p2 = PlayerField.builder()
                .playerId(2L).activePokemon(activeMon("rattata1", 40))
                .bench(new ArrayList<>(List.of(bench("bench-def"))))
                .deck(new ArrayList<>(List.of("p2-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pc")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        BoardState board = board(p1, p2);

        // 1) p1 attacks → rattata KO'd, the DEFENDER (p2) must choose its new Active
        ActionResult paused = turnManager.processAction(attack(), board, 1L, lookup);

        assertTrue(paused.isSuccess());
        assertNotNull(board.getPendingSelection());
        assertEquals(2L, board.getPendingSelection().getOwnerPlayerId()); // the KO'd side chooses
        assertEquals(1L, board.getCurrentPlayerId());                     // attacker still on turn
        assertNull(p2.getActivePokemon());

    }

    @Test
    void endTurn_poisonKnocksOutLastPokemon_gameFinishesWithoutResume() {
        ActivePokemon poisoned = ActivePokemon.builder()
                .cardId("active-1").currentHp(10).isPoisoned(true)
                .attachedEnergies(new ArrayList<>()).condition(SpecialCondition.NONE).build();

        PlayerField p1 = PlayerField.builder()
                .playerId(1L).activePokemon(poisoned)
                .bench(new ArrayList<>())
                .deck(new ArrayList<>(List.of("p1-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pa", "pb")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        PlayerField p2 = PlayerField.builder()
                .playerId(2L).activePokemon(activeMon("active-2", 60))
                .bench(new ArrayList<>())
                .deck(new ArrayList<>(List.of("p2-deck")))
                .hand(new ArrayList<>()).prizeCards(new ArrayList<>(List.of("pc", "pd")))
                .discardPile(new ArrayList<>()).turnFlags(new TurnFlags()).build();

        BoardState board = board(p1, p2);

        ActionResult result = turnManager.processAction(endTurn(), board, 1L, id -> null);

        assertTrue(result.isSuccess());
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(2L, board.getWinnerId());
        assertNull(board.getPendingSelection());
        assertTrue(result.getEvents().stream().anyMatch(e -> e.getType() == GameEventType.GAME_FINISHED));
        assertEquals("NO_POKEMON_LEFT", result.getEvents().stream()
                .filter(e -> e.getType() == GameEventType.GAME_FINISHED)
                .findFirst().orElseThrow().getData().get("reason"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ActivePokemon activeMon(String cardId, int hp) {
        return ActivePokemon.builder()
                .cardId(cardId).currentHp(hp).attachedEnergies(new ArrayList<>())
                .condition(SpecialCondition.NONE).build();
    }

    private BenchPokemon bench(String cardId) {
        BenchPokemon bp = new BenchPokemon();
        bp.setCardId(cardId);
        return bp;
    }

    private BoardState board(PlayerField p1, PlayerField p2) {
        return BoardState.builder()
                .player1Field(p1).player2Field(p2)
                .currentPlayerId(1L).currentPhase(TurnPhase.MAIN)
                .matchState(GameStatus.ACTIVE).firstPlayerHasActed(true).build();
    }

    private ActionRequest endTurn() {
        return ActionRequest.builder().type(ActionType.END_TURN).build();
    }

    private ActionRequest attack() {
        return ActionRequest.builder().type(ActionType.USE_ATTACK).attackIndex(0).build();
    }

    private ActionRequest resolve(int benchIndex) {
        return ActionRequest.builder().type(ActionType.RESOLVE_SELECTION).benchIndex(benchIndex).build();
    }
}
