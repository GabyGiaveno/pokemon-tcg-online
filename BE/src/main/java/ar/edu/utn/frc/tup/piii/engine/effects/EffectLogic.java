package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;

/**
 * Flyweight interface for executing attack effects.
 * Implementations should be stateless singletons that operate on the provided state (AttackEffect DTO + AttackContext).
 *
 * @param <T> The specific type of AttackEffect this logic handles.
 */
public interface EffectLogic<T extends AttackEffect> {
    
    /**
     * Executes the effect.
     *
     * @param effectData The data transfer object mapped from the parsed JSON.
     * @param context    The current attack context (board state, attackers, defenders, etc).
     */
    void execute(T effectData, AttackContext context);

    /**
     * Determines if this logic should run in the PreAttackHandler (before damage is applied).
     */
    default boolean isPreDamage() {
        return false;
    }

    /**
     * Determines if this logic should run in the PostDamageHandler (after damage is applied).
     */
    default boolean isPostDamage() {
        return false;
    }
}
