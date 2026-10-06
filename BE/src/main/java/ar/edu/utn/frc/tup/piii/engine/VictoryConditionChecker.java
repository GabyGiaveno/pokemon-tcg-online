package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.VictoryReason;

import java.util.Optional;

/**
 * Single source of truth for the prize / no-Pokémon victory conditions.
 *
 * <p>A Knockout ({@link KnockoutProcessor}) only mutates the board and <em>enables</em> a win;
 * it does NOT decide the game. The orchestrator ({@link TurnManager}) calls this checker right
 * after each KO. If a condition holds, the board is marked {@link GameStatus#FINISHED}, the
 * winner is recorded, and the matching {@link VictoryReason} is returned so the orchestrator can
 * emit a precise {@code GAME_FINISHED} event.
 *
 * <p>Deck-out is handled separately by
 * {@link ar.edu.utn.frc.tup.piii.engine.state.DrawPhaseState}.
 */
public class VictoryConditionChecker {

    /**
     * Evaluates the victory conditions against the current board. If one holds, sets
     * {@code matchState = FINISHED} and {@code winnerId}, and returns the reason.
     *
     * <p>Simultaneous KO (Sudden Death trigger): if BOTH players have simultaneously
     * exhausted their prize cards, neither player wins outright — {@link VictoryReason#SIMULTANEOUS_KO}
     * is returned with no winner set. The caller is responsible for initiating Sudden Death.
     *
     * @return the {@link VictoryReason} if the game is now over, or empty otherwise
     */
    public Optional<VictoryReason> checkVictoryConditions(BoardState board) {
        // Skip during SETUP — game hasn't started yet, no victory possible
        if (board.getMatchState() == GameStatus.SETUP) {
            return Optional.empty();
        }

        boolean p1NoPokemon = hasNoPokemon(board.getPlayer1Field());
        boolean p2NoPokemon = hasNoPokemon(board.getPlayer2Field());
        boolean p1PrizesDone = board.getPlayer1Field().getPrizeCards().isEmpty();
        boolean p2PrizesDone = board.getPlayer2Field().getPrizeCards().isEmpty();

        // Simultaneous KO: both prize lists empty at the same time → Sudden Death
        if (p1PrizesDone && p2PrizesDone) {
            return finishDraw(board, VictoryReason.SIMULTANEOUS_KO);
        }

        // Simultaneous no-Pokémon (both benches empty and no active) → Sudden Death
        if (p1NoPokemon && p2NoPokemon) {
            return finishDraw(board, VictoryReason.SIMULTANEOUS_KO);
        }

        // No Pokémon left (no Active and empty Bench → cannot continue)
        if (p1NoPokemon) {
            return finish(board, board.getPlayer2Field().getPlayerId(), VictoryReason.NO_POKEMON_LEFT);
        }
        if (p2NoPokemon) {
            return finish(board, board.getPlayer1Field().getPlayerId(), VictoryReason.NO_POKEMON_LEFT);
        }

        // All prizes taken
        if (p1PrizesDone) {
            return finish(board, board.getPlayer1Field().getPlayerId(), VictoryReason.ALL_PRIZES_TAKEN);
        }
        if (p2PrizesDone) {
            return finish(board, board.getPlayer2Field().getPlayerId(), VictoryReason.ALL_PRIZES_TAKEN);
        }

        return Optional.empty();
    }

    private Optional<VictoryReason> finish(BoardState board, Long winnerId, VictoryReason reason) {
        board.setMatchState(GameStatus.FINISHED);
        board.setWinnerId(winnerId);
        return Optional.of(reason);
    }

    /**
     * Marks the board as FINISHED with no winner — signals a draw (Sudden Death trigger).
     * The caller must detect {@link VictoryReason#SIMULTANEOUS_KO} and start Sudden Death
     * instead of ending the game.
     */
    private Optional<VictoryReason> finishDraw(BoardState board, VictoryReason reason) {
        board.setMatchState(GameStatus.FINISHED);
        board.setWinnerId(null);
        return Optional.of(reason);
    }

    private boolean hasNoPokemon(PlayerField field) {
        return field.getActivePokemon() == null && field.getBench().isEmpty();
    }
}
