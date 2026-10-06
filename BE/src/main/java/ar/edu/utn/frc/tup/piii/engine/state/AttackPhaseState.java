package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackResolutionChain;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.List;
import java.util.Set;

/**
 * Handles the ATTACK phase: delegates to the 7-step {@link AttackResolutionChain}.
 *
 * <p>This state is entered automatically by {@link ar.edu.utn.frc.tup.piii.engine.TurnManager}
 * when the player sends {@code USE_ATTACK} from the MAIN phase.
 * Pre-condition validation (not first turn, not paralyzed/asleep) is performed by
 * {@link ar.edu.utn.frc.tup.piii.engine.RuleValidator#validateUseAttack} before reaching here.
 *
 * <p>The {@link AttackResolutionChain} implements the full 7-step Chain of Responsibility pipeline.
 */
public class AttackPhaseState implements GamePhaseState {

    /**
     * Error codes the resolution chain emits when an attack was never legally declared
     * (no energy to pay the cost, or no valid target / attack index). These mean the action
     * was INVALID — it must be rejected so it does NOT consume the player's turn. A cancellation
     * for any other reason (e.g. a confusion-tails self-hit) is a legitimately attempted attack
     * that simply dealt no damage, and it DOES end the turn per TCG rules.
     */
    private static final Set<String> INVALID_DECLARATION_CODES =
            Set.of("INSUFFICIENT_ENERGY", "INVALID_ATTACK_CONTEXT");

    private final AttackResolutionChain attackChain;

    public AttackPhaseState() {
        this.attackChain = new AttackResolutionChain();
    }

    /** Constructor for testing: inject a custom chain. */
    public AttackPhaseState(AttackResolutionChain attackChain) {
        this.attackChain = attackChain;
    }

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.ATTACK;
    }

    @Override
    public ActionResult handle(ActionRequest action, BoardState board,
                               Long playerId, CardLookup cardLookup) {
        if (action.getType() != ActionType.USE_ATTACK) {
            return ActionResult.failure("Only USE_ATTACK is valid in ATTACK phase.");
        }

        PlayerField actorField = getActiveField(board, playerId);

        List<GameEvent> events = attackChain.resolve(action, board, playerId, cardLookup);

        // An invalid attack declaration must not burn the turn: report failure so the
        // TurnManager reverts to MAIN and the player can still act (attach energy, retreat, etc.).
        String invalidReason = invalidDeclarationMessage(events);
        if (invalidReason != null) {
            return ActionResult.failure(invalidReason);
        }

        // The attack was legitimately attempted — it ends the turn even if it dealt 0 damage.
        actorField.getTurnFlags().setAttackedThisTurn(true);
        return ActionResult.success(events);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the description of the first event flagging an invalid attack declaration,
     * or {@code null} if the attack was legally declared (it may still have dealt no damage).
     */
    private String invalidDeclarationMessage(List<GameEvent> events) {
        for (GameEvent event : events) {
            Object code = event.getData() != null ? event.getData().get("errorCode") : null;
            if (code != null && INVALID_DECLARATION_CODES.contains(code.toString())) {
                return event.getDescription();
            }
        }
        return null;
    }

    private PlayerField getActiveField(BoardState board, Long playerId) {
        return board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }
}
