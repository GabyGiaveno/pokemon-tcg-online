package ar.edu.utn.frc.tup.piii.events;

import ar.edu.utn.frc.tup.piii.dtos.ws.GameEventMessage;
import ar.edu.utn.frc.tup.piii.dtos.ws.GameStateChangedMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Publishes game events and state-change notifications to STOMP topics.
 * <p>
 * Two paths:
 * <ol>
 *   <li>{@link #publishGameEvents(UUID, List)} / {@link #publishStateChanged(UUID, String, String)}
 *       — called directly from {@code GameService} after a REST action is processed.</li>
 *   <li>{@link #handleGameEvent(GameEvent)} — Spring {@link EventListener} for the
 *       {@link GameEvent} domain event (future WebSocket-action path or internal use).</li>
 * </ol>
 * </p>
 */
@Component
public class GameEventPublisher {

    static final String EVENTS_TOPIC = "/topic/games/%s/events";
    static final String STATE_CHANGED_TOPIC = "/topic/games/%s/state-changed";

    private final SimpMessagingTemplate messagingTemplate;

    public GameEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ================================================================
    //  Direct publish (called from GameService after REST action)
    // ================================================================

    /**
     * Publishes each game event to {@code /topic/games/{gameId}/events}.
     * No private information is included.
     */
    public void publishGameEvents(UUID gameId, List<GameEvent> events) {
        if (events == null || events.isEmpty()) return;
        String topic = String.format(EVENTS_TOPIC, gameId);
        for (GameEvent event : events) {
            GameEventMessage msg = new GameEventMessage(
                    gameId.toString(),
                    event.getType().name(),
                    event.getData(),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSend(topic, msg);
        }
    }

    /**
     * Publishes a state-changed notification to {@code /topic/games/{gameId}/state-changed}.
     * Clients should react by fetching the full state via REST with their JWT.
     */
    public void publishStateChanged(UUID gameId, String actionType, String status) {
        String topic = String.format(STATE_CHANGED_TOPIC, gameId);
        GameStateChangedMessage msg = new GameStateChangedMessage(
                gameId.toString(),
                actionType,
                status,
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSend(topic, msg);
    }

    // ================================================================
    //  Spring ApplicationEvent listener (kept for backward compat)
    // ================================================================

    @EventListener
    public void handleGameEvent(GameEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + event.getGameId(), event);
    }
}
