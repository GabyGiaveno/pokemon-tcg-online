package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;

public class AddDamageLogic implements EffectLogic<AddDamageEffect> {

    @Override
    public void execute(AddDamageEffect effectData, AttackContext ctx) {
        // Self-targeting damage (recoil) is handled post-damage in PostDamageHandler.
        if ("SELF".equalsIgnoreCase(effectData.getTarget())) return;

        // Evaluate condition if present
        if (effectData.getCondition() != null && !effectData.getCondition().isBlank()) {
            ar.edu.utn.frc.tup.piii.engine.effects.conditions.ConditionStrategy strategy = 
                ar.edu.utn.frc.tup.piii.engine.effects.conditions.ConditionRegistry.getInstance().getStrategy(effectData.getCondition());
            
            if (strategy != null) {
                if (!strategy.evaluate(ctx)) {
                    return; // Condition not met, skip applying damage
                }
            } else {
                // If strategy is unknown, we log and default to true (or skip, but true is safer for MVP)
                System.err.println("Unknown condition strategy: " + effectData.getCondition());
            }
        }

        // Apply damage modifier
        ctx.setDamageModifiers(ctx.getDamageModifiers() + effectData.getAmount());
    }

    @Override
    public boolean isPreDamage() {
        return true;
    }
}
