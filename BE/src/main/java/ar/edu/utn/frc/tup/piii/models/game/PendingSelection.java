package ar.edu.utn.frc.tup.piii.models.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A selection the engine is waiting on, mid-resolution, before it can continue.
 *
 * <p>This is plain data — NOT a callback. The continuation is reified as a {@link SelectionType}
 * plus the data needed to validate the answer, so the board survives serialization between the
 * request that emits the selection and the later request that resolves it. {@code TurnManager}
 * reconstructs the resume path from {@code type} and the board's current phase.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingSelection {

    /** What kind of choice is being requested (drives how it is resolved). */
    private SelectionType type;

    /** ID of the player who must make the choice. Only this player may resolve it. */
    private Long ownerPlayerId;

    /**
     * Valid choices, as card IDs. For {@code CHOOSE_ACTIVE_ON_KO} these are the owner's
     * Benched Pokémon (in bench order, so a 0-based index maps to a bench slot).
     */
    private List<String> validOptions;

    /** Optional human-readable prompt for the UI. */
    private String prompt;

    /**
     * For {@code REORDER_DECK}: the cardId for each instanceId in {@code validOptions} (same order).
     * Lets the UI display the actual card images without needing to look them up from the deck.
     */
    private List<String> revealedCardIds;

    /**
     * For {@code SEARCH_DECK}: how many cards the player must pick from {@code validOptions}.
     * 0 means "pick as many as available" (up to the full list).
     */
    @Builder.Default
    private int selectionCount = 1;
}
