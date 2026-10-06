package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** Flips a coin and runs the {@code ifHeads} or {@code ifTails} sub-effects (e.g. Roller Skates). */
@Data
@EqualsAndHashCode(callSuper = true)
public class CoinFlipTrainerEffect extends TrainerEffect {
    private List<TrainerEffect> ifHeads;
    private List<TrainerEffect> ifTails;
}
