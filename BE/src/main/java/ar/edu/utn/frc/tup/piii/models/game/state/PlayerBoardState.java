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
public class PlayerBoardState {
    
    private Long playerId;
    
    // Active and Bench
    private PokemonInPlayState activePokemon;
    @Builder.Default
    private List<PokemonInPlayState> bench = new ArrayList<>();
    
    // Card lists (mapped as instance IDs and details)
    @Builder.Default
    private List<CardInstanceState> deck = new ArrayList<>();
    @Builder.Default
    private List<CardInstanceState> hand = new ArrayList<>();
    @Builder.Default
    private List<CardInstanceState> prizeCards = new ArrayList<>();
    @Builder.Default
    private List<CardInstanceState> discardPile = new ArrayList<>();
    
    // Turn specific flags
    private boolean hasAttachedEnergyThisTurn;
    private boolean hasPlayedSupporterThisTurn;

    // Engine-persisted turn counters (write-back from domain)
    private Integer playerTurnCount;
    private boolean retreatedThisTurn;
    private boolean attackedThisTurn;

    /** Abilities already activated this turn (key: position#abilityName). */
    @Builder.Default
    private java.util.Set<String> abilitiesUsedThisTurn = new java.util.HashSet<>();
}
