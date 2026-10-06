package ar.edu.utn.frc.tup.piii.dtos.response;

import java.time.LocalDateTime;

/** Response DTO for a discrete game event broadcast via WebSocket (type, payload, timestamp). */
public class GameEventDTO {
    private String type;
    private Object payload;
    private LocalDateTime timestamp;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
