package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.ApplyConditionLogic;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyConditionLogicTest {

    private ApplyConditionLogic logic;
    private AttackContext context;
    private ActivePokemon defender;

    @BeforeEach
    void setUp() {
        logic = new ApplyConditionLogic();
        defender = new ActivePokemon();
        defender.setCardId("defender-1");

        context = new AttackContext();
        context.setDefenderPokemon(defender);
    }

    @Test
    void testApplyAsleepToDefender() {
        ApplyConditionEffect effect = new ApplyConditionEffect();
        effect.setTarget("DEFENDER");
        effect.setCondition("ASLEEP");

        logic.execute(effect, context);

        assertEquals(SpecialCondition.ASLEEP, defender.getCondition());
        assertEquals(1, context.getEvents().size());
    }

    @Test
    void testApplyPoisonedToDefender() {
        ApplyConditionEffect effect = new ApplyConditionEffect();
        effect.setTarget("DEFENDER");
        effect.setCondition("POISONED");

        logic.execute(effect, context);

        assertTrue(defender.isPoisoned());
        // Condition enum should not be overwritten by POISONED
        assertEquals(null, defender.getCondition());
    }
}
