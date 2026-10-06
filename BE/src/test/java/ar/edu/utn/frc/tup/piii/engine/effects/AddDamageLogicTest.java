package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.AddDamageLogic;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddDamageLogicTest {

    @Test
    void testAddDamageWithoutCondition() {
        AddDamageLogic logic = new AddDamageLogic();
        AttackContext context = new AttackContext();
        context.setDamageModifiers(10); // base damage modifier

        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(30);
        // no condition set

        logic.execute(effect, context);

        // 10 base + 30 added
        assertEquals(40, context.getDamageModifiers());
    }

    @Test
    void testAddDamageWithMockedCondition() {
        AddDamageLogic logic = new AddDamageLogic();
        AttackContext context = new AttackContext();
        context.setDamageModifiers(0);

        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(20);
        effect.setCondition("OPPONENT_IS_POISONED"); // Mock condition that evaluates to true in the stub

        logic.execute(effect, context);

        assertEquals(20, context.getDamageModifiers());
    }
}
