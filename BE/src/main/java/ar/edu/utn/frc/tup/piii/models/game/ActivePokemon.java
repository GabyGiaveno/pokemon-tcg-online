package ar.edu.utn.frc.tup.piii.models.game;

import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** In-memory model of the Active Pokémon slot: HP, damage counters, attached cards, and status. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivePokemon {

    private String instanceId;
    private String cardId;
    private int maxHp;
    private int currentHp;

    private List<AttachedCard> attachedEnergies;

    /** Null if no Pokémon Tool is attached. */
    private AttachedCard tool;

    /**
     * Exclusive condition (NONE / ASLEEP / CONFUSED / PARALYZED).
     * BURNED and POISONED are tracked separately as they can coexist with any condition.
     */
    private SpecialCondition condition;

    /** Independent of {@code condition}; can coexist with ASLEEP, CONFUSED, PARALYZED. */
    private boolean isBurned;

    /** Independent of {@code condition}; can coexist with ASLEEP, CONFUSED, PARALYZED. */
    private boolean isPoisoned;

    /**
     * {@code true} during the same turn the Pokémon was first placed on the field (played from hand).
     * Reset to {@code false} at the start of the owner's next DRAW phase.
     * Prevents evolution in the same turn a Pokémon entered play.
     */
    private boolean enteredThisTurn;

    /**
     * {@code true} after a PREVENT_DAMAGE effect (e.g. Harden).
     * Blocks the next instance of damage dealt to this Pokémon, then resets to {@code false}.
     */
    private boolean damageProtected;

    /**
     * Active restrictions on this Pokémon (e.g. "ATTACK", "RETREAT", "ABILITY").
     * Set by RESTRICT effects; cleared at the start of the owner's next DRAW phase.
     */
    @Builder.Default
    private Set<String> restrictions = new HashSet<>();
}
