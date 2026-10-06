package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecycleTrainerEffect extends TrainerEffect {
    /** Card filter applied to the source zone; currently only "POKEMON" is supported. */
    private String filter;
    /** Number of hand cards to auto-discard before the player selects a target. */
    private int discardCost;
    /** Zone to retrieve from; currently only "DISCARD" is supported. */
    private String sourceZone;
    /** Destination zone; currently only "BENCH" is supported. */
    private String destination;
}
