package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;

public class OpponentIsGrassCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        Card defenderCard = ctx.getDefenderCard();
        return defenderCard != null && defenderCard.getTypes() != null
                && defenderCard.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase("Grass"));
    }
}
