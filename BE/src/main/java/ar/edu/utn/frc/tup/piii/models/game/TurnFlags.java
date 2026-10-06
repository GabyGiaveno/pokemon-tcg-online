package ar.edu.utn.frc.tup.piii.models.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/** In-memory model of per-turn boolean flags reset at the start of each DRAW phase. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TurnFlags {
    private boolean energyAttachedThisTurn;   // máx. 1 energía por turno
    private boolean retreatedThisTurn;      // máx. 1 retiro por turno
    private boolean supporterPlayedThisTurn;  // máx. 1 Supporter por turno
    private boolean attackedThisTurn;        // si es true, el turno debe terminar

    /**
     * Abilities already activated this turn ("Once during your turn…").
     * Key format: {@code targetPosition + "#" + abilityName} — positional within the turn,
     * which is enough for XY1 tier 1 (duplicated copies in play differ by position).
     */
    @Builder.Default
    private Set<String> abilitiesUsedThisTurn = new HashSet<>();
}
