package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes a Knockout event for a single Active Pokémon.
 *
 * <p>Responsibilities (mutation + reporting only — does NOT decide the game):
 * <ol>
 *   <li>Discard the KO'd Pokémon and all its attachments.</li>
 *   <li>Award the Prize Card(s) to the opponent (2 for EX/MEGA, otherwise 1).</li>
 *   <li>If the Bench is not empty, request a {@code CHOOSE_ACTIVE_ON_KO} selection so the owner
 *       picks which Bench Pokémon to promote (the orchestrator pauses on it).</li>
 * </ol>
 *
 * <p>Victory ("all prizes taken" / "no Pokémon left") is NOT decided here. The KO only
 * <em>enables</em> a win; the orchestrator ({@link TurnManager}) runs
 * {@link VictoryConditionChecker} after the KO and declares the result.
 */
public class KnockoutProcessor {

    /**
     * Checks whether {@code pokemon} has been knocked out (HP ≤ 0).
     * If so, processes the full KO sequence and mutates {@code board}.
     *
     * @param pokemon       the Active Pokémon to check
     * @param ownerField    the field of the Pokémon's owner
     * @param prizeTakerField the field of the player who takes prize cards (usually the attacker)
     * @param board           board state (mutated to set the pendingSelection when a Bench exists)
     * @return list of events that occurred (empty if no KO)
     */
    public List<GameEvent> processIfKnockedOut(ActivePokemon pokemon,
                                                PlayerField ownerField,
                                                PlayerField prizeTakerField,
                                                BoardState board,
                                                CardLookup cardLookup) {
        List<GameEvent> events = new ArrayList<>();
        if (pokemon == null || pokemon.getCurrentHp() > 0) return events;

        events.add(discardKnockedOut(pokemon, ownerField));
        awardPrizes(pokemon.getCardId(), prizeTakerField, cardLookup, events);
        requestPromotionIfPossible(ownerField, board);
        return events;
    }

    /** Discards the KO'd Pokémon and all its attachments, and clears the Active slot. */
    private GameEvent discardKnockedOut(ActivePokemon pokemon, PlayerField ownerField) {
        String koCardId = pokemon.getCardId();
        // Use instanceId so rebuildZone can look up the CardInstanceState and preserve cardId metadata.
        // Fall back to cardId only if instanceId is absent (defensive; should not happen in normal play).
        String koId = pokemon.getInstanceId() != null ? pokemon.getInstanceId() : koCardId;
        ownerField.getDiscardPile().add(koId);
        pokemon.getAttachedEnergies().forEach(e -> {
            String id = e.getInstanceId() != null ? e.getInstanceId() : e.getCardId();
            ownerField.getDiscardPile().add(id);
        });
        if (pokemon.getTool() != null) {
            String id = pokemon.getTool().getInstanceId() != null ? pokemon.getTool().getInstanceId() : pokemon.getTool().getCardId();
            ownerField.getDiscardPile().add(id);
        }
        ownerField.setActivePokemon(null);
        return GameEvent.of(GameEventType.POKEMON_KNOCKED_OUT,
                koCardId + " was knocked out.",
                Map.of("cardId", koCardId, "playerId", ownerField.getPlayerId()));
    }

    /**
     * Takes the Prize Card(s) for this KO (2 for EX/MEGA, otherwise 1) and reports them.
     * Victory ("all prizes taken") is NOT decided here — the orchestrator runs
     * {@link VictoryConditionChecker} afterwards.
     */
    private void awardPrizes(String koCardId, PlayerField prizeTakerField,
                             CardLookup cardLookup, List<GameEvent> events) {
        int prizesToTake = prizesFor(koCardId, cardLookup);
        int prizesTaken = 0;
        while (prizesTaken < prizesToTake && !prizeTakerField.getPrizeCards().isEmpty()) {
            String prizeCardId = prizeTakerField.getPrizeCards().remove(0);
            prizeTakerField.getHand().add(prizeCardId);
            prizesTaken++;
        }
        if (prizesTaken > 0) {
            events.add(GameEvent.of(GameEventType.PRIZE_TAKEN,
                    "Player " + prizeTakerField.getPlayerId() + " took " + prizesTaken + " prize card(s).",
                    Map.of("playerId", prizeTakerField.getPlayerId(),
                            "prizeCardsLeft", prizeTakerField.getPrizeCards().size(),
                            "prizesTaken", prizesTaken)));
        }
    }

    /** Number of prizes a KO of {@code koCardId} awards: 2 for EX/MEGA, otherwise 1. */
    private int prizesFor(String koCardId, CardLookup cardLookup) {
        ar.edu.utn.frc.tup.piii.entities.Card koCard = cardLookup.findById(koCardId);
        if (koCard != null && koCard.getSubtypes() != null
                && koCard.getSubtypes().stream()
                        .anyMatch(s -> s.equalsIgnoreCase("EX") || s.equalsIgnoreCase("MEGA"))) {
            return 2;
        }
        return 1;
    }

    /**
     * If the owner still has Benched Pokémon, requests a {@code CHOOSE_ACTIVE_ON_KO} selection and
     * STOPS (no promotion): the orchestrator detects the {@code pendingSelection}, cuts the flow,
     * and {@code resolveSelection} promotes the chosen Pokémon later. Empty Bench → no selection;
     * the orchestrator's {@link VictoryConditionChecker} declares the no-Pokémon defeat.
     */
    private void requestPromotionIfPossible(PlayerField ownerField, BoardState board) {
        if (ownerField.getBench().isEmpty()) {
            return;
        }
        List<String> options = ownerField.getBench().stream()
                .map(BenchPokemon::getCardId)
                .toList();
        board.setPendingSelection(PendingSelection.builder()
                .type(SelectionType.CHOOSE_ACTIVE_ON_KO)
                .ownerPlayerId(ownerField.getPlayerId())
                .validOptions(options)
                .prompt("Choose which Benched Pokémon to promote to Active.")
                .build());
    }

    /**
     * Checks whether the Benched Pokémon at {@code benchIndex} has been knocked out (HP ≤ 0).
     * If so, discards it, awards prizes, and removes the slot from the bench.
     * Unlike Active KOs, bench KOs do NOT trigger a promotion selection.
     *
     * @param benchIndex      index of the bench slot to check (caller must validate range)
     * @param ownerField      the field of the bench Pokémon's owner
     * @param prizeTakerField the field of the player who takes prize cards (usually the attacker)
     * @param board           board state (not mutated beyond prize cards — no pendingSelection set)
     * @return list of events (empty if not knocked out)
     */
    public List<GameEvent> processIfKnockedOutOnBench(int benchIndex,
                                                       PlayerField ownerField,
                                                       PlayerField prizeTakerField,
                                                       BoardState board,
                                                       CardLookup cardLookup) {
        List<GameEvent> events = new ArrayList<>();
        if (benchIndex < 0 || benchIndex >= ownerField.getBench().size()) return events;

        BenchPokemon bench = ownerField.getBench().get(benchIndex);
        if (bench == null || bench.getCurrentHp() > 0) return events;

        String koId = bench.getInstanceId() != null ? bench.getInstanceId() : bench.getCardId();
        ownerField.getDiscardPile().add(koId);
        if (bench.getAttachedEnergies() != null) {
            bench.getAttachedEnergies().forEach(e -> {
                String id = e.getInstanceId() != null ? e.getInstanceId() : e.getCardId();
                ownerField.getDiscardPile().add(id);
            });
        }
        if (bench.getTool() != null) {
            String id = bench.getTool().getInstanceId() != null
                    ? bench.getTool().getInstanceId() : bench.getTool().getCardId();
            ownerField.getDiscardPile().add(id);
        }
        ownerField.getBench().remove(benchIndex);

        events.add(GameEvent.of(GameEventType.POKEMON_KNOCKED_OUT,
                bench.getCardId() + " was knocked out on the bench.",
                Map.of("cardId", bench.getCardId(), "playerId", ownerField.getPlayerId())));
        awardPrizes(bench.getCardId(), prizeTakerField, cardLookup, events);
        return events;
    }

    /**
     * Promotes the Benched Pokémon at {@code benchIndex} to the Active slot, transferring its
     * HP, energies and tool, and clearing any conditions. Caller must ensure the index is valid.
     *
     * @param ownerField the field whose Bench Pokémon is being promoted
     * @param benchIndex 0-based index into the owner's Bench
     * @return the PHASE_CHANGED event describing the promotion
     */
    public GameEvent promote(PlayerField ownerField, int benchIndex) {
        BenchPokemon promoted = ownerField.getBench().remove(benchIndex);
        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(promoted.getInstanceId())
                .cardId(promoted.getCardId())
                .maxHp(promoted.getMaxHp())
                .currentHp(promoted.getCurrentHp())
                .attachedEnergies(promoted.getAttachedEnergies() != null
                        ? new ArrayList<>(promoted.getAttachedEnergies())
                        : new ArrayList<>())
                .tool(promoted.getTool())
                .condition(SpecialCondition.NONE)
                .isBurned(false)
                .isPoisoned(false)
                .enteredThisTurn(false) // was already in play on the bench
                .build();
        ownerField.setActivePokemon(newActive);
        return GameEvent.of(GameEventType.PHASE_CHANGED,
                promoted.getCardId() + " was promoted to Active.",
                Map.of("cardId", promoted.getCardId(), "playerId", ownerField.getPlayerId()));
    }
}
