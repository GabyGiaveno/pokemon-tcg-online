package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Continuous immunity to Special Conditions (e.g. Slurpuff's Sweet Veil: each of
 * your Pokémon with Fairy Energy attached can't be affected by Special Conditions).
 * Which Pokémon are protected is decided by the ability's {@link AbilityCondition}s.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ImmuneToConditionsEffect extends AttackEffect {
    private String target;
}
