package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MultiplierDamageEffect extends AttackEffect {
    private int amountPerUnit;
    private String unitType;
    private Integer flips; // Optional, only if unitType is COIN_FLIPS
}
