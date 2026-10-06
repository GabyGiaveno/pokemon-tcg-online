package ar.edu.utn.frc.tup.piii.dtos.request;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Frontend-facing request DTO for {@code POST /api/games/{id}/actions}.
 *
 * <p>Flat structure — no nested "payload" object.
 * The API translates this into the {@link ActionRequest} that the facade expects.
 *
 * <p>Field usage per action type:
 * <ul>
 *   <li>{@code PLAY_BASIC_POKEMON} – {@code cardInstanceId} (or {@code cardId}), {@code targetPosition}</li>
 *   <li>{@code EVOLVE_POKEMON} – {@code cardInstanceId} (or {@code cardId}), {@code targetPosition}</li>
 *   <li>{@code ATTACH_ENERGY} – {@code cardInstanceId} (or {@code cardId}), {@code targetPosition}</li>
 *   <li>{@code ATTACH_TOOL} – {@code cardInstanceId} (or {@code cardId}), {@code targetPosition}</li>
 *   <li>{@code RETREAT} – {@code benchIndex}</li>
 *   <li>{@code USE_ATTACK} – {@code attackIndex}</li>
 *   <li>{@code TAKE_PRIZE_CARD} – {@code prizeIndex}</li>
 *   <li>{@code PLAY_ITEM / PLAY_SUPPORTER / PLAY_STADIUM} – {@code cardInstanceId} (or {@code cardId})</li>
 *   <li>{@code END_TURN / CONCEDE / DRAW_CARD} – no extra fields</li>
 * </ul>
 *
 * <p><b>cardInstanceId vs cardId:</b><br>
 * {@code cardInstanceId} refers to a specific copy of a card within the game (e.g. {@code "p1-xy1-1-a1b2c3"}).
 * {@code cardId} is the global pokemontcg.io identifier (e.g. {@code "xy1-1"}).
 * Prefer {@code cardInstanceId} — the API resolves it to whatever the engine needs internally.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionApiRequest {

    /** The action the player wants to perform. */
    private ActionType type;

    /**
     * Instance ID of the card copy within this game (preferred).
     * Returned by {@code BoardStateDTO.myField.hand[].instanceId}.
     */
    private String cardInstanceId;

    /**
     * Global pokemontcg.io card ID (alternative to {@code cardInstanceId}).
     * Used when the frontend knows the cardId but not the instanceId.
     */
    private String cardId;

    /**
     * Target position: {@code "ACTIVE"} or {@code "BENCH_0"} … {@code "BENCH_4"}.
     */
    private String targetPosition;

    /**
     * 0-based attack index for {@code USE_ATTACK}.
     */
    private Integer attackIndex;

    /**
     * 0-based bench slot for {@code RETREAT}.
     */
    private Integer benchIndex;

    /**
     * 0-based prize card slot for {@code TAKE_PRIZE_CARD}.
     */
    private Integer prizeIndex;

    /**
     * Ordered list of instanceIds for {@code RESOLVE_SELECTION} when the pending type is REORDER_DECK.
     */
    private java.util.List<String> orderedInstanceIds;
}
