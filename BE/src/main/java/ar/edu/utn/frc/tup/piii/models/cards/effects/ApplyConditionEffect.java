package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApplyConditionEffect extends AttackEffect {
    private String condition; // e.g. "PARALYZED", "POISONED"
    private String target; // e.g. "DEFENDER", "SELF"
}
