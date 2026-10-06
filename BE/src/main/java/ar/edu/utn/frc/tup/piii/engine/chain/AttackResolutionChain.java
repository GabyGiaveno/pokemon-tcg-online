package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.ConfusionCheckHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler;
import ar.edu.utn.frc.tup.piii.engine.passive.PassiveEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.EnergyValidationHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.ModifierHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.PostDamageHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.PreAttackHandler;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.SelectionsHandler;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Orchestrator of the 7-step Chain of Responsibility attack resolution pipeline.
 *
 * <h3>Pipeline order</h3>
 * <ol>
 *   <li>{@link EnergyValidationHandler} — sufficient energy?</li>
 *   <li>{@link ConfusionCheckHandler} — confusion coin flip</li>
 *   <li>{@link SelectionsHandler} — resolve target Pokémon</li>
 *   <li>{@link PreAttackHandler} — pre-attack discards / coin-flip bonuses (stub)</li>
 *   <li>{@link ModifierHandler} — tool / stadium flat damage adjustments (stub)</li>
 *   <li>{@link DamageApplicationHandler} — base → Weakness → Resistance → apply HP</li>
 *   <li>{@link PostDamageHandler} — conditions + KO detection (always runs)</li>
 * </ol>
 *
 * <h3>Cancellation contract</h3>
 * Any handler may call {@link AttackContext#cancelAttack()} to signal that the attack did not
 * land. All subsequent handlers are skipped <em>unless</em> they override
 * {@link AttackHandler#alwaysRun()} to return {@code true}.
 *
 * <p>This class is constructed once by {@link ar.edu.utn.frc.tup.piii.engine.state.AttackPhaseState}
 * and reused for every attack resolution. Thread-safety is not required because each match
 * runs in a single thread.
 *
 * <p>A second constructor accepting {@link Random} and {@link KnockoutProcessor} is provided
 * for deterministic unit tests.
 */
public class AttackResolutionChain {

    private final Random random;
    private final KnockoutProcessor knockoutProcessor;

    /** Production constructor — uses a shared Random and a new KnockoutProcessor. */
    public AttackResolutionChain() {
        this.random = new Random();
        this.knockoutProcessor = new KnockoutProcessor();
    }

    /** Test constructor — accepts injectable collaborators for determinism. */
    public AttackResolutionChain(Random random, KnockoutProcessor knockoutProcessor) {
        this.random = random;
        this.knockoutProcessor = knockoutProcessor;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves an attack action end-to-end and returns the ordered list of events produced.
     *
     * @param action           the player action containing the chosen attack index
     * @param board            current board state (mutated: HP, conditions, KO)
     * @param attackerPlayerId ID of the player who declared the attack
     * @param cardLookup       Spring-free bridge to the card data access layer
     * @return ordered list of {@link GameEvent} objects representing everything that happened
     */
    public List<GameEvent> resolve(ActionRequest action,
                                   BoardState board,
                                   Long attackerPlayerId,
                                   CardLookup cardLookup) {
        AttackContext ctx = buildContext(action, board, attackerPlayerId, cardLookup);
        if (ctx == null) {
            return List.of(GameEvent.of(
                    GameEventType.ATTACK_DECLARED,
                    "Attack failed: no valid attack target or invalid attack index.",
                    Map.of("errorCode", "INVALID_ATTACK_CONTEXT")));
        }

        for (AttackHandler handler : buildPipeline()) {
            if (ctx.isAttackCancelled() && !handler.alwaysRun()) continue;
            handler.handle(ctx);
        }

        return Collections.unmodifiableList(ctx.getEvents());
    }

    // ── Context construction ──────────────────────────────────────────────────

    /**
     * Builds the mutable {@link AttackContext} from the incoming action and board state.
     * Returns {@code null} if the attack cannot be resolved (missing active Pokémon or
     * out-of-range attack index).
     */
    private AttackContext buildContext(ActionRequest action,
                                       BoardState board,
                                       Long attackerPlayerId,
                                       CardLookup cardLookup) {
        PlayerField attackerField = resolveField(board, attackerPlayerId);
        PlayerField defenderField = resolveOpponentField(board, attackerPlayerId);

        ActivePokemon attackerPokemon = attackerField.getActivePokemon();
        ActivePokemon defenderPokemon = defenderField.getActivePokemon();

        if (attackerPokemon == null || defenderPokemon == null) return null;

        Card attackerCard = cardLookup.findById(attackerPokemon.getCardId());
        Card defenderCard = cardLookup.findById(defenderPokemon.getCardId());

        if (attackerCard == null) return null;

        List<AttackData> attacks = AttackParser.parse(attackerCard.getAttacks(), attackerCard.getParsedEffects());
        int attackIndex = action.getAttackIndex() != null ? action.getAttackIndex() : 0;
        if (attackIndex < 0 || attackIndex >= attacks.size()) return null;

        return AttackContext.builder()
                .board(board)
                .attackerPlayerId(attackerPlayerId)
                .cardLookup(cardLookup)
                .attackerField(attackerField)
                .defenderField(defenderField)
                .attackerPokemon(attackerPokemon)
                .defenderPokemon(defenderPokemon)
                .attackerCard(attackerCard)
                .defenderCard(defenderCard)
                .attackData(attacks.get(attackIndex))
                .build();
    }

    // ── Pipeline factory ──────────────────────────────────────────────────────

    /**
     * Constructs the ordered list of 7 handlers.
     * Called once per {@link #resolve} invocation to keep each pipeline run stateless.
     */
    private List<AttackHandler> buildPipeline() {
        return List.of(
                new EnergyValidationHandler(),
                new ConfusionCheckHandler(random),
                new SelectionsHandler(),
                new PreAttackHandler(),
                new ModifierHandler(),
                new DamageApplicationHandler(PassiveEffectRegistry.getInstance()),
                new PostDamageHandler(knockoutProcessor,
                        new ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityTriggerResolver(random)));
    }

    // ── Field helpers ─────────────────────────────────────────────────────────

    private PlayerField resolveField(BoardState board, Long playerId) {
        return board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }

    private PlayerField resolveOpponentField(BoardState board, Long playerId) {
        return board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer2Field()
                : board.getPlayer1Field();
    }
}
