package ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.TrainerEffectLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;

/**
 * Executes each parsed {@link TrainerEffect} via the {@link TrainerEffectRegistry} (Flyweight).
 * Effects without a registered logic (e.g. {@code UnknownTrainerEffect}) are skipped.
 */
public class TrainerEffectExecutionHandler implements TrainerHandler {

    @Override
    public void handle(TrainerContext ctx) {
        if (ctx.getParsedEffects() == null) return;
        TrainerEffectRegistry registry = TrainerEffectRegistry.getInstance();
        for (TrainerEffect effect : ctx.getParsedEffects()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            TrainerEffectLogic logic = registry.getLogic(effect.getClass());
            if (logic != null) {
                ctx.getEvents().addAll(logic.execute(effect, ctx));
            }
        }
    }
}
