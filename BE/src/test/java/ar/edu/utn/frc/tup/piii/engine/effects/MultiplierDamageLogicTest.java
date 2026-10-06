package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.MultiplierDamageLogic;
import ar.edu.utn.frc.tup.piii.models.cards.effects.MultiplierDamageEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiplierDamageLogicTest {

    @Test
    void testCoinFlipMultiplier() {
        MultiplierDamageLogic logic = new MultiplierDamageLogic();
        AttackContext context = new AttackContext();
        context.setDamageModifiers(10); // Base modifiers already present

        MultiplierDamageEffect effect = new MultiplierDamageEffect();
        effect.setUnitType("COIN_FLIPS");
        effect.setFlips(3);
        effect.setAmountPerUnit(20);

        logic.execute(effect, context);

        // Since it's secure random, we can't assert exact numbers, but we can assert bounds.
        // Units can be 0, 1, 2, or 3.
        // Extra damage can be 0, 20, 40, or 60.
        // Total damage modifiers: 10 + (0 to 60) -> 10 to 70.
        int finalMods = context.getDamageModifiers();
        assertTrue(finalMods >= 10 && finalMods <= 70);
        assertTrue((finalMods - 10) % 20 == 0); // Must be a multiple of 20
        assertEquals(1, context.getEvents().size());
    }
}
