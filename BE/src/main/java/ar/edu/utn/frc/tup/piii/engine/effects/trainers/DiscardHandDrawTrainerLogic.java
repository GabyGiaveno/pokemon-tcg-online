package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DiscardHandDrawTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Professor Sycamore: discard your hand and draw {@code amount} cards. */
public class DiscardHandDrawTrainerLogic implements TrainerEffectLogic<DiscardHandDrawTrainerEffect> {

    @Override
    public List<GameEvent> execute(DiscardHandDrawTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        List<GameEvent> events = new ArrayList<>();
        PlayerField field = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();

        field.getDiscardPile().addAll(field.getHand());
        field.getHand().clear();

        int drawn = 0;
        for (int i = 0; i < effectData.getAmount() && !field.getDeck().isEmpty(); i++) {
            field.getHand().add(field.getDeck().remove(0));
            drawn++;
        }

        events.add(GameEvent.of(GameEventType.CARD_DRAWN,
                "Discarded hand and drew " + drawn + " card(s).",
                Map.of("playerId", playerId, "drawn", drawn)));
        return events;
    }
}
