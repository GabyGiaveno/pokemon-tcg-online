package ar.edu.utn.frc.tup.piii.dtos.ws;

import java.time.LocalDateTime;

/** Lightweight WebSocket DTO for a single game event (no private information). */
public class GameEventMessage {

    private String gameId;
    private String eventType;
    private Object payload;
    private LocalDateTime timestamp;

    public GameEventMessage() {}

    public GameEventMessage(String gameId, String eventType, Object payload, LocalDateTime timestamp) {
        this.gameId = gameId;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
