package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ShuffleHandEffect;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.Collections;
import java.util.Map;

/**
 * Shuffles a player's hand back into their deck and draws {@code drawAmount} cards.
 * {@code target}: {@code "SELF"} (attacker) or {@code "OPPONENT"} (defender).
 * Deck order is hidden, so the shuffle is non-deterministic by design.
 */
public class ShuffleHandLogic implements EffectLogic<ShuffleHandEffect> {

    @Override
    public void execute(ShuffleHandEffect effectData, AttackContext ctx) {
        PlayerField field = "OPPONENT".equalsIgnoreCase(effectData.getTarget())
                ? ctx.getDefenderField()
                : ctx.getAttackerField();
        if (field == null || field.getDeck() == null || field.getHand() == null) return;

        if (!field.getHand().isEmpty()) {
            field.getDeck().addAll(field.getHand());
            field.getHand().clear();
        }
        Collections.shuffle(field.getDeck());

        int drawn = 0;
        for (int i = 0; i < effectData.getDrawAmount() && !field.getDeck().isEmpty(); i++) {
            field.getHand().add(field.getDeck().remove(0));
            drawn++;
        }

        ctx.addEvent(GameEvent.of(GameEventType.CARD_DRAWN,
                "Hand shuffled into deck; drew " + drawn + " card(s).",
                Map.of("playerId", field.getPlayerId(), "drawn", drawn)));
    }

    @Override
    public boolean isPostDamage() {
        return true;
    }
}
