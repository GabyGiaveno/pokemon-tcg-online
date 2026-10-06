package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;

public class DefenderHasSpecialCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        var defender = ctx.getDefenderPokemon();
        if (defender == null) return false;
        return (defender.getCondition() != null && defender.getCondition() != SpecialCondition.NONE)
                || defender.isBurned() || defender.isPoisoned();
    }
}
