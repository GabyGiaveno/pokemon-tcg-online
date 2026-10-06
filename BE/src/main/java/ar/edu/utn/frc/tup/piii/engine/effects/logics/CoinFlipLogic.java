package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectRegistry;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipEffect;

import java.util.Map;
import java.util.Random;

public class CoinFlipLogic implements EffectLogic<CoinFlipEffect> {

    private final Random random;

    public CoinFlipLogic() {
        this.random = new Random();
    }

    public CoinFlipLogic(Random random) {
        this.random = random;
    }

    @Override
    public void execute(CoinFlipEffect effectData, AttackContext ctx) {
        boolean heads = random.nextBoolean();

        ctx.addEvent(GameEvent.of(
                GameEventType.COIN_FLIPPED,
                heads ? "Heads!" : "Tails!",
                Map.of("flip", heads ? "HEADS" : "TAILS")));

        EffectRegistry registry = EffectRegistry.getInstance();
        if (heads && effectData.getIfHeads() != null) {
            for (AttackEffect subEffect : effectData.getIfHeads()) {
                @SuppressWarnings("unchecked")
                EffectLogic<AttackEffect> logic = (EffectLogic<AttackEffect>) registry.getLogic(subEffect.getClass());
                if (logic != null) logic.execute(subEffect, ctx);
            }
        } else if (!heads && effectData.getIfTails() != null) {
            for (AttackEffect subEffect : effectData.getIfTails()) {
                @SuppressWarnings("unchecked")
                EffectLogic<AttackEffect> logic = (EffectLogic<AttackEffect>) registry.getLogic(subEffect.getClass());
                if (logic != null) logic.execute(subEffect, ctx);
            }
        }
    }

    /**
     * Runs in the pre-damage phase: the coin is flipped once and all sub-effects are dispatched
     * inline. Damage-bonus sub-effects (ADD_DAMAGE) must resolve before DamageApplicationHandler.
     */
    @Override
    public boolean isPreDamage() {
        return true;
    }
}
