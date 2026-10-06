package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShufflePokemonIntoDeckTrainerEffect extends TrainerEffect {
    /** Zone the Pokémon is chosen from; currently only "BENCH" is supported. */
    private String source;
}
