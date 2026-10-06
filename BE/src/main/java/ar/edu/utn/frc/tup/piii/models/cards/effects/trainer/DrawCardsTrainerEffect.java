package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DrawCardsTrainerEffect extends TrainerEffect {
    private int amount;
}
