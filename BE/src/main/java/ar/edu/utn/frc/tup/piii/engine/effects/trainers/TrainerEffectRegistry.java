package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.CoinFlipTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DiscardEnergyTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DiscardHandDrawTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DrawCardsTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.HealTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.RecycleTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.SearchDeckTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.ShuffleHandTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.ShufflePokemonIntoDeckTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight Factory / Registry for Trainer Effects.
 */
public class TrainerEffectRegistry {

    private static final TrainerEffectRegistry INSTANCE = new TrainerEffectRegistry();

    private final Map<Class<? extends TrainerEffect>, TrainerEffectLogic<? extends TrainerEffect>> registry = new HashMap<>();

    private TrainerEffectRegistry() {
        // Register default logics
        register(DrawCardsTrainerEffect.class, new DrawCardsTrainerLogic());
        register(DiscardHandDrawTrainerEffect.class, new DiscardHandDrawTrainerLogic());
        register(ShuffleHandTrainerEffect.class, new ShuffleHandTrainerLogic());
        register(DiscardEnergyTrainerEffect.class, new DiscardEnergyTrainerLogic());
        register(CoinFlipTrainerEffect.class, new CoinFlipTrainerLogic());
        register(HealTrainerEffect.class, new HealTrainerLogic());
        register(SearchDeckTrainerEffect.class, new SearchDeckTrainerLogic());
        register(ShufflePokemonIntoDeckTrainerEffect.class, new ShufflePokemonIntoDeckTrainerLogic());
        register(RecycleTrainerEffect.class, new RecycleTrainerLogic());
    }

    public static TrainerEffectRegistry getInstance() {
        return INSTANCE;
    }

    public <T extends TrainerEffect> void register(Class<T> effectClass, TrainerEffectLogic<T> logic) {
        registry.put(effectClass, logic);
    }

    @SuppressWarnings("unchecked")
    public <T extends TrainerEffect> TrainerEffectLogic<T> getLogic(Class<T> effectClass) {
        return (TrainerEffectLogic<T>) registry.get(effectClass);
    }
}
