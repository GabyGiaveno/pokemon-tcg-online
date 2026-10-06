package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Discards the player's whole hand, then draws {@code amount} cards (e.g. Professor Sycamore). */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiscardHandDrawTrainerEffect extends TrainerEffect {
    private int amount;
}
