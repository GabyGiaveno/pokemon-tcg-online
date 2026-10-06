package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AbilityCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.List;

/**
 * Evaluates the {@link AbilityCondition}s of a parsed ability against the live board.
 *
 * <p>XY1 vocabulary (from the bigpickle scripts):
 * <ul>
 *   <li>{@code IS_ACTIVE} — the ability's source Pokémon is its owner's Active.</li>
 *   <li>{@code HAS_ENERGY} — the evaluated Pokémon has an energy of the given color attached
 *       (color resolved through {@link CardLookup}).</li>
 *   <li>{@code HAS_CONDITION} — the source has the given special condition.</li>
 * </ul>
 * Unknown condition types evaluate to {@code false} (fail-closed: an ability whose conditions
 * the engine cannot understand must not fire).
 */
public final class AbilityConditionEvaluator {

    private AbilityConditionEvaluator() {}

    /**
     * @param conditions   conditions to satisfy (null/empty → satisfied)
     * @param sourceIsActive whether the ability's source Pokémon is its owner's Active slot
     * @param evaluated    the Pokémon the conditions are checked against (source, or the
     *                     protected target for HAS_ENERGY-style conditions)
     * @param cardLookup   bridge to resolve attached energy colors
     * @return true when every condition holds
     */
    public static boolean allHold(List<AbilityCondition> conditions,
                                  boolean sourceIsActive,
                                  ActivePokemon evaluated,
                                  CardLookup cardLookup) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (AbilityCondition condition : conditions) {
            if (!holds(condition, sourceIsActive, evaluated, cardLookup)) {
                return false;
            }
        }
        return true;
    }

    private static boolean holds(AbilityCondition condition,
                                 boolean sourceIsActive,
                                 ActivePokemon evaluated,
                                 CardLookup cardLookup) {
        String type = condition.getType() != null ? condition.getType().toUpperCase() : "";
        return switch (type) {
            case "IS_ACTIVE" -> sourceIsActive;
            case "HAS_ENERGY" -> hasEnergyOfColor(evaluated, condition.getValue(), cardLookup);
            case "HAS_CONDITION" -> hasSpecialCondition(evaluated, condition.getValue());
            default -> false; // fail-closed
        };
    }

    /** True when the Pokémon has at least one attached energy whose card type matches the color. */
    public static boolean hasEnergyOfColor(ActivePokemon pokemon, String color, CardLookup cardLookup) {
        if (pokemon == null || color == null || pokemon.getAttachedEnergies() == null) return false;
        for (AttachedCard attached : pokemon.getAttachedEnergies()) {
            try {
                Card energyCard = cardLookup.findById(attached.getCardId());
                if (energyCard != null && energyCard.getTypes() != null
                        && energyCard.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(color))) {
                    return true;
                }
            } catch (RuntimeException e) {
                // unresolvable card → cannot prove the color, keep scanning
            }
        }
        return false;
    }

    private static boolean hasSpecialCondition(ActivePokemon pokemon, String value) {
        if (pokemon == null || value == null) return false;
        return switch (value.toUpperCase()) {
            case "POISONED" -> pokemon.isPoisoned();
            case "BURNED" -> pokemon.isBurned();
            default -> pokemon.getCondition() != null
                    && pokemon.getCondition().name().equalsIgnoreCase(value);
        };
    }

    /** Helper: whether {@code pokemon} occupies the Active slot of {@code ownerField}. */
    public static boolean isActiveOf(PlayerField ownerField, ActivePokemon pokemon) {
        return ownerField != null && ownerField.getActivePokemon() == pokemon;
    }
}
