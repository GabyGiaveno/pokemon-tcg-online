package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;

public class LunatoneOnBenchCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        var bench = ctx.getAttackerField().getBench();
        if (bench == null) return false;

        for (var bp : bench) {
            Card benchCard = ctx.getCardLookup().findById(bp.getCardId());
            if (benchCard != null && "Lunatone".equalsIgnoreCase(benchCard.getName())) {
                return true;
            }
        }
        return false;
    }
}
