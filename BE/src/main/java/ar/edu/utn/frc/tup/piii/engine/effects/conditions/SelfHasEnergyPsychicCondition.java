package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;

public class SelfHasEnergyPsychicCondition implements ConditionStrategy {
    @Override
    public boolean evaluate(AttackContext ctx) {
        var energies = ctx.getAttackerPokemon().getAttachedEnergies();
        if (energies == null) return false;

        for (var attached : energies) {
            Card energyCard = ctx.getCardLookup().findById(attached.getCardId());
            if (energyCard != null && energyCard.getTypes() != null
                    && energyCard.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase("Psychic"))) {
                return true;
            }
        }
        return false;
    }
}
