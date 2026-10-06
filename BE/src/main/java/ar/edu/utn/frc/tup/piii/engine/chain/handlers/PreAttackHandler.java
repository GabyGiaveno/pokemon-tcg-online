package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;

import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectRegistry;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.MultiplierDamageEffect;

/**
 * Handler 4 — Pre-attack effects.
 *
 * <p>Responsible for processing any effects that must resolve <em>before</em> damage is applied:
 * <ul>
 *   <li>Mandatory energy discards (e.g. "Discard 2 Fire Energy attached to this Pokémon").</li>
 *   <li>Coin-flip damage bonuses declared before damage (e.g. "Flip a coin; if heads,
 *       this attack does 30 more damage").</li>
 *   <li>Self-effects that modify the attacker's state before the hit lands.</li>
 * </ul>
 *
 * <p>Now uses the Flyweight EffectRegistry to execute parsed effects.
 */
public class PreAttackHandler implements AttackHandler {

    @Override
    public void handle(AttackContext ctx) {
        if (!ctx.getAttackData().hasParsedEffects()) return;

        EffectRegistry registry = EffectRegistry.getInstance();
        for (AttackEffect effect : ctx.getAttackData().getParsedEffects()) {
            @SuppressWarnings("unchecked")
            EffectLogic<AttackEffect> logic = (EffectLogic<AttackEffect>) registry.getLogic(effect.getClass());
            
            if (logic != null && logic.isPreDamage()) {
                logic.execute(effect, ctx);
            }
        }
    }
}
