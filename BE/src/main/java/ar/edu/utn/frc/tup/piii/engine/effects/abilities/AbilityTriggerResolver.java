package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageCountersEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PassiveAbilityEffect;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Resolves triggered abilities at the two reaction points of the attack pipeline
 * (design D8): {@code ON_ATTACK_RECEIVED} (e.g. Chesnaught's Spiky Shield) and
 * {@code ON_ALLY_KNOCKOUT} (e.g. Voltorb's Destiny Burst).
 *
 * <p>Invoked by {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.PostDamageHandler}
 * AFTER the attack's own post-damage effects and BEFORE KO processing, so:
 * "even if this Pokémon is Knocked Out" holds (the defender is still in its slot), and
 * recoil that knocks out the attacker is caught by the existing double KO check.
 */
public class AbilityTriggerResolver {

    private final Random random;

    public AbilityTriggerResolver() {
        this.random = new Random();
    }

    /** Test constructor: inject a deterministic {@link Random}. */
    public AbilityTriggerResolver(Random random) {
        this.random = random;
    }

    /**
     * Fires the defender's {@code ON_ATTACK_RECEIVED} abilities (requires landed damage)
     * and, if the defender was left at 0 HP by this attack, its {@code ON_ALLY_KNOCKOUT}
     * abilities. Mutates HP on the attacker for recoil-style effects.
     */
    public void resolveDefenderTriggers(AttackContext ctx) {
        if (ctx.isAttackCancelled() || ctx.getFinalDamage() <= 0) return;
        if (ctx.getDefenderCard() == null || ctx.getDefenderPokemon() == null) return;

        List<AbilityData> abilities = AbilityParser.parse(ctx.getDefenderCard().getParsedEffects());
        if (abilities.isEmpty()) return;

        boolean defenderIsActive = AbilityConditionEvaluator
                .isActiveOf(ctx.getDefenderField(), ctx.getDefenderPokemon());
        boolean defenderKnockedOut = ctx.getDefenderPokemon().getCurrentHp() <= 0;

        for (AbilityData ability : abilities) {
            for (PassiveAbilityEffect passive : ability.passiveEffects()) {
                String trigger = passive.getTrigger() != null ? passive.getTrigger() : "";
                boolean fires = switch (trigger) {
                    case "ON_ATTACK_RECEIVED" -> true;
                    case "ON_ALLY_KNOCKOUT" -> defenderKnockedOut;
                    default -> false;
                };
                if (!fires) continue;

                boolean conditionsHold = AbilityConditionEvaluator.allHold(
                        passive.getConditions(), defenderIsActive,
                        ctx.getDefenderPokemon(), ctx.getCardLookup());
                if (!conditionsHold) continue;

                applyTriggeredEffect(ctx, ability.getName(), passive.getEffect());
            }
        }
    }

    // ── Effect dispatch ───────────────────────────────────────────────────────

    private void applyTriggeredEffect(AttackContext ctx, String abilityName, AttackEffect effect) {
        if (effect instanceof DamageCountersEffect counters) {
            applyDamageCounters(ctx, abilityName, counters);
        } else if (effect instanceof CoinFlipDamageEffect flip) {
            boolean heads = random.nextBoolean();
            ctx.addEvent(GameEvent.of(GameEventType.COIN_FLIPPED,
                    abilityName + ": flipped " + (heads ? "heads" : "tails") + ".",
                    Map.of("ability", abilityName, "flip", heads ? "HEADS" : "TAILS")));
            if (heads && flip.getHeadsCondition() != null) {
                applyTriggeredEffect(ctx, abilityName, flip.getHeadsCondition());
            }
        }
        // ReduceDamage / RestrictItems / ImmuneToConditions are continuous —
        // resolved by ContinuousAbilityQuery at their own hook points, not here.
    }

    private void applyDamageCounters(AttackContext ctx, String abilityName, DamageCountersEffect counters) {
        if (!"ATTACKER".equalsIgnoreCase(counters.getTarget())) return; // tier 1: recoil only
        int damage = counters.getAmount() * 10;
        int newHp = Math.max(0, ctx.getAttackerPokemon().getCurrentHp() - damage);
        ctx.getAttackerPokemon().setCurrentHp(newHp);
        ctx.addEvent(GameEvent.of(GameEventType.DAMAGE_DEALT,
                abilityName + " dealt " + damage + " damage to "
                        + ctx.getAttackerPokemon().getCardId() + ". HP remaining: " + newHp + ".",
                Map.of("ability", abilityName,
                        "cardId", ctx.getAttackerPokemon().getCardId(),
                        "damage", damage,
                        "hpRemaining", newHp,
                        "source", "ABILITY")));
    }
}
