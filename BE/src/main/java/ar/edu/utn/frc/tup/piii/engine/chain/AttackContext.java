package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context object passed between every step of the
 * 7-handler attack resolution pipeline.
 *
 * <p>Created once by {@link AttackResolutionChain} before the pipeline starts.
 * Each {@link AttackHandler} reads what it needs and writes its results back:
 * <ul>
 *   <li>Cancels the pipeline via {@link #cancelAttack()} if the attack fails or is blocked.</li>
 *   <li>Appends events to {@link #events} to be returned to the caller.</li>
 *   <li>{@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler}
 *       writes {@link #finalDamage} after calculation.</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttackContext {

    // ── Immutable inputs (set by AttackResolutionChain.buildContext) ──────────

    private BoardState board;
    private Long attackerPlayerId;
    private CardLookup cardLookup;

    private PlayerField attackerField;
    private PlayerField defenderField;

    private ActivePokemon attackerPokemon;
    private ActivePokemon defenderPokemon;

    private Card attackerCard;
    private Card defenderCard;

    /** The parsed attack being executed. */
    private AttackData attackData;

    // ── Mutable pipeline state ─────────────────────────────────────────────

    /**
     * When {@code true}, all remaining handlers skip execution.
     * Set by {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.ConfusionCheckHandler}
     * on a tails flip, or by
     * {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.EnergyValidationHandler}
     * on insufficient energy.
     */
    private boolean attackCancelled;

    /**
     * When {@code true}, {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler}
     * skips HP mutation and the DAMAGE_DEALT event. Post-damage effects (conditions, etc.) still fire.
     * Set by {@link ar.edu.utn.frc.tup.piii.engine.effects.logics.PreventDamageLogic}.
     */
    private boolean damageBlocked;

    /**
     * Accumulator for damage modifiers (+/-) applied before weakness/resistance.
     */
    private int damageModifiers;

    /**
     * Final damage value after all modifiers, weakness, and resistance have been applied.
     * Written by {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler}.
     */
    private int finalDamage;

    /** Ordered list of events accumulated during pipeline execution. */
    @Builder.Default
    private List<GameEvent> events = new ArrayList<>();

    // ── Convenience mutators ──────────────────────────────────────────────

    /** Cancels the attack pipeline — all subsequent handlers will short-circuit. */
    public void cancelAttack() {
        this.attackCancelled = true;
    }

    /** Appends one event to the accumulated list. */
    public void addEvent(GameEvent event) {
        this.events.add(event);
    }
}
