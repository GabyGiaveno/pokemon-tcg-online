package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Parses the {@code abilities} block embedded in {@code Card.parsedEffects} (the same JSON
 * string that already carries attacks/trainerEffects) into typed {@link AbilityData} objects.
 *
 * <p>Mirror of {@link ar.edu.utn.frc.tup.piii.engine.effects.trainers.TrainerEffectParser}:
 * the mapper tolerates unknown properties and an unmapped nested {@code type} degrades to
 * {@link ar.edu.utn.frc.tup.piii.models.cards.effects.UnknownEffect} (inert), never crashing
 * the engine. This closes the gap where the abilities block travelled to the entity but no
 * consumer ever read it.
 */
public final class AbilityParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<AbilityData>> ABILITY_LIST_TYPE =
            new TypeReference<>() {};

    private AbilityParser() {}

    /**
     * @param parsedEffectsJson the {@code Card.parsedEffects} JSON string
     *                          ({@code {attacks, abilities, trainerEffects}})
     * @return parsed abilities, or an empty list if absent, blank or malformed
     */
    public static List<AbilityData> parse(String parsedEffectsJson) {
        if (parsedEffectsJson == null || parsedEffectsJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = MAPPER.readTree(parsedEffectsJson);
            JsonNode abilities = root.get("abilities");
            if (abilities == null || abilities.isNull()) return Collections.emptyList();
            return MAPPER.convertValue(abilities, ABILITY_LIST_TYPE);
        } catch (Exception e) {
            System.err.println("Failed to parse abilities: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
