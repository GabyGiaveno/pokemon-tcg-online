package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.ShuffleHandTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Shauna (SELF) / Red Card (OPPONENT): shuffle a hand into its deck and draw {@code drawAmount}. */
public class ShuffleHandTrainerLogic implements TrainerEffectLogic<ShuffleHandTrainerEffect> {

    @Override
    public List<GameEvent> execute(ShuffleHandTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        List<GameEvent> events = new ArrayList<>();
        PlayerField self = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
        PlayerField opponent = self == board.getPlayer1Field()
                ? board.getPlayer2Field()
                : board.getPlayer1Field();

        PlayerField target = "OPPONENT".equalsIgnoreCase(effectData.getTarget()) ? opponent : self;
        if (target == null || target.getDeck() == null || target.getHand() == null) return events;

        target.getDeck().addAll(target.getHand());
        target.getHand().clear();
        Collections.shuffle(target.getDeck());

        int drawn = 0;
        for (int i = 0; i < effectData.getDrawAmount() && !target.getDeck().isEmpty(); i++) {
            target.getHand().add(target.getDeck().remove(0));
            drawn++;
        }

        events.add(GameEvent.of(GameEventType.CARD_DRAWN,
                "Shuffled hand into deck and drew " + drawn + " card(s).",
                Map.of("playerId", target.getPlayerId(), "drawn", drawn)));
        return events;
    }
}
