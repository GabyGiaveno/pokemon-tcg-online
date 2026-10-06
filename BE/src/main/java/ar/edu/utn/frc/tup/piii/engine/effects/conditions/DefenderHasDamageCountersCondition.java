package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;

public class DefenderHasDamageCountersCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        var defender = ctx.getDefenderPokemon();
        if (defender == null) return false;
        int dmg = defender.getMaxHp() - defender.getCurrentHp();
        return dmg > 0;
    }
}
