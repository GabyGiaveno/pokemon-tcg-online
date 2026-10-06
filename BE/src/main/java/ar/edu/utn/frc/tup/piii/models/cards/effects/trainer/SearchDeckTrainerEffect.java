package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchDeckTrainerEffect extends TrainerEffect {
    /** "POKEMON", "POKEMON_EVOLUTION", "ENERGY_BASIC", or "ANY" */
    private String filter;
    /** Number of top deck cards to reveal; -1 means the entire deck. */
    private int lookAtCount;
    /** Maximum number of matching cards the player may take. */
    private int takeCount;
    /** "HAND" or "BENCH" */
    private String destination;
}
