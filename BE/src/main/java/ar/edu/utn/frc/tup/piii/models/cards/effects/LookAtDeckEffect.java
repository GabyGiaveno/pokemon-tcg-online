package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LookAtDeckEffect extends AttackEffect {
    private int amount;
    private String target;
    private boolean reorder;
}
