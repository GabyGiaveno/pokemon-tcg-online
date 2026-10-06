package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A choice the engine is waiting on mid-resolution (e.g. the defender picking
 * which Benched Pokémon to promote after a KO). When present in
 * {@link BoardStateDTO}, the owner answers with
 * {@code POST /actions {type: RESOLVE_SELECTION, benchIndex}}; every other
 * action is rejected until then.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingSelectionDTO {
    /** SelectionType name, e.g. {@code CHOOSE_ACTIVE_ON_KO}. */
    private String type;
    /** Who must answer — may NOT be the current-turn player. */
    private Long ownerPlayerId;
    /** Candidate cardIds (the owner's bench, in benchIndex order). */
    private List<String> validOptions;
    /** Human-readable prompt for the UI. */
    private String prompt;
    /** For REORDER_DECK: cardId for each instanceId in validOptions (same order). */
    private List<String> revealedCardIds;
    /** For SEARCH_DECK: how many cards the player must pick. */
    private int selectionCount;
}
