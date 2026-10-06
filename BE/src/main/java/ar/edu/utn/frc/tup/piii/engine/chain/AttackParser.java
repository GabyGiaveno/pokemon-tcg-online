package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PassiveAbilityEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.UnknownEffect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the {@code attacks} JSONB column (stored as a plain String in {@code Card})
 * into a typed list of {@link AttackData} objects.
 *
 * <p>The ObjectMapper is created once and reused — it is thread-safe.
 */
public class AttackParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<AttackData>> ATTACK_LIST_TYPE =
            new TypeReference<List<AttackData>>() {};

    private AttackParser() {}

    /**
     * Parses the raw JSON string from {@code Card.attacks}.
     *
     * @param attacksJson the raw JSON string (may be null or blank)
     * @return parsed list, or an empty list if the input is null, blank, or malformed
     */
    public static List<AttackData> parse(String attacksJson) {
        if (attacksJson == null || attacksJson.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(attacksJson, ATTACK_LIST_TYPE);
        } catch (Exception e) {
            // Log-worthy in production; for now return empty to avoid crashing the engine
            return Collections.emptyList();
        }
    }

    /**
     * Parses the raw attacks and merges them with the structured parsedEffects logic.
     *
     * @param attacksJson       the raw attacks JSON
     * @param parsedEffectsJson the parsed_effects JSON column from the database
     * @return list of AttackData enriched with parsedEffects
     */
    public static List<AttackData> parse(String attacksJson, String parsedEffectsJson) {
        List<AttackData> attacks = parse(attacksJson);
        if (attacks.isEmpty() || parsedEffectsJson == null || parsedEffectsJson.isBlank()) {
            return attacks;
        }

        Set<String> unknownTypes = new HashSet<>();
        try {
            java.util.Map<String, Object> parsedData = MAPPER.readValue(parsedEffectsJson, new TypeReference<>() {});
            
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> parsedAttacks = 
                    (List<java.util.Map<String, Object>>) parsedData.getOrDefault("attacks", Collections.emptyList());

            for (AttackData attack : attacks) {
                for (java.util.Map<String, Object> pa : parsedAttacks) {
                    if (attack.getName().equals(pa.get("name"))) {
                        Object effectsRaw = pa.get("parsedEffects");
                        if (effectsRaw != null) {
                            List<ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect> effects = 
                                    MAPPER.convertValue(effectsRaw, new TypeReference<>() {});
                            attack.setParsedEffects(effects);
                            collectUnknownTypes(effects, unknownTypes);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // If merge fails, we log and return the raw attacks safely
            System.err.println("Failed to merge parsed effects: " + e.getMessage());
        }

        if (!unknownTypes.isEmpty()) {
            System.err.println("[AttackParser] Unmapped effect type(s) skipped (parsed as inert UnknownEffect): "
                    + unknownTypes);
        }

        return attacks;
    }

    /**
     * Recursively collects the original {@code type} of every {@link UnknownEffect} found,
     * including those nested inside {@code COIN_FLIP} branches and {@code PASSIVE_ABILITY}
     * effects, so they are logged instead of failing silently.
     */
    private static void collectUnknownTypes(List<AttackEffect> effects, Set<String> sink) {
        if (effects == null) return;
        for (AttackEffect e : effects) {
            if (e instanceof UnknownEffect ue) {
                sink.add(ue.getType() != null ? ue.getType() : "(unknown)");
            } else if (e instanceof CoinFlipEffect cf) {
                collectUnknownTypes(cf.getIfHeads(), sink);
                collectUnknownTypes(cf.getIfTails(), sink);
            } else if (e instanceof PassiveAbilityEffect pa && pa.getEffect() instanceof UnknownEffect ue) {
                sink.add(ue.getType() != null ? ue.getType() : "(unknown)");
            }
        }
    }
}
