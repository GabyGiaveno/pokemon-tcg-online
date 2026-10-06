package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.HealEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.MultiplierDamageEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.ApplyConditionLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.HealLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.AddDamageLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.MultiplierDamageLogic;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight Factory / Registry for Attack Effects.
 * This class holds singleton instances of EffectLogic implementations.
 */
public class EffectRegistry {

    private static final EffectRegistry INSTANCE = new EffectRegistry();

    private final Map<Class<? extends AttackEffect>, EffectLogic<? extends AttackEffect>> registry = new HashMap<>();

    private EffectRegistry() {
        // Register default logics
        register(ApplyConditionEffect.class, new ApplyConditionLogic());
        register(HealEffect.class, new HealLogic());
        register(AddDamageEffect.class, new AddDamageLogic());
        register(MultiplierDamageEffect.class, new MultiplierDamageLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.CoinFlipLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.DiscardEnergyEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.DiscardEnergyLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.PreventDamageEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.PreventDamageLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.SearchDeckEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.SearchDeckLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.SwitchPokemonEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.SwitchPokemonLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.RestrictEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.RestrictLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.DamageToBenchEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.DamageToBenchLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.DamageCountersEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.DamageCountersLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.LookAtDeckEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.LookAtDeckLogic());
        register(ar.edu.utn.frc.tup.piii.models.cards.effects.ShuffleHandEffect.class, new ar.edu.utn.frc.tup.piii.engine.effects.logics.ShuffleHandLogic());
        // TODO: Register others when implemented
    }

    public static EffectRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a logic handler for a specific effect type.
     *
     * @param effectClass The class of the effect data (DTO).
     * @param logic       The stateless Flyweight logic instance.
     * @param <T>         The specific type of AttackEffect.
     */
    public <T extends AttackEffect> void register(Class<T> effectClass, EffectLogic<T> logic) {
        registry.put(effectClass, logic);
    }

    /**
     * Retrieves the logic handler for a specific effect type.
     *
     * @param effectClass The class of the effect data.
     * @param <T>         The specific type of AttackEffect.
     * @return The logic handler, or null if not registered.
     */
    @SuppressWarnings("unchecked")
    public <T extends AttackEffect> EffectLogic<T> getLogic(Class<T> effectClass) {
        return (EffectLogic<T>) registry.get(effectClass);
    }
}
