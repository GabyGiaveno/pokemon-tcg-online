package ar.edu.utn.frc.tup.piii.engine.effects.conditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton registry that maps string condition keys (from JSON) to concrete {@link ConditionStrategy} instances.
 */
public class ConditionRegistry {

    private static ConditionRegistry instance;
    private final Map<String, ConditionStrategy> strategies = new HashMap<>();

    private ConditionRegistry() {
        registerDefaultConditions();
    }

    public static synchronized ConditionRegistry getInstance() {
        if (instance == null) {
            instance = new ConditionRegistry();
        }
        return instance;
    }

    public void register(String key, ConditionStrategy strategy) {
        strategies.put(key, strategy);
    }

    public ConditionStrategy getStrategy(String key) {
        return strategies.get(key);
    }

    private void registerDefaultConditions() {
        register("OPPONENT_IS_GRASS", new OpponentIsGrassCondition());
        register("DEFENDER_IS_EX", new DefenderIsExCondition());
        register("DEFENDER_HAS_DAMAGE_COUNTERS", new DefenderHasDamageCountersCondition());
        register("DEFENDER_HAS_SPECIAL_CONDITION", new DefenderHasSpecialCondition());
        register("SELF_HAS_ENERGY_PSYCHIC", new SelfHasEnergyPsychicCondition());
        register("LUNATONE_ON_BENCH", new LunatoneOnBenchCondition());
    }
}
