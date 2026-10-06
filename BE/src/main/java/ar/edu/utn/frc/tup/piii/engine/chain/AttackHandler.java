package ar.edu.utn.frc.tup.piii.engine.chain;

/**
 * Contract for every step in the 7-handler Chain of Responsibility attack pipeline.
 *
 * <p>Each handler receives the mutable {@link AttackContext}, reads what it needs, writes its
 * results back, and either continues the chain or signals cancellation via
 * {@link AttackContext#cancelAttack()}.
 *
 * <p>The default {@link #alwaysRun()} implementation returns {@code false} — all handlers
 * short-circuit when {@code attackCancelled == true} unless they override this method.
 * {@link ar.edu.utn.frc.tup.piii.engine.chain.handlers.PostDamageHandler} overrides it
 * to {@code true} so that KO checks always run even after a confusion self-hit cancels the attack.
 */
public interface AttackHandler {

    /**
     * Executes this pipeline step.
     *
     * @param context mutable state object shared by the entire pipeline
     */
    void handle(AttackContext context);

    /**
     * When {@code true}, {@link AttackResolutionChain} will invoke this handler even after the
     * pipeline has been cancelled via {@link AttackContext#cancelAttack()}.
     * Override and return {@code true} for any handler that must always run (e.g. KO checks).
     */
    default boolean alwaysRun() {
        return false;
    }
}
