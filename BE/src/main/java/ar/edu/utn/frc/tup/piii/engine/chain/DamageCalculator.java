package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.entities.Card;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Pure-function damage calculator for the attack resolution pipeline.
 *
 * <p>Applies the standard XY era formula in strict order:
 * <ol>
 *   <li>Base damage from the attack.</li>
 *   <li>Weakness — multiplies damage by 2 when the attacker's type matches the defender's weakness.</li>
 *   <li>Resistance — subtracts 20 when the attacker's type matches the defender's resistance.</li>
 * </ol>
 *
 * <p>Neither Weakness nor Resistance can make the result negative; the minimum is 0.
 *
 * <p>Weaknesses and resistances are stored in the {@code Card} entity as JSONB strings,
 * e.g. {@code [{"type":"Water","value":"×2"}]}, and are parsed once per call using Jackson.
 */
public class DamageCalculator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<TypeModifier>> TYPE_MODIFIER_LIST = new TypeReference<>() {};

    private DamageCalculator() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Calculates the final damage to be applied to the defending Pokémon.
     *
     * @param baseDamage   raw base damage extracted from the attack (before modifiers)
     * @param attackerCard JPA entity of the attacking Pokémon card (provides its primary type)
     * @param defenderCard JPA entity of the defending Pokémon card (provides weaknesses/resistances)
     * @return final damage value, never negative
     */
    public static int calculate(int baseDamage, Card attackerCard, Card defenderCard) {
        return calculate(baseDamage, attackerCard, defenderCard, false);
    }

    public static int calculate(int baseDamage, Card attackerCard, Card defenderCard, boolean suppressWeakness) {
        if (baseDamage <= 0) return 0;

        String attackerType = getFirstType(attackerCard);
        int damage = baseDamage;

        if (!suppressWeakness) {
            damage = applyWeakness(damage, defenderCard, attackerType);
        }
        damage = applyResistance(damage, defenderCard, attackerType);

        damage = (int) (Math.round(damage / 10.0) * 10);
        return Math.max(0, damage);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Doubles damage when the attacker type matches a ×2 weakness entry.
     * Also handles older-set "+N" weakness format just in case.
     */
    private static int applyWeakness(int damage, Card defenderCard, String attackerType) {
        if (attackerType.isBlank()) return damage;
        for (TypeModifier w : parseTypeModifiers(defenderCard.getWeaknesses())) {
            if (matches(w.getType(), attackerType)) {
                String val = w.getValue() != null ? w.getValue().trim() : "";
                if (val.contains("×") || val.toLowerCase().contains("x")) {
                    String numStr = val.replaceAll("[^0-9]", "");
                    int multiplier = numStr.isEmpty() ? 2 : Integer.parseInt(numStr);
                    return damage * multiplier;
                }
                if (val.startsWith("+")) {
                    return damage + Integer.parseInt(val.substring(1).trim());
                }
                // Default for any unrecognised weakness format: ×2
                return damage * 2;
            }
        }
        return damage;
    }

    /**
     * Subtracts 20 when the attacker type matches a resistance entry.
     * The standard modern format is {@code "-20"}.
     */
    private static int applyResistance(int damage, Card defenderCard, String attackerType) {
        if (attackerType.isBlank()) return damage;
        for (TypeModifier r : parseTypeModifiers(defenderCard.getResistances())) {
            if (matches(r.getType(), attackerType)) {
                String val = r.getValue() != null ? r.getValue().trim() : "";
                if (val.startsWith("-")) {
                    int reduction = Integer.parseInt(val.substring(1).trim());
                    return damage - reduction;
                }
            }
        }
        return damage;
    }

    private static String getFirstType(Card card) {
        if (card == null || card.getTypes() == null || card.getTypes().isEmpty()) return "";
        return card.getTypes().get(0);
    }

    private static boolean matches(String modifierType, String attackerType) {
        return modifierType != null && modifierType.equalsIgnoreCase(attackerType);
    }

    private static List<TypeModifier> parseTypeModifiers(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, TYPE_MODIFIER_LIST);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ── Inner model ───────────────────────────────────────────────────────────

    /**
     * Maps one entry of the {@code weaknesses} / {@code resistances} JSONB array.
     * Example entry: {@code {"type": "Water", "value": "×2"}}
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TypeModifier {
        private String type;
        private String value;
    }
}
