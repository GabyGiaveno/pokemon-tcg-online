package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;

/**
 * Strategy pattern interface for evaluating dynamic conditions during attack resolution.
 * E.g., "If the defending Pokémon is Grass type", "If this Pokémon has Psychic energy", etc.
 */
public interface ConditionStrategy {
    boolean evaluate(AttackContext ctx);
}
