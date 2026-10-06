package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Shuffles a hand into its deck and draws {@code drawAmount} cards.
 * {@code target}: {@code "SELF"} (Shauna) or {@code "OPPONENT"} (Red Card).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShuffleHandTrainerEffect extends TrainerEffect {
    private String target;
    private int drawAmount;
}
