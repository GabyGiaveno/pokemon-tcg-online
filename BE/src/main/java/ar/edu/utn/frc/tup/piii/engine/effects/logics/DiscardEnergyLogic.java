package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DiscardEnergyEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;

import java.util.List;
import java.util.Map;

public class DiscardEnergyLogic implements EffectLogic<DiscardEnergyEffect> {

    @Override
    public void execute(DiscardEnergyEffect effectData, AttackContext ctx) {
        ActivePokemon target;
        if ("SELF".equalsIgnoreCase(effectData.getTarget())) {
            target = ctx.getAttackerPokemon();
        } else if ("DEFENDER".equalsIgnoreCase(effectData.getTarget())) {
            target = ctx.getDefenderPokemon();
        } else {
            return;
        }

        List<AttachedCard> energies = target.getAttachedEnergies();
        if (energies == null || energies.isEmpty()) return;

        int amount = effectData.getAmount();
        // -1 means "all"
        int toDiscard = amount < 0 ? energies.size() : Math.min(amount, energies.size());

        for (int i = 0; i < toDiscard; i++) {
            if (!energies.isEmpty()) {
                energies.remove(energies.size() - 1);
            }
        }

        ctx.addEvent(GameEvent.of(
                GameEventType.ENERGY_DISCARDED,
                "Discarded " + toDiscard + " energy from " + target.getCardId(),
                Map.of("cardId", target.getCardId(), "amount", toDiscard)));
    }

    /** Runs after damage is applied (energy discard as part of the attack's resolution). */
    @Override
    public boolean isPostDamage() {
        return true;
    }
}
