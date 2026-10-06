package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.factories.PokemonFactory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the SETUP phase where each player places their Active Pokémon
 * and (optionally) Bench Pokémon before the game begins.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@code SETUP_PLACE_POKEMON} — places a Basic Pokémon from hand
 *       to the position indicated by {@code targetPosition} ("ACTIVE" or "BENCH").
 *       Active must be filled before bench.</li>
 *   <li>{@code END_TURN} — confirms setup for the current player. If the
 *       other player has also confirmed, transitions to {@code ACTIVE}.</li>
 * </ol>
 */
public class SetupPhaseState implements GamePhaseState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.SETUP;
    }

    @Override
    public ActionResult handle(ActionRequest action, BoardState board,
                               Long playerId, CardLookup cardLookup) {
        if (!board.getCurrentPlayerId().equals(playerId)) {
            return ActionResult.failure("It is not your turn during SETUP.");
        }

        PlayerField field = getActiveField(board);

        return switch (action.getType()) {
            case SETUP_PLACE_POKEMON -> handleSetupPlacePokemon(action, field, cardLookup);
            case END_TURN -> handleSetupEndTurn(board, field, cardLookup);
            default -> ActionResult.failure(
                    "Action " + action.getType() + " is not valid during SETUP.");
        };
    }

    // ── Place a Basic Pokémon from hand ──────────────────────────────────

    private ActionResult handleSetupPlacePokemon(ActionRequest action,
                                                  PlayerField field,
                                                  CardLookup cardLookup) {
        String cardId = action.getCardId();

        if (!field.getHand().contains(cardId)) {
            return ActionResult.failure("Card " + cardId + " is not in your hand.");
        }

        Card card = cardLookup.findById(cardId);
        if (!"Pokémon".equals(card.getSupertype())
                || card.getSubtypes() == null
                || !card.getSubtypes().contains("Basic")) {
            return ActionResult.failure(
                    (card.getName() != null ? card.getName() : cardId) + " is not a Basic Pokémon.");
        }

        boolean wantsBench = "BENCH".equalsIgnoreCase(action.getTargetPosition());
        boolean wantsActive = action.getTargetPosition() == null
                || "ACTIVE".equalsIgnoreCase(action.getTargetPosition());

        if (wantsBench && field.getActivePokemon() == null) {
            return ActionResult.failure(
                    "You must place an Active Pokémon before placing Pokémon on the Bench.");
        }
        if (wantsActive && field.getActivePokemon() != null) {
            return ActionResult.failure(
                    "Active slot is already occupied. Use targetPosition: \"BENCH\" to place on the Bench.");
        }

        field.getHand().remove(cardId);

        if (wantsActive) {
            field.setActivePokemon(PokemonFactory.getInstance().createActivePokemon(card));
            return ActionResult.success(List.of(GameEvent.of(
                    GameEventType.POKEMON_PLAYED_TO_ACTIVE,
                    (card.getName() != null ? card.getName() : cardId) + " placed as Active Pokémon.",
                    Map.of("cardId", cardId, "pokemonName", card.getName() != null ? card.getName() : ""))));
        } else {
            if (field.getBench().size() >= 5) {
                return ActionResult.failure("Bench is full (max 5 Pokémon).");
            }
            field.getBench().add(PokemonFactory.getInstance().createBenchPokemon(card));
            return ActionResult.success(List.of(GameEvent.of(
                    GameEventType.POKEMON_PLAYED_TO_BENCH,
                    (card.getName() != null ? card.getName() : cardId) + " placed on the Bench.",
                    Map.of("cardId", cardId, "pokemonName", card.getName() != null ? card.getName() : ""))));
        }
    }

    // ── Confirm setup for the current player ────────────────────────────

    private ActionResult handleSetupEndTurn(BoardState board, PlayerField field,
                                             CardLookup cardLookup) {
        if (field.getActivePokemon() == null) {
            return ActionResult.failure(
                    "You must place at least one Basic Pokémon as your Active Pokémon before ending setup.");
        }

        // Mark current player as completed
        if (!board.getSetupCompletedPlayers().contains(field.getPlayerId())) {
            board.getSetupCompletedPlayers().add(field.getPlayerId());
        }

        Long p1Id = board.getPlayer1Field().getPlayerId();
        Long p2Id = board.getPlayer2Field().getPlayerId();
        boolean bothDone = board.getSetupCompletedPlayers().contains(p1Id)
                && board.getSetupCompletedPlayers().contains(p2Id);

        if (bothDone) {
            // Both players ready → transition to ACTIVE
            board.setMatchState(GameStatus.ACTIVE);
            board.getSetupCompletedPlayers().clear();

            List<GameEvent> events = new ArrayList<>();
            events.add(GameEvent.of(
                    GameEventType.SETUP_COMPLETE,
                    "Both players have completed setup. The game begins!",
                    Map.of()));
            return ActionResult.success(events);
        }

        // Switch to the other player for their setup
        Long p1 = board.getPlayer1Field().getPlayerId();
        Long p2 = board.getPlayer2Field().getPlayerId();
        board.setCurrentPlayerId(board.getCurrentPlayerId().equals(p1) ? p2 : p1);

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.PHASE_CHANGED,
                "Player " + board.getCurrentPlayerId() + "'s SETUP phase.",
                Map.of("playerId", board.getCurrentPlayerId(), "phase", "SETUP"))));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private PlayerField getActiveField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }
}
