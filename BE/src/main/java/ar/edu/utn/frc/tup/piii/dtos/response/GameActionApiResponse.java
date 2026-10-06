package ar.edu.utn.frc.tup.piii.dtos.response;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Stable API response DTO for {@code POST /api/games/{id}/actions}.
 *
 * <p>This is the frontend-facing response. Game-rule errors (wrong phase,
 * card not in hand, etc.) are returned as {@code success: false} with a
 * descriptive {@code error} message — not as HTTP error codes.
 *
 * <p>The {@code events} list contains only public event summaries.
 * The frontend must call {@code GET /api/games/{id}/state} to obtain the
 * full filtered board state after receiving a {@code state-changed} WebSocket
 * notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionApiResponse {

    /** Whether the action was accepted and processed by the game engine. */
    private boolean success;

    /** The action type that was requested (echoed from the request). */
    private ActionType actionType;

    /**
     * Game events produced by this action (e.g. card drawn, energy attached).
     * Empty on failure. Never exposes private opponent state.
     */
    @Builder.Default
    private List<GameEventDTO> events = new ArrayList<>();

    /**
     * Human-readable error message. Present only when {@code success = false}.
     * Example: {@code "Card not in hand"}, {@code "Not your turn"}.
     */
    private String error;

    // ---------------------------------------------------------------
    //  Factory methods
    // ---------------------------------------------------------------

    /** Creates a success response for the given action type and events. */
    public static GameActionApiResponse ok(ActionType actionType, List<GameEventDTO> events) {
        return GameActionApiResponse.builder()
                .success(true)
                .actionType(actionType)
                .events(events != null ? events : new ArrayList<>())
                .build();
    }

    /** Creates a failure response for the given action type and error message. */
    public static GameActionApiResponse fail(ActionType actionType, String error) {
        return GameActionApiResponse.builder()
                .success(false)
                .actionType(actionType)
                .events(new ArrayList<>())
                .error(error)
                .build();
    }
}
