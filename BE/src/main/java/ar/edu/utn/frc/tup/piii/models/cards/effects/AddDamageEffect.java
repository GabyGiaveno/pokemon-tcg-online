package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddDamageEffect extends AttackEffect {
    private int amount;
    private String condition;
    private String target;
    private Boolean optional;
}
