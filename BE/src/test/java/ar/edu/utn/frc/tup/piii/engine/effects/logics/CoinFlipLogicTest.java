package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoinFlipLogicTest {

    @Test
    void isPreDamage_true() {
        assertTrue(new CoinFlipLogic().isPreDamage(),
                "COIN_FLIP debe correr en pre-damage para que ADD_DAMAGE afecte el daño");
    }

    @Test
    void heads_executesIfHeadsSubEffects() {
        Random random = mock(Random.class);
        when(random.nextBoolean()).thenReturn(true); // cara

        CoinFlipEffect effect = new CoinFlipEffect();
        AddDamageEffect add = new AddDamageEffect();
        add.setAmount(20);
        effect.setIfHeads(List.of(add));
        effect.setIfTails(List.of());

        AttackContext ctx = AttackContext.builder().damageModifiers(0).build();
        new CoinFlipLogic(random).execute(effect, ctx);

        assertEquals(20, ctx.getDamageModifiers(), "Con cara, el ADD_DAMAGE de ifHeads debe sumar 20");
    }

    @Test
    void tails_doesNotExecuteIfHeads() {
        Random random = mock(Random.class);
        when(random.nextBoolean()).thenReturn(false); // cruz

        CoinFlipEffect effect = new CoinFlipEffect();
        AddDamageEffect add = new AddDamageEffect();
        add.setAmount(20);
        effect.setIfHeads(List.of(add));
        effect.setIfTails(List.of());

        AttackContext ctx = AttackContext.builder().damageModifiers(0).build();
        new CoinFlipLogic(random).execute(effect, ctx);

        assertEquals(0, ctx.getDamageModifiers(), "Con cruz, no se aplica ifHeads");
    }
}
