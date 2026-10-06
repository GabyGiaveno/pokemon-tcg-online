package ar.edu.utn.frc.tup.piii.models.game.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokemonInPlayState {
    
    private String instanceId; // unique ID for this specific card instance
    private String cardId;     // The pokemontcg.io ID (e.g. "xy1-1")
    private String name;
    
    private int currentHp;
    private int maxHp;
    
    @Builder.Default
    private List<CardInstanceState> attachedEnergies = new ArrayList<>();
    @Builder.Default
    private List<CardInstanceState> attachedTools = new ArrayList<>();
    @Builder.Default
    private List<CardInstanceState> evolutionCards = new ArrayList<>();
    @Builder.Default
    private List<String> statusConditions = new ArrayList<>();

    /**
     * Engine-side condition name (e.g. "ASLEEP", "CONFUSED", "PARALYZED", "NONE").
     * Complement to {@code statusConditions} — used by the engine mapper for lossless round-trip.
     */
    private String condition;

    /** Independent burn status (can coexist with condition). */
    private boolean burned;

    /** Independent poison status (can coexist with condition). */
    private boolean poisoned;

    /** {@code true} if this Pokémon was placed on the field this turn. */
    private boolean enteredThisTurn;

    /** {@code true} if a PREVENT_DAMAGE effect is active; cleared after absorbing one hit. */
    private boolean damageProtected;

    /** Active RESTRICT restrictions (e.g. "ATTACK", "RETREAT", "ABILITY"); cleared next turn. */
    @Builder.Default
    private List<String> restrictions = new ArrayList<>();
}
