package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageToBenchEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DamageToBenchLogicTest {

    private DamageToBenchLogic logic;
    private PlayerField attackerField;
    private PlayerField defenderField;
    private AttackContext ctx;

    @BeforeEach
    void setUp() {
        logic = new DamageToBenchLogic(new KnockoutProcessor());

        attackerField = new PlayerField();
        attackerField.setPlayerId(1L);
        attackerField.setTurnFlags(new TurnFlags());
        attackerField.setBench(new ArrayList<>());
        attackerField.setDiscardPile(new ArrayList<>());
        attackerField.setPrizeCards(new ArrayList<>(List.of("prize1", "prize2")));
        attackerField.setHand(new ArrayList<>());

        defenderField = new PlayerField();
        defenderField.setPlayerId(2L);
        defenderField.setTurnFlags(new TurnFlags());
        defenderField.setBench(new ArrayList<>());
        defenderField.setDiscardPile(new ArrayList<>());
        defenderField.setPrizeCards(new ArrayList<>(List.of("prize3", "prize4")));
        defenderField.setHand(new ArrayList<>());

        ctx = AttackContext.builder()
                .attackerField(attackerField)
                .defenderField(defenderField)
                .attackerPokemon(ActivePokemon.builder().cardId("attacker").build())
                .board(new BoardState())
                .cardLookup(id -> null)
                .build();
    }

    private DamageToBenchEffect effect(int amount, int targetCount, String target) {
        DamageToBenchEffect e = new DamageToBenchEffect();
        e.setAmount(amount);
        e.setTargetCount(targetCount);
        e.setTarget(target);
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
    void execute_dealsDamageToOpponentBench() {
        defenderField.getBench().add(bench("xy1-1", 60));
        logic.execute(effect(10, 1, "OPPONENT_BENCH"), ctx);
        assertEquals(50, defenderField.getBench().get(0).getCurrentHp());
    }

    @Test
    void execute_dealsDamageToSelfBench() {
        attackerField.getBench().add(bench("xy1-2", 60));
        logic.execute(effect(10, -1, "SELF_BENCH"), ctx);
        assertEquals(50, attackerField.getBench().get(0).getCurrentHp());
    }

    @Test
    void execute_targetCountLimitsHits() {
        defenderField.getBench().add(bench("xy1-1", 60));
        defenderField.getBench().add(bench("xy1-2", 60));
        defenderField.getBench().add(bench("xy1-3", 60));
        logic.execute(effect(20, 2, "OPPONENT_BENCH"), ctx);
        assertEquals(40, defenderField.getBench().get(0).getCurrentHp());
        assertEquals(40, defenderField.getBench().get(1).getCurrentHp());
        assertEquals(60, defenderField.getBench().get(2).getCurrentHp(), "third slot untouched");
    }

    @Test
    void execute_targetCountMinusOneHitsAllBench() {
        attackerField.getBench().add(bench("xy1-1", 60));
        attackerField.getBench().add(bench("xy1-2", 60));
        logic.execute(effect(10, -1, "SELF_BENCH"), ctx);
        assertEquals(50, attackerField.getBench().get(0).getCurrentHp());
        assertEquals(50, attackerField.getBench().get(1).getCurrentHp());
    }

    @Test
    void execute_emitsDamageDealtEventPerTarget() {
        defenderField.getBench().add(bench("xy1-1", 60));
        defenderField.getBench().add(bench("xy1-2", 60));
        logic.execute(effect(10, 2, "OPPONENT_BENCH"), ctx);
        long dmgEvents = ctx.getEvents().stream()
                .filter(e -> e.getType() == GameEventType.DAMAGE_DEALT).count();
        assertEquals(2, dmgEvents);
    }

    @Test
    void execute_emptyBench_doesNothing() {
        logic.execute(effect(10, 1, "OPPONENT_BENCH"), ctx);
        assertTrue(ctx.getEvents().isEmpty());
    }

    @Test
    void execute_knockedOutBenchPokemon_removedAndPrizeAwarded() {
        defenderField.getBench().add(bench("xy1-1", 10));
        logic.execute(effect(10, 1, "OPPONENT_BENCH"), ctx);

        assertTrue(defenderField.getBench().isEmpty(), "KO'd bench slot must be removed");
        assertTrue(attackerField.getHand().contains("prize1"), "attacker takes from own prize pile");
        long koEvents = ctx.getEvents().stream()
                .filter(e -> e.getType() == GameEventType.POKEMON_KNOCKED_OUT).count();
        assertEquals(1, koEvents);
    }

    @Test
    void execute_benchKo_doesNotSetPendingSelection() {
        defenderField.getBench().add(bench("xy1-1", 10));
        logic.execute(effect(10, 1, "OPPONENT_BENCH"), ctx);
        assertNull(ctx.getBoard().getPendingSelection(), "bench KO must not trigger promotion selection");
    }

    @Test
    void execute_hpFloorIsZero() {
        defenderField.getBench().add(bench("xy1-1", 5));
        logic.execute(effect(50, 1, "OPPONENT_BENCH"), ctx);
        // After KO the slot is removed; no crash from negative HP
        assertTrue(defenderField.getBench().isEmpty());
    }
}
