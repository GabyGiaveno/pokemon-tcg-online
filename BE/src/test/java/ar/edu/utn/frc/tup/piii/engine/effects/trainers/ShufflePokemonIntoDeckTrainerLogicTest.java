package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.ShufflePokemonIntoDeckTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShufflePokemonIntoDeckTrainerLogicTest {

    private ShufflePokemonIntoDeckTrainerLogic logic;
    private BoardState board;
    private PlayerField field;

    @BeforeEach
    void setUp() {
        logic = new ShufflePokemonIntoDeckTrainerLogic();
        board = new BoardState();
        field = new PlayerField();
        field.setPlayerId(1L);
        field.setHand(new ArrayList<>());
        field.setDeck(new ArrayList<>());
        field.setDiscardPile(new ArrayList<>());

        BenchPokemon bench1 = BenchPokemon.builder()
                .instanceId("bench-inst-1").cardId("xy1-1").maxHp(60).currentHp(60)
                .attachedEnergies(new ArrayList<>()).build();
        BenchPokemon bench2 = BenchPokemon.builder()
                .instanceId("bench-inst-2").cardId("xy1-2").maxHp(90).currentHp(80)
                .attachedEnergies(new ArrayList<>()).build();
        field.setBench(new ArrayList<>(List.of(bench1, bench2)));

        board.setPlayer1Field(field);
        board.setPlayer2Field(new PlayerField());
        board.getPlayer2Field().setPlayerId(2L);
    }

    private TrainerContext ctx() {
        return TrainerContext.builder().board(board).playerId(1L).build();
    }

    @Test
    void createsSHUFFLE_POKEMON_TO_DECK_pendingSelection() {
        ShufflePokemonIntoDeckTrainerEffect effect = new ShufflePokemonIntoDeckTrainerEffect();
        effect.setSource("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        assertFalse(events.isEmpty());
        PendingSelection pending = board.getPendingSelection();
        assertNotNull(pending);
        assertEquals(SelectionType.SHUFFLE_POKEMON_TO_DECK, pending.getType());
        assertEquals(1L, pending.getOwnerPlayerId());
        assertEquals(List.of("bench-inst-1", "bench-inst-2"), pending.getValidOptions());
        assertEquals(1, pending.getSelectionCount());
    }

    @Test
    void emptyBench_returnsEmptyAndNoPendingSelection() {
        field.setBench(new ArrayList<>());
        ShufflePokemonIntoDeckTrainerEffect effect = new ShufflePokemonIntoDeckTrainerEffect();
        effect.setSource("BENCH");

        List<GameEvent> events = logic.execute(effect, ctx());

        assertTrue(events.isEmpty());
        assertNull(board.getPendingSelection());
    }
}
