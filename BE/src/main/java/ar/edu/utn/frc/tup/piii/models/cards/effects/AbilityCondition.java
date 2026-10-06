package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One activation condition of a {@link PassiveAbilityEffect}, as produced by the
 * bigpickle parsing scripts in {@code xy1_parsed.json}.
 *
 * <p>Vocabulary in XY1: {@code IS_ACTIVE} (source must be its owner's Active),
 * {@code HAS_ENERGY} (target has energy of the given color attached),
 * {@code HAS_CONDITION} (source has the given special condition).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbilityCondition {
    private String type;
    private String target;
    private String value;
}
