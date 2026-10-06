package ar.edu.utn.frc.tup.piii.controllers.ws;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Handles inbound STOMP messages from clients at {@code /app/game/...}.
 * <p>
 * Game actions are handled via REST ({@code POST /api/games/{id}/action}),
 * which triggers {@code GameEventPublisher} to broadcast events + state-changed
 * notifications to STOMP subscribers.
 * </p>
 */
@Controller
public class GameWebSocketController {

    /**
     * Minimal ping/pong to verify WebSocket connectivity.
     * Client sends {@code {"ping": true}} to {@code /app/game/ping}
     * and receives {@code {"pong": true}} on {@code /topic/game/pong}.
     */
    @MessageMapping("/game/ping")
    @SendTo("/topic/game/pong")
    public Map<String, Object> ping(Map<String, Object> payload) {
        return Map.of("pong", true, "echo", payload);
    }
}
