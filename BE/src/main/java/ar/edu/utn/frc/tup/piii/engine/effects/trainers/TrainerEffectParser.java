package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Parses the {@code trainerEffects} block embedded in {@code Card.parsedEffects}
 * (the same JSON string that already carries attacks/abilities) into typed
 * {@link TrainerEffect} objects via Jackson polymorphism.
 *
 * <p>Mirror of {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackParser} (Bloque 1): the mapper
 * tolerates unknown properties and a {@code type} without a registered subtype degrades to
 * {@link ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.UnknownTrainerEffect} (inert),
 * never crashing the engine.
 */
public final class TrainerEffectParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<TrainerEffect>> TRAINER_EFFECT_LIST_TYPE =
            new TypeReference<>() {};

    private TrainerEffectParser() {}

    /**
     * @param parsedEffectsJson the {@code Card.parsedEffects} JSON string
     *                          ({@code {attacks, abilities, trainerEffects}})
     * @return parsed trainer effects, or an empty list if absent, blank or malformed
     */
    public static List<TrainerEffect> parse(String parsedEffectsJson) {
        if (parsedEffectsJson == null || parsedEffectsJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = MAPPER.readTree(parsedEffectsJson);
            JsonNode trainerEffects = root.get("trainerEffects");
            if (trainerEffects == null || trainerEffects.isNull()) return Collections.emptyList();
            return MAPPER.convertValue(trainerEffects, TRAINER_EFFECT_LIST_TYPE);
        } catch (Exception e) {
            System.err.println("Failed to parse trainer effects: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
