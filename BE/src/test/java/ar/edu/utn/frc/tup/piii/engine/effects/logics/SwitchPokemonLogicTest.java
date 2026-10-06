package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.SwitchPokemonEffect;
import ar.edu.utn.frc.tup.piii.models.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwitchPokemonLogicTest {

    private SwitchPokemonLogic logic;
    private PlayerField attackerField;
    private PlayerField defenderField;
    private AttackContext ctx;

    @BeforeEach
    void setUp() {
        logic = new SwitchPokemonLogic();

        attackerField = new PlayerField();
        attackerField.setPlayerId(1L);
        attackerField.setTurnFlags(new TurnFlags());
        attackerField.setBench(new ArrayList<>());

        defenderField = new PlayerField();
        defenderField.setPlayerId(2L);
        defenderField.setTurnFlags(new TurnFlags());
        defenderField.setBench(new ArrayList<>());

        ctx = AttackContext.builder()
                .attackerPokemon(ActivePokemon.builder().cardId("attacker").build())
                .defenderPokemon(ActivePokemon.builder().cardId("defender").build())
                .attackerField(attackerField)
                .defenderField(defenderField)
                .board(new BoardState())
                .cardLookup(id -> null)
                .build();
    }

    private SwitchPokemonEffect effect(String target, boolean force) {
        SwitchPokemonEffect e = new SwitchPokemonEffect();
        e.setTarget(target);
        e.setForce(force);
        return e;
    }

    private BenchPokemon bench(String cardId, int hp) {
        return BenchPokemon.builder().cardId(cardId).instanceId(cardId + "-inst")
                .currentHp(hp).maxHp(hp).attachedEnergies(new ArrayList<>()).build();
    }

    @Test
    void isPostDamage_returnsTrue() {
        assertTrue(logic.isPostDamage());
    }

    @Test
    void execute_opponentTarget_setsPendingSelectionOnDefenderField() {
        defenderField.getBench().add(bench("xy1-1", 80));
        logic.execute(effect("OPPONENT", true), ctx);
        PendingSelection sel = ctx.getBoard().getPendingSelection();
        assertNotNull(sel);
        assertEquals(SelectionType.SWITCH_POKEMON, sel.getType());
        assertEquals(2L, sel.getOwnerPlayerId());
    }

    @Test
    void execute_selfTarget_setsPendingSelectionOnAttackerField() {
        attackerField.getBench().add(bench("xy1-2", 80));
        logic.execute(effect("SELF", true), ctx);
        PendingSelection sel = ctx.getBoard().getPendingSelection();
        assertNotNull(sel);
        assertEquals(SelectionType.SWITCH_POKEMON, sel.getType());
        assertEquals(1L, sel.getOwnerPlayerId());
    }

    @Test
    void execute_emptyBench_noPendingSelection() {
        logic.execute(effect("OPPONENT", true), ctx);
        assertNull(ctx.getBoard().getPendingSelection());
    }

    @Test
    void execute_validOptionContainsBenchInstanceId() {
        defenderField.getBench().add(bench("xy1-1", 80));
        logic.execute(effect("OPPONENT", true), ctx);
        assertTrue(ctx.getBoard().getPendingSelection().getValidOptions().contains("xy1-1-inst"));
    }

    @Test
    void execute_existingPendingSelection_skipsSecondSwitch() {
        attackerField.getBench().add(bench("xy1-2", 80));
        defenderField.getBench().add(bench("xy1-1", 80));
        // First switch sets selection
        logic.execute(effect("OPPONENT", true), ctx);
        PendingSelection first = ctx.getBoard().getPendingSelection();
        // Second switch (Rapid Spin dual-switch) should be skipped
        logic.execute(effect("SELF", true), ctx);
        assertSame(first, ctx.getBoard().getPendingSelection(), "second switch must not overwrite first");
    }
}
