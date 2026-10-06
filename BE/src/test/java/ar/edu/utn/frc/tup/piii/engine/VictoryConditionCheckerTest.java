package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.VictoryReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VictoryConditionCheckerTest {

    private VictoryConditionChecker checker;
    private BoardState board;
    private PlayerField p1Field;
    private PlayerField p2Field;

    @BeforeEach
    void setUp() {
        checker = new VictoryConditionChecker();

        p1Field = new PlayerField();
        p1Field.setPlayerId(1L);
        p1Field.setPrizeCards(new ArrayList<>(List.of("prize1", "prize2", "prize3")));
        p1Field.setBench(new ArrayList<>());
        p1Field.setActivePokemon(activePokemon());

        p2Field = new PlayerField();
        p2Field.setPlayerId(2L);
        p2Field.setPrizeCards(new ArrayList<>(List.of("prize4", "prize5", "prize6")));
        p2Field.setBench(new ArrayList<>());
        p2Field.setActivePokemon(activePokemon());

        board = new BoardState();
        board.setPlayer1Field(p1Field);
        board.setPlayer2Field(p2Field);
        board.setMatchState(GameStatus.ACTIVE);
    }

    private ActivePokemon activePokemon() {
        ActivePokemon ap = new ActivePokemon();
        ap.setCardId("bulbasaur");
        ap.setCurrentHp(50);
        return ap;
    }

    private BenchPokemon benchPokemon() {
        BenchPokemon bp = new BenchPokemon();
        bp.setCardId("bench-poke");
        return bp;
    }

    // ── No detectable condition ───────────────────────────────────────────────

    @Test
    void checkVictory_normalGame_returnsEmpty() {
        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertTrue(result.isEmpty());
        assertEquals(GameStatus.ACTIVE, board.getMatchState());
        assertNull(board.getWinnerId());
    }

    @Test
    void checkVictory_finishedButNoDetectableCondition_returnsEmpty_keepsWinner() {
        // The game ended through another path (e.g. concede): no prize/no-Pokémon condition holds.
        board.setMatchState(GameStatus.FINISHED);
        board.setWinnerId(99L);

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertTrue(result.isEmpty());
        assertEquals(99L, board.getWinnerId()); // unchanged
    }

    // ── No Pokémon conditions ─────────────────────────────────────────────────

    @Test
    void checkVictory_player1NoActivePokemonAndNoBench_player2Wins() {
        p1Field.setActivePokemon(null);
        p1Field.getBench().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.NO_POKEMON_LEFT, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(2L, board.getWinnerId());
    }

    @Test
    void checkVictory_player2NoActivePokemonAndNoBench_player1Wins() {
        p2Field.setActivePokemon(null);
        p2Field.getBench().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.NO_POKEMON_LEFT, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(1L, board.getWinnerId());
    }

    @Test
    void checkVictory_player1NoActivePokemonButHasBench_noVictory() {
        p1Field.setActivePokemon(null);
        p1Field.getBench().add(benchPokemon());

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertTrue(result.isEmpty());
    }

    @Test
    void checkVictory_bothPlayersNoPokemon_suddenDeath() {
        // Both players lose their last Pokémon simultaneously → Sudden Death, no winner
        p1Field.setActivePokemon(null);
        p1Field.getBench().clear();
        p2Field.setActivePokemon(null);
        p2Field.getBench().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.SIMULTANEOUS_KO, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertNull(board.getWinnerId());
    }

    @Test
    void checkVictory_activePokemonAtZeroHp_isNotNoPokemon() {
        ActivePokemon zeroHp = activePokemon();
        zeroHp.setCurrentHp(0);
        zeroHp.setTool(AttachedCard.builder().cardId("tool-1").build());
        p1Field.setActivePokemon(zeroHp);

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertTrue(result.isEmpty());
        assertEquals(GameStatus.ACTIVE, board.getMatchState());
    }

    // ── Simultaneous KO (Sudden Death) ────────────────────────────────────────

    @Test
    void checkVictory_bothPrizesEmpty_simultaneousKo_noWinner() {
        // Both players simultaneously take their last prize card → Sudden Death
        p1Field.getPrizeCards().clear();
        p2Field.getPrizeCards().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.SIMULTANEOUS_KO, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertNull(board.getWinnerId());
    }

    // ── Prize conditions ──────────────────────────────────────────────────────

    @Test
    void checkVictory_player1NoPrizes_player1Wins() {
        p1Field.getPrizeCards().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.ALL_PRIZES_TAKEN, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(1L, board.getWinnerId());
    }

    @Test
    void checkVictory_player2NoPrizes_player2Wins() {
        p2Field.getPrizeCards().clear();

        Optional<VictoryReason> result = checker.checkVictoryConditions(board);

        assertEquals(VictoryReason.ALL_PRIZES_TAKEN, result.orElse(null));
        assertEquals(GameStatus.FINISHED, board.getMatchState());
        assertEquals(2L, board.getWinnerId());
    }
}
