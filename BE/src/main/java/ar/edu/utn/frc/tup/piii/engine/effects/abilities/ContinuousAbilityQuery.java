package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ImmuneToConditionsEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PassiveAbilityEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ReduceDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.RestrictItemsEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.List;

/**
 * Answers "is there a continuous ability in play that modifies this rule?" (design D9).
 *
 * <p>Continuous abilities don't execute — they are consulted by the subsystems they alter:
 * <ul>
 *   <li>{@code RESTRICT_ITEMS} (Forest's Curse) → {@code MainPhaseState.handlePlayItem}.</li>
 *   <li>{@code REDUCE_DAMAGE} (Fur Coat) → {@code DamageApplicationHandler}, after W&R.</li>
 *   <li>{@code IMMUNE_TO_CONDITIONS} (Sweet Veil) → condition application.</li>
 * </ul>
 * Unresolvable cards are treated as having no abilities (fail-open for lookups, fail-closed
 * for conditions — consistent with {@link AbilityConditionEvaluator}).
 */
public final class ContinuousAbilityQuery {

    private ContinuousAbilityQuery() {}

    /**
     * True when {@code field}'s ACTIVE Pokémon has an Item-lock ability whose conditions hold
     * (Forest's Curse only works from the Active slot — its IS_ACTIVE condition enforces it).
     */
    public static boolean itemsLockedBy(PlayerField field, CardLookup cardLookup) {
        if (field == null || field.getActivePokemon() == null) return false;
        ActivePokemon active = field.getActivePokemon();
        for (PassiveAbilityEffect passive : passivesOf(active.getCardId(), cardLookup)) {
            if (passive.getEffect() instanceof RestrictItemsEffect
                    && AbilityConditionEvaluator.allHold(
                            passive.getConditions(), true, active, cardLookup)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Total flat damage reduction granted by the defender's own abilities
     * (applied after Weakness/Resistance and modifiers, per Fur Coat's text).
     */
    public static int damageReductionFor(ActivePokemon defender, boolean defenderIsActive,
                                         CardLookup cardLookup) {
        if (defender == null) return 0;
        int reduction = 0;
        for (PassiveAbilityEffect passive : passivesOf(defender.getCardId(), cardLookup)) {
            if (passive.getEffect() instanceof ReduceDamageEffect reduce
                    && AbilityConditionEvaluator.allHold(
                            passive.getConditions(), defenderIsActive, defender, cardLookup)) {
                reduction += reduce.getAmount();
            }
        }
        return reduction;
    }

    /**
     * True when {@code target} is protected from Special Conditions by an immunity ability
     * anywhere on {@code ownerField} (Active or Bench — Sweet Veil protects from the Bench too).
     * The ability's conditions (e.g. HAS_ENERGY FAIRY) are evaluated against the PROTECTED target.
     */
    public static boolean isImmuneToConditions(ActivePokemon target, PlayerField ownerField,
                                               CardLookup cardLookup) {
        if (target == null || ownerField == null) return false;

        if (hasImmunityFor(target, ownerField.getActivePokemon() != null
                ? ownerField.getActivePokemon().getCardId() : null, ownerField, cardLookup)) {
            return true;
        }
        if (ownerField.getBench() != null) {
            for (BenchPokemon benched : ownerField.getBench()) {
                if (hasImmunityFor(target, benched.getCardId(), ownerField, cardLookup)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static boolean hasImmunityFor(ActivePokemon target, String sourceCardId,
                                          PlayerField ownerField, CardLookup cardLookup) {
        if (sourceCardId == null) return false;
        boolean sourceIsActive = ownerField.getActivePokemon() != null
                && sourceCardId.equals(ownerField.getActivePokemon().getCardId());
        for (PassiveAbilityEffect passive : passivesOf(sourceCardId, cardLookup)) {
            if (passive.getEffect() instanceof ImmuneToConditionsEffect
                    && AbilityConditionEvaluator.allHold(
                            passive.getConditions(), sourceIsActive, target, cardLookup)) {
                return true;
            }
        }
        return false;
    }

    /** All {@link PassiveAbilityEffect}s of the card, or empty when unresolvable/no abilities. */
    private static List<PassiveAbilityEffect> passivesOf(String cardId, CardLookup cardLookup) {
        if (cardId == null) return List.of();
        Card card;
        try {
            card = cardLookup.findById(cardId);
        } catch (RuntimeException e) {
            return List.of();
        }
        if (card == null) return List.of();
        return AbilityParser.parse(card.getParsedEffects()).stream()
                .flatMap(a -> a.passiveEffects().stream())
                .toList();
    }
}
