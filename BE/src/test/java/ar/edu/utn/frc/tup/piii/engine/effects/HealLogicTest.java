package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.HealLogic;
import ar.edu.utn.frc.tup.piii.models.cards.effects.HealEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealLogicTest {

    private HealLogic healLogic;
    private AttackContext context;
    private ActivePokemon attacker;
    private ActivePokemon defender;

    @BeforeEach
    void setUp() {
        healLogic = new HealLogic();
        attacker = new ActivePokemon();
        attacker.setCardId("attacker-1");
        attacker.setMaxHp(100);
        attacker.setCurrentHp(50);

        defender = new ActivePokemon();
        defender.setCardId("defender-1");
        defender.setMaxHp(120);
        defender.setCurrentHp(100);

        context = new AttackContext();
        context.setAttackerPokemon(attacker);
        context.setDefenderPokemon(defender);
    }

    @Test
    void testHealSelfWithinBounds() {
        HealEffect effect = new HealEffect();
        effect.setTarget("SELF");
        effect.setAmount(30);

        healLogic.execute(effect, context);

        // 50 + 30 = 80
        assertEquals(80, attacker.getCurrentHp());
        assertEquals(1, context.getEvents().size());
    }

    @Test
    void testHealSelfExceedsMaxHp() {
        HealEffect effect = new HealEffect();
        effect.setTarget("SELF");
        effect.setAmount(100); // Healing 100 on 50 HP with 100 Max

        healLogic.execute(effect, context);

        // 50 + 100 = 150, but max is 100
        assertEquals(100, attacker.getCurrentHp());
    }

    @Test
    void testHealDefender() {
        HealEffect effect = new HealEffect();
        effect.setTarget("DEFENDER");
        effect.setAmount(10);

        healLogic.execute(effect, context);

        // 100 + 10 = 110
        assertEquals(110, defender.getCurrentHp());
    }
}
