package ar.edu.utn.frc.tup.piii.dtos.ws;

import java.time.LocalDateTime;

/**
 * Lightweight WebSocket notification that the game state has changed.
 * <p>
 * Clients should react by calling {@code GET /api/games/{gameId}/state}
 * to fetch the full filtered state with their JWT.
 * </p>
 */
public class GameStateChangedMessage {

    private String gameId;
    private String actionType;
    private String status;
    private LocalDateTime timestamp;

    public GameStateChangedMessage() {}

    public GameStateChangedMessage(String gameId, String actionType, String status, LocalDateTime timestamp) {
        this.gameId = gameId;
        this.actionType = actionType;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
