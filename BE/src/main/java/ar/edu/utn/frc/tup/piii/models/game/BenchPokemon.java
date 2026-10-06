package ar.edu.utn.frc.tup.piii.models.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** In-memory model of a Benched Pokémon slot: HP, damage counters, and attached cards. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BenchPokemon {

    private String instanceId;
    private String cardId;
    private int maxHp;
    private int currentHp;

    private List<AttachedCard> attachedEnergies;

    /** Null if no Pokémon Tool is attached. */
    private AttachedCard tool;

    /**
     * {@code true} during the same turn the Pokémon was first placed on the bench (played from hand).
     * Reset to {@code false} at the start of the owner's next DRAW phase.
     * Prevents evolution in the same turn a Pokémon entered play.
     */
    private boolean enteredThisTurn;
}
