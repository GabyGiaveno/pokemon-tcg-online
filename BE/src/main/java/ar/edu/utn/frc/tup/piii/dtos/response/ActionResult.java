package ar.edu.utn.frc.tup.piii.dtos.response;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Returned by {@code GameEngineFacade.processAction()} for every player action.
 *
 * <p>On success: {@code success = true}, {@code events} contains everything
 * that happened (draw, status effects, KO, etc.).
 * On failure: {@code success = false}, {@code error} describes why.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionResult {

    private boolean success;

    /** Human-readable reason for rejection. Null when {@code success = true}. */
    private String error;

    /** Ordered list of game events produced by this action and any automatic phases that followed. */
    @Builder.Default
    private List<GameEvent> events = new ArrayList<>();

    public static ActionResult success(List<GameEvent> events) {
        return ActionResult.builder()
                .success(true)
                .events(events != null ? events : new ArrayList<>())
                .build();
    }

    public static ActionResult failure(String error) {
        return ActionResult.builder()
                .success(false)
                .error(error)
                .events(new ArrayList<>())
                .build();
    }
}
