package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DrawCardsTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawCardsTrainerLogic implements TrainerEffectLogic<DrawCardsTrainerEffect> {

    @Override
    public List<GameEvent> execute(DrawCardsTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        List<GameEvent> events = new ArrayList<>();
        PlayerField field = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
        
        int amount = effectData.getAmount();
        int drawn = 0;

        for (int i = 0; i < amount; i++) {
            if (!field.getDeck().isEmpty()) {
                field.getHand().add(field.getDeck().remove(0));
                drawn++;
            }
        }

        if (drawn > 0) {
            events.add(GameEvent.of(
                    GameEventType.CARD_DRAWN,
                    "Player drew " + drawn + " cards.",
                    Map.of("playerId", playerId, "amount", drawn)
            ));
        }

        return events;
    }
}
