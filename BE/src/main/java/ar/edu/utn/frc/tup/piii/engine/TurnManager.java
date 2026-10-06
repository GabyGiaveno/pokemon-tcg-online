package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.state.AttackPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.BetweenTurnsState;
import ar.edu.utn.frc.tup.piii.engine.state.DrawPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.MainPhaseState;
import ar.edu.utn.frc.tup.piii.engine.state.SetupPhaseState;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import ar.edu.utn.frc.tup.piii.models.game.VictoryReason;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the full turn lifecycle: DRAW → MAIN → (optional ATTACK) → BETWEEN_TURNS → (next DRAW).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Enforces turn ownership (only the active player may act).</li>
 *   <li>Routes {@link ActionType#USE_ATTACK} and {@link ActionType#END_TURN} / {@link ActionType#CONCEDE}
 *       before delegating regular MAIN actions to {@link MainPhaseState}.</li>
 *   <li>Executes automatic phases (DRAW, BETWEEN_TURNS) and switches the active player.</li>
 *   <li>Resets per-turn flags and clears {@code enteredThisTurn} at the start of every DRAW phase.</li>
 * </ul>
 *
 * <p>The {@link CardLookup} parameter is the only link to the persistence layer;
 * this class and all phase states are plain Java with no Spring dependencies.
 */
@Service
public class TurnManager {

    private final DrawPhaseState    drawPhase;
    private final MainPhaseState    mainPhase;
    private final AttackPhaseState  attackPhase;
    private final BetweenTurnsState betweenTurnsPhase;
    private final SetupPhaseState   setupPhase;

    private final VictoryConditionChecker victoryChecker;
    private final SelectionResolver       selectionResolver;

    public TurnManager() {
        this(new DrawPhaseState(), new MainPhaseState(), new AttackPhaseState(),
                new BetweenTurnsState(), new SetupPhaseState());
    }

    /** Constructor for testing: inject custom phase implementations (decision collaborators are real). */
    public TurnManager(DrawPhaseState draw, MainPhaseState main,
                       AttackPhaseState attack, BetweenTurnsState betweenTurns,
                       SetupPhaseState setup) {
        this(draw, main, attack, betweenTurns, setup,
                new VictoryConditionChecker(), new SelectionResolver(new KnockoutProcessor()));
    }

    /** Full constructor for testing: inject the decision collaborators too (DIP). */
    public TurnManager(DrawPhaseState draw, MainPhaseState main,
                       AttackPhaseState attack, BetweenTurnsState betweenTurns,
                       SetupPhaseState setup,
                       VictoryConditionChecker victoryChecker, SelectionResolver selectionResolver) {
        this.drawPhase         = draw;
        this.mainPhase         = main;
        this.attackPhase       = attack;
        this.betweenTurnsPhase = betweenTurns;
        this.setupPhase        = setup;
        this.victoryChecker    = victoryChecker;
        this.selectionResolver = selectionResolver;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Starts a new turn for the current active player.
     * Resets turn flags, clears {@code enteredThisTurn} on all Pokémon, then
     * auto-executes the DRAW phase and transitions the board to MAIN.
     *
     * <p>Called by {@link ar.edu.utn.frc.tup.piii.engine.GameEngineFacade} when a new game
     * starts, and internally after each BETWEEN_TURNS completes.
     *
     * @return ordered list of events produced (draw event, or deck-out if game over)
     */
    public List<GameEvent> beginTurn(BoardState board, CardLookup cardLookup) {
        resetTurnFlags(board);
        List<GameEvent> events = new ArrayList<>(drawPhase.execute(board, cardLookup));

        if (board.getMatchState() == GameStatus.FINISHED) {
            return events; // Deck-out: game is over, don't transition to MAIN
        }

        board.setCurrentPhase(TurnPhase.MAIN);
        events.add(GameEvent.of(GameEventType.PHASE_CHANGED,
                "Player " + board.getCurrentPlayerId() + "'s MAIN phase.",
                Map.of("playerId", board.getCurrentPlayerId(), "phase", "MAIN")));
        return events;
    }

    /**
     * Processes a player action. Only valid when the board is in MAIN phase.
     *
     * <p>Three actions are intercepted here before reaching {@link MainPhaseState}:
     * <ul>
     *   <li>{@code USE_ATTACK} – validates, executes attack, then auto-completes the turn.</li>
     *   <li>{@code END_TURN} – skips attack, auto-completes the turn.</li>
     *   <li>{@code CONCEDE} – ends the game immediately.</li>
     * </ul>
     *
     * @param action     the player's action
     * @param board      current board (mutated in place)
     * @param playerId   ID of the requesting player
     * @param cardLookup bridge to card persistence
     * @return result containing all events from this action and any automatic phases
     */
    public ActionResult processAction(ActionRequest action, BoardState board,
                                      Long playerId, CardLookup cardLookup) {
        if (board.getMatchState() == GameStatus.FINISHED) {
            return ActionResult.failure("The game is already over.");
        }

        ActionType type = action.getType();

        // Pending selection: the engine is paused waiting for a specific player's choice.
        // Only RESOLVE_SELECTION from the selection's owner is accepted; anything else is rejected
        // without mutating. The owner may NOT be the current player (e.g. the defender was KO'd).
        if (board.getPendingSelection() != null) {
            if (type != ActionType.RESOLVE_SELECTION) {
                return ActionResult.failure("A pending selection must be resolved first.");
            }
            return resolveSelection(board, playerId, action, cardLookup);
        }
        if (type == ActionType.RESOLVE_SELECTION) {
            return ActionResult.failure("There is no selection to resolve.");
        }

        // CONCEDE is always allowed from any non-FINISHED state
        if (type == ActionType.CONCEDE) {
            return handleConcede(board, playerId);
        }

        // ── SETUP phase ─────────────────────────────────────────────────
        if (board.getMatchState() == GameStatus.SETUP) {
            ActionResult result = setupPhase.handle(action, board, playerId, cardLookup);
            if (!result.isSuccess()) {
                return result;
            }
            // Setup just completed both players → begin the first turn
            if (board.getMatchState() == GameStatus.ACTIVE) {
                List<GameEvent> allEvents = new ArrayList<>(result.getEvents());
                allEvents.addAll(beginTurn(board, cardLookup));
                return ActionResult.success(allEvents);
            }
            return result;
        }

        // ── ACTIVE: guard: turn ownership + MAIN phase ──────────────────
        if (!board.getCurrentPlayerId().equals(playerId)) {
            return ActionResult.failure("It is not your turn.");
        }
        if (board.getCurrentPhase() != TurnPhase.MAIN) {
            return ActionResult.failure("Actions are only allowed during the MAIN phase.");
        }

        // Route special actions
        // (CONCEDE is already handled above, before the turn-ownership guard.)
        if (type == ActionType.END_TURN) {
            return handleEndTurnFlow(board, playerId, cardLookup);
        }
        if (type == ActionType.USE_ATTACK) {
            return handleAttackFlow(action, board, playerId, cardLookup);
        }

        // Regular MAIN action: delegate to MainPhaseState
        return mainPhase.handle(action, board, playerId, cardLookup);
    }

    // =========================================================================
    // Private: action flows
    // =========================================================================

    private ActionResult handleAttackFlow(ActionRequest action, BoardState board,
                                          Long playerId, CardLookup cardLookup) {
        PlayerField actorField = getActiveField(board);
        try {
            RuleValidator.validateUseAttack(board, actorField);
        } catch (InvalidActionException e) {
            return ActionResult.failure("[" + e.getErrorCode() + "] " + e.getMessage());
        }

        board.setCurrentPhase(TurnPhase.ATTACK);
        ActionResult attackResult = attackPhase.handle(action, board, playerId, cardLookup);
        if (!attackResult.isSuccess()) {
            board.setCurrentPhase(TurnPhase.MAIN); // revert on failure
            return attackResult;
        }

        List<GameEvent> allEvents = new ArrayList<>(attackResult.getEvents());

        // A KO may have happened in the attack pipeline. The KO only mutates; victory is decided here.
        if (finishedByVictory(board, allEvents)) {
            return ActionResult.success(allEvents);
        }
        // If a KO'd player must choose its new Active, pause here and wait for RESOLVE_SELECTION.
        if (board.getPendingSelection() != null) {
            return ActionResult.success(allEvents);
        }
        return completeTurn(board, cardLookup, allEvents);
    }

    private ActionResult handleEndTurnFlow(BoardState board, Long playerId, CardLookup cardLookup) {
        List<GameEvent> events = new ArrayList<>();
        events.add(GameEvent.of(GameEventType.TURN_ENDED,
                "Player " + playerId + " ended their turn.",
                Map.of("playerId", playerId)));
        return completeTurn(board, cardLookup, events);
    }

    private ActionResult handleConcede(BoardState board, Long playerId) {
        Long winnerId = getOpponentField(board).getPlayerId();
        board.setMatchState(GameStatus.FINISHED);
        board.setWinnerId(winnerId);
        board.setFinishedReason("CONCEDE");
        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.PLAYER_CONCEDED,
                "Player " + playerId + " conceded. Player " + winnerId + " wins!",
                Map.of("concededPlayerId", playerId, "winnerId", winnerId))));
    }

    /**
     * Shared end-of-turn sequence: BETWEEN_TURNS → switch player → beginTurn (DRAW for next player).
     */
    private ActionResult completeTurn(BoardState board, CardLookup cardLookup,
                                      List<GameEvent> events) {
        board.setCurrentPhase(TurnPhase.BETWEEN_TURNS);
        events.addAll(betweenTurnsPhase.execute(board, cardLookup));

        // A poison/burn KO may have happened in between-turns. Decide victory centrally.
        if (finishedByVictory(board, events)) {
            return ActionResult.success(events);
        }
        // If the KO'd player must choose its new Active, pause mid between-turns.
        if (board.getPendingSelection() != null) {
            return ActionResult.success(events);
        }

        switchActivePlayer(board);
        events.addAll(beginTurn(board, cardLookup));

        return ActionResult.success(events);
    }

    /**
     * Resolves a pending selection produced mid-turn. Validates the owner and chosen index,
     * applies the choice, clears the {@code pendingSelection}, and resumes the paused flow.
     *
     * <p>Resume is reconstructed from the board's current phase: a KO during ATTACK resumes the
     * full end-of-turn ({@link #completeTurn}); a KO during BETWEEN_TURNS only switches player and
     * begins the next turn (between-turns already ran, re-running it would double-apply poison/burn).
     */
    public ActionResult resolveSelection(BoardState board, Long playerId,
                                         ActionRequest action, CardLookup cardLookup) {
        ActionResult selection = selectionResolver.resolve(board, playerId, action, cardLookup);
        if (!selection.isSuccess()) {
            return selection; // validation failed, board left untouched
        }

        // Selection applied (e.g. promotion). Resume the paused flow by current phase:
        // MAIN        → stay in main phase so the current player can continue their turn.
        // ATTACK      → finish the turn (between-turns + switch + beginTurn).
        // BETWEEN_TURNS → only switch + beginTurn (between-turns already ran, re-running
        //                 it would double-apply poison/burn).
        List<GameEvent> events = new ArrayList<>(selection.getEvents());
        if (board.getCurrentPhase() == TurnPhase.MAIN) {
            return ActionResult.success(events);
        }
        if (board.getCurrentPhase() == TurnPhase.ATTACK) {
            return completeTurn(board, cardLookup, events);
        }
        switchActivePlayer(board);
        events.addAll(beginTurn(board, cardLookup));
        return ActionResult.success(events);
    }

    /**
     * Runs the victory check after a KO. If the game is now over, clears any pending selection
     * (a win takes precedence over a promotion choice) and appends a precise GAME_FINISHED event.
     *
     * <p>For {@link VictoryReason#SIMULTANEOUS_KO}, {@code winnerId} is null — the payload uses
     * {@code "DRAW"} as the winnerId string so callers can identify the Sudden Death trigger.
     *
     * @return true if the game ended (including a draw that triggers Sudden Death)
     */
    private boolean finishedByVictory(BoardState board, List<GameEvent> events) {
        Optional<VictoryReason> reason = victoryChecker.checkVictoryConditions(board);
        if (reason.isEmpty()) {
            return false;
        }
        board.setPendingSelection(null);
        board.setFinishedReason(reason.get().name());
        Long winnerId = board.getWinnerId();
        // Map.of does not allow null values — use "DRAW" sentinel when there is no winner (SIMULTANEOUS_KO)
        Object winnerPayload = winnerId != null ? winnerId : "DRAW";
        events.add(GameEvent.of(GameEventType.GAME_FINISHED,
                reason.get().describe(winnerId),
                Map.of("winnerId", winnerPayload, "reason", reason.get().name())));
        return true;
    }

    // =========================================================================
    // Private: flag & player management
    // =========================================================================

    /**
     * Resets all per-turn flags and clears {@code enteredThisTurn} on every Pokémon
     * owned by the active player.
     */
    private void resetTurnFlags(BoardState board) {
        PlayerField activeField = getActiveField(board);

        // All flags → false
        activeField.setTurnFlags(TurnFlags.builder().build());

        // Clear enteredThisTurn so Pokémon played last turn can now be evolved
        if (activeField.getActivePokemon() != null) {
            activeField.getActivePokemon().setEnteredThisTurn(false);
            activeField.getActivePokemon().getRestrictions().clear();
        }
        activeField.getBench().forEach(bp -> bp.setEnteredThisTurn(false));
        activeField.getPlayerRestrictions().clear();
    }

    /** Swaps {@code currentPlayerId} to the other player. */
    private void switchActivePlayer(BoardState board) {
        Long p1 = board.getPlayer1Field().getPlayerId();
        Long p2 = board.getPlayer2Field().getPlayerId();
        board.setCurrentPlayerId(board.getCurrentPlayerId().equals(p1) ? p2 : p1);
    }

    // =========================================================================
    // Private: field helpers (reduce duplication across phase states)
    // =========================================================================

    private PlayerField getActiveField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }

    private PlayerField getOpponentField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer2Field()
                : board.getPlayer1Field();
    }
}
