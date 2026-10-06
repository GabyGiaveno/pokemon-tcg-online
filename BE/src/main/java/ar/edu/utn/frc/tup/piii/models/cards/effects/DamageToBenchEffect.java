package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DamageToBenchEffect extends AttackEffect {
    private int amount;
    private int targetCount;
    private String target;
}
