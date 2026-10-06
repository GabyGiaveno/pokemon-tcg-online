package ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain;

/**
 * Contract for every step in the Trainer/Item/Stadium resolution Chain of Responsibility.
 * Mirror of {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler}.
 */
public interface TrainerHandler {

    void handle(TrainerContext context);

    /**
     * When {@code true}, the handler runs even if the chain was cancelled
     * (via {@link TrainerContext#cancel()}). Defaults to {@code false}.
     */
    default boolean alwaysRun() {
        return false;
    }
}
