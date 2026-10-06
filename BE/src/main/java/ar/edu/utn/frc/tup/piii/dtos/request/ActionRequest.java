package ar.edu.utn.frc.tup.piii.dtos.request;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for {@code POST /api/games/{id}/actions}.
 *
 * <p>Field usage per action type:
 * <ul>
 *   <li>PLAY_BASIC_POKEMON – {@code cardId}</li>
 *   <li>EVOLVE_POKEMON – {@code cardId} (evolution card), {@code targetPosition}</li>
 *   <li>ATTACH_ENERGY – {@code cardId}, {@code targetPosition}</li>
 *   <li>ATTACH_TOOL – {@code cardId}, {@code targetPosition}</li>
 *   <li>RETREAT – {@code benchIndex} (bench slot to promote)</li>
 *   <li>PLAY_ITEM / PLAY_SUPPORTER / PLAY_STADIUM – {@code cardId}</li>
 *   <li>USE_ATTACK – {@code attackIndex}</li>
 *   <li>TAKE_PRIZE_CARD – {@code prizeIndex}</li>
 *   <li>END_TURN / CONCEDE – no extra fields</li>
 * </ul>
 *
 * <p>{@code targetPosition} format: {@code "ACTIVE"} or {@code "BENCH_0"} … {@code "BENCH_4"}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionRequest {

    /** The action the player wants to perform. */
    private ActionType type;

    /**
     * ID of the card in hand to play.
     * Used by: PLAY_BASIC_POKEMON, EVOLVE_POKEMON, ATTACH_ENERGY, ATTACH_TOOL,
     *          PLAY_ITEM, PLAY_SUPPORTER, PLAY_STADIUM.
     */
    private String cardId;

    /**
     * Target position on the field: {@code "ACTIVE"} or {@code "BENCH_0"} … {@code "BENCH_4"}.
     * Used by: EVOLVE_POKEMON, ATTACH_ENERGY, ATTACH_TOOL.
     */
    private String targetPosition;

    /**
     * 0-based index of the attack to execute.
     * Used by: USE_ATTACK.
     */
    private Integer attackIndex;

    /**
     * 0-based bench slot of the Pokémon to promote to Active.
     * Used by: RETREAT.
     */
    private Integer benchIndex;

    /**
     * 0-based index of the prize card slot to take.
     * Used by: TAKE_PRIZE_CARD.
     */
    private Integer prizeIndex;

    /**
     * 0-based index of the ability to activate on the Pokémon at {@code targetPosition}.
     * Used by: USE_ABILITY. Defaults to 0 when null (most XY1 Pokémon have one ability).
     */
    private Integer abilityIndex;

    /**
     * Ordered list of instanceIds for deck-reorder selections.
     * Used by: RESOLVE_SELECTION when the pending type is REORDER_DECK.
     * Must be a permutation of the instanceIds returned in {@code pendingSelection.validOptions}.
     */
    private java.util.List<String> orderedInstanceIds;
}
