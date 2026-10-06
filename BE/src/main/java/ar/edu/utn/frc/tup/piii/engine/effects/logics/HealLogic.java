package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.HealEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

import java.util.Map;

public class HealLogic implements EffectLogic<HealEffect> {

    @Override
    public void execute(HealEffect effectData, AttackContext ctx) {
        ActivePokemon target = getTargetPokemon(effectData.getTarget(), ctx);
        if (target == null) return;

        int amount = effectData.getAmount();
        int newHp = Math.min(target.getCurrentHp() + amount, target.getMaxHp());
        target.setCurrentHp(newHp);

        emitEvent(ctx, target, amount);
    }

    private void emitEvent(AttackContext ctx, ActivePokemon target, int amountHealed) {
        ctx.addEvent(GameEvent.of(
                GameEventType.POKEMON_HEALED,
                target.getCardId() + " healed " + amountHealed + " HP.",
                Map.of("cardId", target.getCardId(), "amount", amountHealed, "newHp", target.getCurrentHp())
        ));
    }

    @Override
    public boolean isPostDamage() {
        return true;
    }

    private ActivePokemon getTargetPokemon(String targetStr, AttackContext ctx) {
        if ("DEFENDER".equalsIgnoreCase(targetStr)) {
            return ctx.getDefenderPokemon();
        } else if ("SELF".equalsIgnoreCase(targetStr)) {
            return ctx.getAttackerPokemon();
        }
        return null; // For simplicity, bench healing could be added later
    }
}
