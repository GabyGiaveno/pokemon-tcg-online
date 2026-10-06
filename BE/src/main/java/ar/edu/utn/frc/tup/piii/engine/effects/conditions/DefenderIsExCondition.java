package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;

public class DefenderIsExCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        Card defenderCard = ctx.getDefenderCard();
        return defenderCard != null && defenderCard.getSubtypes() != null
                && defenderCard.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("EX"));
    }
}
