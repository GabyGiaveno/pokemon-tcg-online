package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HealTrainerEffect extends TrainerEffect {
    private int amount;
    private String target; // e.g. "ACTIVE", "BENCH_1", "ALL"
}
