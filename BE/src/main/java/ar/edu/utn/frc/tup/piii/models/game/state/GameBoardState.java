package ar.edu.utn.frc.tup.piii.models.game.state;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of the full game state suitable for serialization (JSON/DB) and API transport.
 * Clean of any internal engine logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBoardState {
    
    private String gameId;
    
    // Player IDs mapping to their respective states
    private Long player1Id;
    private Long player2Id;
    
    // Turn metadata
    private Long currentPlayerId;
    private Integer turnNumber;
    private TurnPhase phase;
    
    // Player specific board states
    private PlayerBoardState player1State;
    private PlayerBoardState player2State;
    
    // Active stadium
    private String activeStadiumCardId;

    // Game state (maps to engine's BoardState.matchState)
    private GameStatus matchState;

    // Persisted verbatim — replaces the fragile turnNumber>1 derivation
    private boolean firstPlayerHasActed;

    /** Players that confirmed their initial setup (domain BoardState.setupCompletedPlayers). */
    @Builder.Default
    private java.util.List<Long> setupCompletedPlayers = new java.util.ArrayList<>();

    // A player choice the engine is waiting on mid-resolution
    private PendingSelection pendingSelection;

    // Game conclusion
    private Long winnerPlayerId;
    private String finishedReason;
}
