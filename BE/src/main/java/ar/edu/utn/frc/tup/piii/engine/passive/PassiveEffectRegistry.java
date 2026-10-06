package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.passive.stadiums.FairyGardenEffect;
import ar.edu.utn.frc.tup.piii.engine.passive.stadiums.ShadowCircleEffect;
import ar.edu.utn.frc.tup.piii.engine.passive.tools.HardCharmEffect;
import ar.edu.utn.frc.tup.piii.engine.passive.tools.MuscleBandEffect;

import java.util.HashMap;
import java.util.Map;

public class PassiveEffectRegistry {

    private static final PassiveEffectRegistry INSTANCE = new PassiveEffectRegistry();

    private final Map<String, ToolEffect> toolEffects = new HashMap<>();
    private final Map<String, StadiumEffect> stadiumEffects = new HashMap<>();

    private PassiveEffectRegistry() {
        toolEffects.put("xy1-121", new MuscleBandEffect());
        toolEffects.put("xy1-119", new HardCharmEffect());
        stadiumEffects.put("xy1-117", new FairyGardenEffect());
        stadiumEffects.put("xy1-126", new ShadowCircleEffect());
    }

    public static PassiveEffectRegistry getInstance() { return INSTANCE; }

    public ToolEffect getToolEffect(String cardId) { return toolEffects.get(cardId); }

    public StadiumEffect getStadiumEffect(String cardId) { return stadiumEffects.get(cardId); }
}
