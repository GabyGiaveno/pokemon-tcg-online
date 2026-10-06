package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.MultiplierDamageEffect;

import java.security.SecureRandom;
import java.util.Map;

public class MultiplierDamageLogic implements EffectLogic<MultiplierDamageEffect> {

    private final java.util.Random random;

    public MultiplierDamageLogic() {
        this(new java.util.Random());
    }

    public MultiplierDamageLogic(java.util.Random random) {
        this.random = random;
    }

    @Override
    public void execute(MultiplierDamageEffect effectData, AttackContext ctx) {
        int units = 0;

        if ("COIN_FLIPS".equalsIgnoreCase(effectData.getUnitType()) && effectData.getFlips() != null) {
            int flips = effectData.getFlips();
            for (int i = 0; i < flips; i++) {
                if (random.nextBoolean()) {
                    units++;
                }
            }
            ctx.addEvent(GameEvent.of(
                    GameEventType.COIN_FLIPPED,
                    "Flipped " + flips + " coins, got " + units + " heads!",
                    Map.of("flips", flips, "heads", units)
            ));
        } else if ("ENERGY_ON_SELF".equalsIgnoreCase(effectData.getUnitType())) {
            var energies = ctx.getAttackerPokemon().getAttachedEnergies();
            units = (energies != null) ? energies.size() : 0;
        }

        int extraDamage = effectData.getAmountPerUnit() * units;
        ctx.setDamageModifiers(ctx.getDamageModifiers() + extraDamage);
    }

    @Override
    public boolean isPreDamage() {
        return true;
    }
}
