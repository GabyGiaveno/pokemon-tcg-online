package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Catch-all subtype for any trainer-effect {@code type} not registered in
 * {@link TrainerEffect}'s {@code @JsonSubTypes}. Declared as {@code defaultImpl} so an unknown or
 * not-yet-implemented trainer effect never aborts parsing: it deserializes to this inert placeholder.
 *
 * <p>Has NO {@link ar.edu.utn.frc.tup.piii.engine.effects.trainers.TrainerEffectLogic} registered,
 * so {@code TrainerEffectRegistry.getLogic(...)} returns {@code null} and it is skipped naturally.
 * Mirrors {@link ar.edu.utn.frc.tup.piii.models.cards.effects.UnknownEffect} (Bloque 1).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnknownTrainerEffect extends TrainerEffect {

    /** The original {@code type} discriminator, captured for diagnostics. */
    private String type;
}
