package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectRegistry;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;

import java.util.List;
import java.util.Map;

/**
 * Handler 7 — Post-damage effects and KO detection.
 *
 * <p>This is the final step of the attack pipeline. It has three responsibilities:
 * <ol>
 *   <li><b>Condition application</b> — if the attack was not cancelled, parses
 *       {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackData#getText()} for condition
 *       keywords and applies them to the defending Pokémon.</li>
 *   <li><b>Defender KO check</b> — calls {@link KnockoutProcessor} for the defender.</li>
 *   <li><b>Attacker KO check</b> — calls {@link KnockoutProcessor} for the attacker
 *       (necessary when a confusion tails self-hit reduces its HP to 0).</li>
 * </ol>
 *
 * <p><b>Why {@code alwaysRun() == true}:</b>
 * When {@link ConfusionCheckHandler} lands tails, it applies 30 self-damage to the attacker
 * and calls {@link AttackContext#cancelAttack()}, stopping all subsequent handlers.
 * Without {@code alwaysRun()}, a potential self-KO from confusion would never be detected.
 * Overriding this method guarantees that KO processing always happens regardless of cancellation.
 *
 * <p><b>Condition detection (keyword-based):</b>
 * The pokemontcg.io API returns only a human-readable text string for each attack effect.
 * This handler performs substring matching for standard phrasing. For full accuracy across
 * all XY-era attacks, the team should implement an {@code AttackEffectRegistry} that maps
 * attack names/card IDs to typed {@code AttackEffect} lambdas (same pattern as
 * {@code TrainerEffectRegistry}).
 */
public class PostDamageHandler implements AttackHandler {

    private final KnockoutProcessor knockoutProcessor;
    private final ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityTriggerResolver abilityTriggerResolver;

    public PostDamageHandler(KnockoutProcessor knockoutProcessor) {
        this(knockoutProcessor,
                new ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityTriggerResolver());
    }

    /** Constructor for the chain / tests: inject the trigger resolver (deterministic Random). */
    public PostDamageHandler(KnockoutProcessor knockoutProcessor,
                             ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityTriggerResolver abilityTriggerResolver) {
        this.knockoutProcessor = knockoutProcessor;
        this.abilityTriggerResolver = abilityTriggerResolver;
    }

    /**
     * Always runs — ensures KO checks happen even after a cancelled attack (confusion tails).
     */
    @Override
    public boolean alwaysRun() {
        return true;
    }

    @Override
    public void handle(AttackContext ctx) {
        // Condition application only makes sense for a successful, non-cancelled attack
        if (!ctx.isAttackCancelled()) {
            executePostDamageEffects(ctx);
        }

        // Triggered abilities of the defender (ON_ATTACK_RECEIVED / ON_ALLY_KNOCKOUT) fire
        // BEFORE KO processing: "even if Knocked Out" holds, and recoil that kills the
        // attacker is caught by the double KO check below (design D8).
        abilityTriggerResolver.resolveDefenderTriggers(ctx);

        // KO checks must always run — self-damage from confusion can kill the attacker
        processKo(ctx.getDefenderPokemon(),
                ctx.getDefenderField(),
                ctx.getAttackerField(),
                ctx);

        processKo(ctx.getAttackerPokemon(),
                ctx.getAttackerField(),
                ctx.getDefenderField(),
                ctx);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void processKo(ActivePokemon pokemon,
                           ar.edu.utn.frc.tup.piii.models.game.PlayerField ownerField,
                           ar.edu.utn.frc.tup.piii.models.game.PlayerField prizeTakerField,
                           AttackContext ctx) {
        List<GameEvent> koEvents = knockoutProcessor.processIfKnockedOut(
                pokemon, ownerField, prizeTakerField, ctx.getBoard(), ctx.getCardLookup());
        koEvents.forEach(ctx::addEvent);
    }

    private void executePostDamageEffects(AttackContext ctx) {
        if (!ctx.getAttackData().hasParsedEffects()) return;

        EffectRegistry registry = EffectRegistry.getInstance();
        for (AttackEffect effect : ctx.getAttackData().getParsedEffects()) {
            // Recoil damage: ADD_DAMAGE with target SELF applies fixed damage to the attacker,
            // bypassing weakness/resistance (same as confusion self-hit).
            if (effect instanceof AddDamageEffect ade && "SELF".equalsIgnoreCase(ade.getTarget())) {
                int newHp = Math.max(0, ctx.getAttackerPokemon().getCurrentHp() - ade.getAmount());
                ctx.getAttackerPokemon().setCurrentHp(newHp);
                ctx.addEvent(GameEvent.of(
                        GameEventType.DAMAGE_DEALT,
                        ctx.getAttackerPokemon().getCardId() + " took " + ade.getAmount()
                                + " recoil damage. HP remaining: " + newHp + ".",
                        Map.of("attacker", ctx.getAttackerPokemon().getCardId(),
                                "recoilDamage", ade.getAmount(),
                                "hpRemaining", newHp)));
                continue;
            }

            @SuppressWarnings("unchecked")
            EffectLogic<AttackEffect> logic = (EffectLogic<AttackEffect>) registry.getLogic(effect.getClass());

            if (logic != null && logic.isPostDamage()) {
                logic.execute(effect, ctx);
            }
        }
    }
}
