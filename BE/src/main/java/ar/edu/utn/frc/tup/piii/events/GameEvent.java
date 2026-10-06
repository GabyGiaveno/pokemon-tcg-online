package ar.edu.utn.frc.tup.piii.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event emitted by the GameEngine after every state change.
 * Published to both players via WebSocket by GameEventPublisher.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {

    private UUID gameId;
    private GameEventType type;
    private String description;
    private Map<String, Object> data;
    private long timestamp;

    /** Factory for events without extra payload. */
    public static GameEvent of(GameEventType type, String description) {
        return GameEvent.builder()
                .type(type)
                .description(description)
                .data(Collections.emptyMap())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Factory for events with a typed payload map. */
    public static GameEvent of(GameEventType type, String description, Map<String, Object> data) {
        return GameEvent.builder()
                .type(type)
                .description(description)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Factory with gameId for WebSocket routing. */
    public static GameEvent of(UUID gameId, GameEventType type, String description, Map<String, Object> data) {
        return GameEvent.builder()
                .gameId(gameId)
                .type(type)
                .description(description)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
