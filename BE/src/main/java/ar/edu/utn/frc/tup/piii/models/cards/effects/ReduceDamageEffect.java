package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Continuous damage reduction granted by an ability (e.g. Furfrou's Fur Coat:
 * attacks deal 20 less damage to this Pokémon, after Weakness and Resistance).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReduceDamageEffect extends AttackEffect {
    private int amount;
    private String target;
}
