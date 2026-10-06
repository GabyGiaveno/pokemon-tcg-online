package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-executes the BETWEEN_TURNS phase after every turn ends (END_TURN or post-attack).
 *
 * <p>Processing order (ENGINE_SPEC §4):
 * <ol>
 *   <li>Apply status-effect ticks to the current player's Active Pokémon.</li>
 *   <li>KO check for the current player's Active Pokémon (may die from poison/burn).</li>
 *   <li>Mark {@code firstPlayerHasActed = true} after the first player's first turn.</li>
 * </ol>
 *
 * <p>Player switching and the next turn's DRAW phase are handled by
 * {@link ar.edu.utn.frc.tup.piii.engine.TurnManager}, not here.
 */
public class BetweenTurnsState implements GamePhaseState {

    private final StatusEffectManager statusEffectManager;
    private final KnockoutProcessor knockoutProcessor;

    public BetweenTurnsState() {
        this.statusEffectManager = new StatusEffectManager();
        this.knockoutProcessor   = new KnockoutProcessor();
    }

    /** Constructor for testing: inject custom sub-managers. */
    public BetweenTurnsState(StatusEffectManager statusEffectManager,
                              KnockoutProcessor knockoutProcessor) {
        this.statusEffectManager = statusEffectManager;
        this.knockoutProcessor   = knockoutProcessor;
    }

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.BETWEEN_TURNS;
    }

    @Override
    public List<GameEvent> execute(BoardState board, CardLookup cardLookup) {
        List<GameEvent> events = new ArrayList<>();
        if (board.getMatchState() == GameStatus.FINISHED) return events;

        PlayerField activeField   = getActiveField(board);
        PlayerField opponentField = getOpponentField(board);

        // 1. Status-effect ticks — poison/burn apply every turn boundary for both sides;
        //    ASLEEP wake check and PARALYZED cure only fire at the end of the afflicted
        //    Pokémon's controller's own turn (isOwnerTurn flag).
        ActivePokemon active   = activeField.getActivePokemon();
        ActivePokemon oppActive = opponentField.getActivePokemon();
        if (active != null) {
            events.addAll(statusEffectManager.applyBetweenTurnEffects(active, true));
        }
        if (oppActive != null) {
            events.addAll(statusEffectManager.applyBetweenTurnEffects(oppActive, false));
        }

        // 2. KO checks for both sides (either Active may have died from poison or burn)
        if (board.getMatchState() != GameStatus.FINISHED && active != null) {
            events.addAll(knockoutProcessor.processIfKnockedOut(
                    active, activeField, opponentField, board, cardLookup));
        }
        if (board.getMatchState() != GameStatus.FINISHED && oppActive != null) {
            events.addAll(knockoutProcessor.processIfKnockedOut(
                    oppActive, opponentField, activeField, board, cardLookup));
        }

        // 3. After the first player's very first turn, unlock draw + attack for player 2
        if (!board.isFirstPlayerHasActed()) {
            board.setFirstPlayerHasActed(true);
        }

        return events;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
