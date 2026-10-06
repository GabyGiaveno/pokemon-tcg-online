package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageCountersEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCountersLogicTest {

    @Test
    void defenderTarget_placesCounters() {
        ActivePokemon defender = new ActivePokemon();
        defender.setCardId("defender-1");
        defender.setMaxHp(100);
        defender.setCurrentHp(100);

        DamageCountersEffect effect = new DamageCountersEffect();
        effect.setAmount(3);          // 3 counters = 30 HP
        effect.setTarget("DEFENDER");

        AttackContext ctx = AttackContext.builder().defenderPokemon(defender).build();
        new DamageCountersLogic().execute(effect, ctx);

        assertEquals(70, defender.getCurrentHp());
    }

    @Test
    void negativeAmount_isNoOp() {
        ActivePokemon defender = new ActivePokemon();
        defender.setCardId("defender-1");
        defender.setMaxHp(100);
        defender.setCurrentHp(100);

        DamageCountersEffect effect = new DamageCountersEffect();
        effect.setAmount(-1);         // "hasta HP 10" → fuera de alcance Bloque 2
        effect.setTarget("DEFENDER");

        AttackContext ctx = AttackContext.builder().defenderPokemon(defender).build();
        new DamageCountersLogic().execute(effect, ctx);

        assertEquals(100, defender.getCurrentHp(), "amount<=0 no debe hacer nada en este bloque");
    }
}
