package ar.edu.utn.frc.tup.piii.engine.chain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;

/**
 * Represents one attack entry from the pokemontcg.io JSON payload stored in {@code Card.attacks}.
 *
 * <p>Example raw JSON for one attack:
 * <pre>
 * {
 *   "name": "Flamethrower",
 *   "cost": ["Fire", "Colorless", "Colorless"],
 *   "convertedEnergyCost": 3,
 *   "damage": "120",
 *   "text": "Discard an Energy attached to this Pokémon."
 * }
 * </pre>
 *
 * <p>{@code damage} can be a bare number ("120"), a multiply marker ("10×"),
 * a plus marker ("30+"), or empty/null for effect-only attacks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttackData {

    private String name;

    /** Energy types required — e.g. {@code ["Fire","Colorless","Colorless"]}. */
    private List<String> cost;

    /** Total number of energies required (sum of cost list). */
    private int convertedEnergyCost;

    /** Raw damage string from the API — may contain modifiers like "×" or "+". */
    private String damage;

    /** Human-readable effect text. Null/blank for damage-only attacks. */
    private String text;

    /** Parsed effect objects mapped from xy1_parsed.json using Jackson polymorphism */
    private List<AttackEffect> parsedEffects;

    // -------------------------------------------------------------------------
    // Derived helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the numeric base damage, ignoring any modifier suffix.
     * <ul>
     *   <li>"120"  → 120</li>
     *   <li>"10×"  → 10 (base; multiplier determined by attack text)</li>
     *   <li>"30+"  → 30 (base; bonus determined by attack text)</li>
     *   <li>""     → 0</li>
     * </ul>
     */
    public int getBaseDamage() {
        if (damage == null || damage.isBlank()) return 0;
        StringBuilder sb = new StringBuilder();
        for (char c : damage.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else break;
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }

    /** Returns {@code true} if this attack has a non-empty effect text. */
    public boolean hasEffect() {
        return text != null && !text.isBlank();
    }

    /** Returns {@code true} if this attack has parsed effects. */
    public boolean hasParsedEffects() {
        return parsedEffects != null && !parsedEffects.isEmpty();
    }

    /** Returns {@code true} if the damage value contains a modifier (×, +, −). */
    public boolean hasDamageModifier() {
        return damage != null && (damage.contains("×") || damage.contains("+") || damage.contains("-"));
    }

    /** Safe accessor — never returns null. */
    public List<String> getCost() {
        return cost != null ? cost : Collections.emptyList();
    }
}
