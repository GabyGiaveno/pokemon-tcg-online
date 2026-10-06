package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PreventDamageEffect;

public class PreventDamageLogic implements EffectLogic<PreventDamageEffect> {

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(PreventDamageEffect effect, AttackContext ctx) {
        ctx.getAttackerPokemon().setDamageProtected(true);
        ctx.addEvent(GameEvent.of(
                GameEventType.DAMAGE_PREVENTED,
                ctx.getAttackerPokemon().getCardId()
                        + " is now protected from the opponent's next attack."));
    }
}
