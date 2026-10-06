package ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context passed between {@link TrainerHandler}s while resolving the play of a
 * Trainer / Item / Stadium card. Mirror of
 * {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackContext}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrainerContext {

    private BoardState board;
    private Long playerId;
    private Card card;
    private CardLookup cardLookup;

    /** Parsed trainer effects of the played card. */
    private List<TrainerEffect> parsedEffects;

    /** Set by a handler to short-circuit the remaining chain (e.g. a pending selection). */
    private boolean cancelled;

    @Builder.Default
    private List<GameEvent> events = new ArrayList<>();

    public void cancel() {
        this.cancelled = true;
    }

    public void addEvent(GameEvent event) {
        this.events.add(event);
    }
}
