package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link PendingSelection}: validates the player's answer and applies the chosen
 * mutation, dispatching by {@link ar.edu.utn.frc.tup.piii.models.game.SelectionType}.
 *
 * <p>Single responsibility: <b>what was chosen and how it is applied</b>. This class grows as new
 * selection types are added (search deck, switch Pokémon, …) without touching the turn orchestrator.
 * It does NOT resume the turn flow — on success it returns the produced events and the caller
 * ({@link TurnManager}) decides how to continue.
 */
public class SelectionResolver {

    private final KnockoutProcessor knockoutProcessor;

    public SelectionResolver(KnockoutProcessor knockoutProcessor) {
        this.knockoutProcessor = knockoutProcessor;
    }

    /**
     * Validates {@code action} against the board's pending selection and applies it.
     * On success the {@code pendingSelection} is cleared and the resulting events are returned;
     * on any validation failure the board is left untouched.
     *
     * @return success with the applied events, or failure with the reason
     */
    public ActionResult resolve(BoardState board, Long playerId, ActionRequest action, CardLookup cardLookup) {
        PendingSelection pending = board.getPendingSelection();
        if (pending == null) {
            return ActionResult.failure("There is no selection to resolve.");
        }
        if (!pending.getOwnerPlayerId().equals(playerId)) {
            return ActionResult.failure("It is not your selection to resolve.");
        }
        switch (pending.getType()) {
            case CHOOSE_ACTIVE_ON_KO -> {
                Integer index = action.getBenchIndex();
                if (index == null || index < 0 || index >= pending.getValidOptions().size()) {
                    return ActionResult.failure("Invalid selection index.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                GameEvent promotion = knockoutProcessor.promote(ownerField, index);
                board.setPendingSelection(null);
                return ActionResult.success(List.of(promotion));
            }
            case REORDER_DECK -> {
                List<String> ordered = action.getOrderedInstanceIds();
                if (ordered == null || ordered.isEmpty()) {
                    return ActionResult.failure("orderedInstanceIds is required for REORDER_DECK.");
                }
                List<String> validOptions = pending.getValidOptions();
                if (ordered.size() != validOptions.size()
                        || !new HashSet<>(ordered).equals(new HashSet<>(validOptions))) {
                    return ActionResult.failure(
                            "orderedInstanceIds must be a permutation of the peeked cards.");
                }

                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                List<String> deck = ownerField.getDeck();
                // Remove the original top N entries and replace them with the chosen order.
                for (int i = 0; i < validOptions.size(); i++) {
                    deck.remove(0);
                }
                for (int i = ordered.size() - 1; i >= 0; i--) {
                    deck.add(0, ordered.get(i));
                }

                board.setPendingSelection(null);
                GameEvent reorderEvent = GameEvent.of(
                        GameEventType.DECK_PEEKED,
                        "Player reordered the top " + ordered.size() + " card(s) of their deck.");
                return ActionResult.success(List.of(reorderEvent));
            }
            case SEARCH_DECK -> {
                List<String> chosen = action.getOrderedInstanceIds();
                if (chosen == null || chosen.isEmpty()) {
                    return ActionResult.failure("orderedInstanceIds is required for SEARCH_DECK.");
                }
                List<String> validOptions = pending.getValidOptions();
                int max = pending.getSelectionCount();
                if (chosen.size() > max) {
                    return ActionResult.failure("Too many cards selected: max is " + max + ".");
                }
                if (!validOptions.containsAll(chosen)) {
                    return ActionResult.failure("Selected cards are not valid options.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                chosen.forEach(instanceId -> {
                    ownerField.getDeck().remove(instanceId);
                    ownerField.getHand().add(instanceId);
                });
                board.setPendingSelection(null);
                GameEvent searchEvent = GameEvent.of(
                        GameEventType.CARD_DRAWN,
                        "Player " + ownerField.getPlayerId() + " searched and took " + chosen.size() + " card(s).",
                        Map.of("playerId", ownerField.getPlayerId(), "count", chosen.size()));
                return ActionResult.success(List.of(searchEvent));
            }
            case SWITCH_POKEMON -> {
                Integer index = action.getBenchIndex();
                if (index == null || index < 0) {
                    return ActionResult.failure("benchIndex is required for SWITCH_POKEMON.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                if (index >= ownerField.getBench().size()) {
                    return ActionResult.failure("Invalid bench index for SWITCH_POKEMON.");
                }
                GameEvent switchEvent = switchActive(ownerField, index);
                board.setPendingSelection(null);
                return ActionResult.success(List.of(switchEvent));
            }
            case PLACE_ON_BENCH -> {
                List<String> chosen = action.getOrderedInstanceIds();
                if (chosen == null || chosen.size() != 1) {
                    return ActionResult.failure("orderedInstanceIds must contain exactly 1 entry for PLACE_ON_BENCH.");
                }
                String instanceId = chosen.get(0);
                List<String> validOptions = pending.getValidOptions();
                if (!validOptions.contains(instanceId)) {
                    return ActionResult.failure("Selected card is not a valid option.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                String cardId = ownerField.getInstanceCardIds().get(instanceId);
                if (cardId == null || cardLookup == null) {
                    return ActionResult.failure("Cannot resolve card for PLACE_ON_BENCH.");
                }
                Card card = cardLookup.findById(cardId);
                int maxHp = card.getHp() != null ? card.getHp() : 0;
                ownerField.getDeck().remove(instanceId);
                BenchPokemon benched = BenchPokemon.builder()
                        .instanceId(instanceId)
                        .cardId(cardId)
                        .maxHp(maxHp)
                        .currentHp(maxHp)
                        .attachedEnergies(new ArrayList<>())
                        .enteredThisTurn(true)
                        .build();
                ownerField.getBench().add(benched);
                board.setPendingSelection(null);
                return ActionResult.success(List.of(GameEvent.of(
                        GameEventType.POKEMON_PLACED,
                        card.getName() + " placed on bench.",
                        Map.of("cardId", cardId, "instanceId", instanceId,
                                "playerId", ownerField.getPlayerId()))));
            }
            case CHOOSE_FROM_DISCARD -> {
                List<String> chosen = action.getOrderedInstanceIds();
                if (chosen == null || chosen.size() != 1) {
                    return ActionResult.failure("orderedInstanceIds must contain exactly 1 entry for CHOOSE_FROM_DISCARD.");
                }
                String instanceId = chosen.get(0);
                List<String> validOptions = pending.getValidOptions();
                if (!validOptions.contains(instanceId)) {
                    return ActionResult.failure("Selected card is not a valid option.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                String cardId = ownerField.getInstanceCardIds().get(instanceId);
                if (cardId == null || cardLookup == null) {
                    return ActionResult.failure("Cannot resolve card for CHOOSE_FROM_DISCARD.");
                }
                Card card = cardLookup.findById(cardId);
                int maxHp = card.getHp() != null ? card.getHp() : 0;
                // Max Revive places the Pokémon with half its max HP (rounded down, minimum 1)
                int currentHp = Math.max(1, maxHp / 2);
                ownerField.getDiscardPile().remove(instanceId);
                BenchPokemon revived = BenchPokemon.builder()
                        .instanceId(instanceId)
                        .cardId(cardId)
                        .maxHp(maxHp)
                        .currentHp(currentHp)
                        .attachedEnergies(new ArrayList<>())
                        .enteredThisTurn(true)
                        .build();
                ownerField.getBench().add(revived);
                board.setPendingSelection(null);
                return ActionResult.success(List.of(GameEvent.of(
                        GameEventType.POKEMON_PLACED,
                        card.getName() + " revived onto bench with " + currentHp + " HP.",
                        Map.of("cardId", cardId, "instanceId", instanceId,
                                "currentHp", currentHp, "playerId", ownerField.getPlayerId()))));
            }
            case SHUFFLE_POKEMON_TO_DECK -> {
                List<String> chosen = action.getOrderedInstanceIds();
                if (chosen == null || chosen.size() != 1) {
                    return ActionResult.failure("orderedInstanceIds must contain exactly 1 entry for SHUFFLE_POKEMON_TO_DECK.");
                }
                String instanceId = chosen.get(0);
                List<String> validOptions = pending.getValidOptions();
                if (!validOptions.contains(instanceId)) {
                    return ActionResult.failure("Selected Pokémon is not a valid option.");
                }
                PlayerField ownerField = fieldOf(board, pending.getOwnerPlayerId());
                BenchPokemon target = ownerField.getBench().stream()
                        .filter(bp -> instanceId.equals(bp.getInstanceId()))
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    return ActionResult.failure("Bench Pokémon not found.");
                }
                List<String> toShuffle = new ArrayList<>();
                toShuffle.add(target.getInstanceId());
                if (target.getAttachedEnergies() != null) {
                    target.getAttachedEnergies().forEach(e -> toShuffle.add(e.getInstanceId()));
                }
                if (target.getTool() != null) {
                    toShuffle.add(target.getTool().getInstanceId());
                }
                ownerField.getBench().remove(target);
                ownerField.getDeck().addAll(toShuffle);
                Collections.shuffle(ownerField.getDeck());
                board.setPendingSelection(null);
                return ActionResult.success(List.of(GameEvent.of(
                        GameEventType.POKEMON_SHUFFLED_INTO_DECK,
                        target.getCardId() + " and " + (toShuffle.size() - 1) + " attached card(s) shuffled into deck.",
                        Map.of("cardId", target.getCardId(), "instanceId", instanceId,
                                "playerId", ownerField.getPlayerId()))));
            }
            default -> {
                return ActionResult.failure("Unsupported selection type.");
            }
        }
    }

    /** Backward-compatible overload for callers that don't have a CardLookup. */
    public ActionResult resolve(BoardState board, Long playerId, ActionRequest action) {
        return resolve(board, playerId, action, null);
    }

    /**
     * Moves the current Active to the bench and promotes the Benched Pokémon at {@code benchIndex}.
     * Conditions are cleared on the newly promoted Pokémon (standard switch rule).
     */
    private GameEvent switchActive(PlayerField field, int benchIndex) {
        ActivePokemon currentActive = field.getActivePokemon();
        BenchPokemon chosen = field.getBench().remove(benchIndex);

        // Move current Active → Bench (preserve HP, energies, tool; no condition on bench)
        if (currentActive != null) {
            BenchPokemon demoted = BenchPokemon.builder()
                    .instanceId(currentActive.getInstanceId())
                    .cardId(currentActive.getCardId())
                    .maxHp(currentActive.getMaxHp())
                    .currentHp(currentActive.getCurrentHp())
                    .attachedEnergies(currentActive.getAttachedEnergies() != null
                            ? new ArrayList<>(currentActive.getAttachedEnergies())
                            : new ArrayList<>())
                    .tool(currentActive.getTool())
                    .enteredThisTurn(false)
                    .build();
            field.getBench().add(demoted);
        }

        // Promote chosen bench → Active (conditions cleared per TCG switch rules)
        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(chosen.getInstanceId())
                .cardId(chosen.getCardId())
                .maxHp(chosen.getMaxHp())
                .currentHp(chosen.getCurrentHp())
                .attachedEnergies(chosen.getAttachedEnergies() != null
                        ? new ArrayList<>(chosen.getAttachedEnergies())
                        : new ArrayList<>())
                .tool(chosen.getTool())
                .condition(SpecialCondition.NONE)
                .isBurned(false)
                .isPoisoned(false)
                .enteredThisTurn(false)
                .build();
        field.setActivePokemon(newActive);

        return GameEvent.of(
                GameEventType.POKEMON_RETREATED,
                chosen.getCardId() + " switched in for player " + field.getPlayerId() + ".",
                Map.of("cardId", chosen.getCardId(), "playerId", field.getPlayerId()));
    }

    /** Resolves a field by an arbitrary playerId (the selection owner, not necessarily the current player). */
    private PlayerField fieldOf(BoardState board, Long playerId) {
        return board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }
}
