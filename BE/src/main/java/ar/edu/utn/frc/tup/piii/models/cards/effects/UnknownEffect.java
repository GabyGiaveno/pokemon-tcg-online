package ar.edu.utn.frc.tup.piii.models.cards.effects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Catch-all subtype for any effect {@code type} not registered in
 * {@link AttackEffect}'s {@code @JsonSubTypes}. Declared as the {@code defaultImpl}
 * of the polymorphic hierarchy so that an unknown — or not-yet-implemented — effect
 * type never aborts parsing of the whole card: it deserializes to this inert
 * placeholder instead of throwing {@code InvalidTypeIdException}.
 *
 * <p>Intentionally has NO {@link ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic}
 * registered in {@code EffectRegistry}, so the attack handlers skip it naturally
 * ({@code getLogic(...) == null}). The original {@code type} discriminator is captured
 * for diagnostics/logging; any other properties are ignored.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnknownEffect extends AttackEffect {

    /** The original {@code type} discriminator, captured for logging. */
    private String type;
}
