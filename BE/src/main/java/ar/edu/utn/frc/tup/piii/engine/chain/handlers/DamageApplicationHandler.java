package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.passive.PassiveEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.passive.StadiumEffect;
import ar.edu.utn.frc.tup.piii.engine.passive.ToolEffect;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;

import java.util.Map;

/**
 * Handler 6 — Damage application.
 *
 * <p>Calculates and applies the final damage to the defending Pokémon:
 * <ol>
 *   <li>Retrieves the base damage from the parsed {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackData}.</li>
 *   <li>Delegates to {@link DamageCalculator} to apply Weakness (×2) and Resistance (−20).</li>
 *   <li>Subtracts the final damage from {@code defenderPokemon.currentHp} (floor: 0).</li>
 *   <li>Writes the result to {@link AttackContext#setFinalDamage(int)} for downstream handlers.</li>
 * </ol>
 *
 * <p>If the attack has no base damage (effect-only attacks like "Hypnosis"), the method
 * short-circuits with zero damage and no event.
 */
public class DamageApplicationHandler implements AttackHandler {

    private final PassiveEffectRegistry registry;

    public DamageApplicationHandler() {
        this.registry = PassiveEffectRegistry.getInstance();
    }

    public DamageApplicationHandler(PassiveEffectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(AttackContext ctx) {
        int base = ctx.getAttackData().getBaseDamage();

        if (ctx.getDefenderPokemon().isDamageProtected()) {
            ctx.getDefenderPokemon().setDamageProtected(false); // consume: protection lasts one hit
            ctx.setFinalDamage(0);
            ctx.addEvent(GameEvent.of(
                    GameEventType.DAMAGE_PREVENTED,
                    ctx.getDefenderPokemon().getCardId() + " was protected — no damage taken."));
            return; // post-damage effects (conditions) still run in PostDamageHandler
        }

        if (base == 0 && ctx.getDamageModifiers() == 0) {
            ctx.setFinalDamage(0);
            return; // effect-only attack — no damage event
        }

        // Step 1: apply flat damage modifiers (ADD_DAMAGE) before weakness/resistance
        int modifiedBase = base + ctx.getDamageModifiers();

        // Step 1b: Muscle Band — +20 outgoing before weakness/resistance
        AttachedCard attackerTool = ctx.getAttackerPokemon().getTool();
        if (attackerTool != null) {
            ToolEffect toolEffect = registry.getToolEffect(attackerTool.getCardId());
            if (toolEffect != null) {
                modifiedBase += toolEffect.modifyOutgoingDamage(ctx);
            }
        }

        // Step 2: Shadow Circle — suppress weakness if defender has Darkness energy
        boolean suppressWeakness = false;
        String stadiumId = ctx.getBoard() != null ? ctx.getBoard().getActiveStadiumCardId() : null;
        if (stadiumId != null) {
            StadiumEffect stadiumEffect = registry.getStadiumEffect(stadiumId);
            if (stadiumEffect != null) {
                suppressWeakness = stadiumEffect.suppressesWeakness(ctx.getDefenderPokemon(), ctx.getCardLookup());
            }
        }

        // Step 2b: apply weakness (×2) and resistance (−20) to the modified base
        int finalDamage = DamageCalculator.calculate(modifiedBase, ctx.getAttackerCard(), ctx.getDefenderCard(), suppressWeakness);

        // Step 3: continuous damage-reduction abilities of the defender (e.g. Fur Coat:
        // "reduced by 20, after applying Weakness and Resistance") — design D9
        boolean defenderIsActive = ar.edu.utn.frc.tup.piii.engine.effects.abilities
                .AbilityConditionEvaluator.isActiveOf(ctx.getDefenderField(), ctx.getDefenderPokemon());
        finalDamage -= ar.edu.utn.frc.tup.piii.engine.effects.abilities.ContinuousAbilityQuery
                .damageReductionFor(ctx.getDefenderPokemon(), defenderIsActive, ctx.getCardLookup());

        // Step 3b: Hard Charm — -20 incoming after weakness/resistance
        AttachedCard defenderTool = ctx.getDefenderPokemon().getTool();
        if (defenderTool != null) {
            ToolEffect toolEffect = registry.getToolEffect(defenderTool.getCardId());
            if (toolEffect != null) {
                finalDamage += toolEffect.modifyIncomingDamage(ctx);
            }
        }

        // Step 4: ensure non-negative
        finalDamage = Math.max(0, finalDamage);

        ctx.setFinalDamage(finalDamage);

        int newHp = Math.max(0, ctx.getDefenderPokemon().getCurrentHp() - finalDamage);
        ctx.getDefenderPokemon().setCurrentHp(newHp);

        ctx.addEvent(GameEvent.of(
                GameEventType.DAMAGE_DEALT,
                ctx.getAttackerPokemon().getCardId()
                        + " dealt " + finalDamage + " damage to "
                        + ctx.getDefenderPokemon().getCardId()
                        + ". HP remaining: " + newHp + ".",
                Map.of("attacker", ctx.getAttackerPokemon().getCardId(),
                        "defender", ctx.getDefenderPokemon().getCardId(),
                        "baseDamage", base,
                        "finalDamage", finalDamage,
                        "hpRemaining", newHp)));
    }
}
