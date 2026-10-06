package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Continuous lock on playing Item cards (e.g. Trevenant's Forest's Curse:
 * while this Pokémon is Active, the opponent can't play Items).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RestrictItemsEffect extends AttackEffect {
    private String target;
}
