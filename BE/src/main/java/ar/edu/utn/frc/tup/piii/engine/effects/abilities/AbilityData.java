package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PassiveAbilityEffect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * One ability of a card as parsed from the {@code "abilities"} block of
 * {@code Card.parsedEffects} (mirror of {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackData}
 * for the abilities family).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbilityData {

    private String name;
    private String text;
    @Builder.Default
    private List<AttackEffect> parsedEffects = new ArrayList<>();

    /** Convenience accessor: the typed {@link PassiveAbilityEffect}s of this ability. */
    public List<PassiveAbilityEffect> passiveEffects() {
        List<PassiveAbilityEffect> result = new ArrayList<>();
        if (parsedEffects == null) return result;
        for (AttackEffect e : parsedEffects) {
            if (e instanceof PassiveAbilityEffect pae) {
                result.add(pae);
            }
        }
        return result;
    }
}
