package ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.TrainerEffectParser;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;

import java.util.Collections;
import java.util.List;

/**
 * Orchestrator of the Trainer/Item/Stadium resolution Chain of Responsibility.
 * Mirror of {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackResolutionChain}.
 *
 * <p>Block 2 pipeline = {@link TrainerEffectExecutionHandler} only. Designed so that a
 * {@code TrainerSelectionHandler} (Block 3) and validation/post handlers (Block 4) can be
 * inserted without changing the existing handlers.
 */
public class TrainerResolutionChain {

    /**
     * Resolves the effects of playing a Trainer card and returns the events produced.
     * The physical card movement (discard / stadium / tool) and turn flags remain in
     * {@code MainPhaseState} — this chain only executes the parsed effects.
     */
    public List<GameEvent> resolve(Card card, BoardState board, Long playerId, CardLookup cardLookup) {
        if (card == null) return Collections.emptyList();

        TrainerContext ctx = TrainerContext.builder()
                .board(board)
                .playerId(playerId)
                .card(card)
                .cardLookup(cardLookup)
                .parsedEffects(TrainerEffectParser.parse(card.getParsedEffects()))
                .build();

        for (TrainerHandler handler : buildPipeline()) {
            if (ctx.isCancelled() && !handler.alwaysRun()) continue;
            handler.handle(ctx);
        }

        return ctx.getEvents();
    }

    private List<TrainerHandler> buildPipeline() {
        return List.of(
                new TrainerEffectExecutionHandler()
                // Block 3: new TrainerSelectionHandler() — inserted before execution
                // Block 4: validation / post handlers
        );
    }
}
