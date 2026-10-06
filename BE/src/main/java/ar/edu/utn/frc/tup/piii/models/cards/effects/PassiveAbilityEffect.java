package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PassiveAbilityEffect extends AttackEffect {
    private String trigger;
    private AttackEffect effect;
    private boolean stackable;
    /** Activation conditions from the parsed JSON (IS_ACTIVE / HAS_ENERGY / HAS_CONDITION). */
    private List<AbilityCondition> conditions = new ArrayList<>();
}
