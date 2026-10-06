package ar.edu.utn.frc.tup.piii.controllers.ws;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWebSocketControllerTest {

    private final GameWebSocketController controller = new GameWebSocketController();

    @Test
    void ping_returnsPongAndEcho() {
        Map<String, Object> payload = Map.of("ping", true, "client", "tester");

        Map<String, Object> response = controller.ping(payload);

        assertEquals(true, response.get("pong"));
        assertEquals(payload, response.get("echo"));
        assertTrue(response.containsKey("pong"));
        assertTrue(response.containsKey("echo"));
    }
}
