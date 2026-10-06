package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

import java.util.Map;

public class ApplyConditionLogic implements EffectLogic<ApplyConditionEffect> {

    @Override
    public void execute(ApplyConditionEffect effectData, AttackContext ctx) {
        ActivePokemon target = getTargetPokemon(effectData.getTarget(), ctx);
        if (target == null) return;

        // Continuous immunity guard (e.g. Sweet Veil): the target's own side may carry an
        // ability that blocks Special Conditions on it (design D9).
        var ownerField = "SELF".equalsIgnoreCase(effectData.getTarget())
                ? ctx.getAttackerField()
                : ctx.getDefenderField();
        if (ar.edu.utn.frc.tup.piii.engine.effects.abilities.ContinuousAbilityQuery
                .isImmuneToConditions(target, ownerField, ctx.getCardLookup())) {
            ctx.addEvent(GameEvent.of(
                    GameEventType.STATUS_EFFECT_CLEARED,
                    target.getCardId() + " is protected from Special Conditions by an ability.",
                    Map.of("cardId", target.getCardId(), "blocked", effectData.getCondition())));
            return;
        }

        String conditionName = effectData.getCondition().toUpperCase();

        switch (conditionName) {
            case "ASLEEP":
                target.setCondition(SpecialCondition.ASLEEP);
                emitEvent(ctx, target, "ASLEEP", "is now Asleep");
                break;
            case "PARALYZED":
                target.setCondition(SpecialCondition.PARALYZED);
                emitEvent(ctx, target, "PARALYZED", "is now Paralyzed");
                break;
            case "CONFUSED":
                target.setCondition(SpecialCondition.CONFUSED);
                emitEvent(ctx, target, "CONFUSED", "is now Confused");
                break;
            case "BURNED":
                target.setBurned(true);
                emitEvent(ctx, target, "BURNED", "is now Burned");
                break;
            case "POISONED":
                target.setPoisoned(true);
                emitEvent(ctx, target, "POISONED", "is now Poisoned");
                break;
        }
    }

    private ActivePokemon getTargetPokemon(String targetStr, AttackContext ctx) {
        if ("DEFENDER".equalsIgnoreCase(targetStr)) {
            return ctx.getDefenderPokemon();
        } else if ("SELF".equalsIgnoreCase(targetStr)) {
            return ctx.getAttackerPokemon();
        }
        return null;
    }

    private void emitEvent(AttackContext ctx, ActivePokemon target, String conditionCode, String description) {
        ctx.addEvent(GameEvent.of(
                GameEventType.STATUS_EFFECT_APPLIED,
                target.getCardId() + " " + description + "!",
                Map.of("cardId", target.getCardId(), "condition", conditionCode)
        ));
    }

    @Override
    public boolean isPostDamage() {
        return true;
    }
}
