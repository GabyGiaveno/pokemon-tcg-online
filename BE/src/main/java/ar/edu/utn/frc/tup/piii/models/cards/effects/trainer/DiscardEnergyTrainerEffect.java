package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Discards {@code amount} energy from a target Pokémon.
 * {@code target}: {@code "OPPONENT_ACTIVE"} (Team Flare Grunt). {@code amount < 0} = all.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiscardEnergyTrainerEffect extends TrainerEffect {
    private int amount;
    private String target;
}
