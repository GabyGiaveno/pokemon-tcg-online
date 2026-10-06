package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.LookAtDeckEffect;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LookAtDeckLogic implements EffectLogic<LookAtDeckEffect> {

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(LookAtDeckEffect effect, AttackContext ctx) {
        List<String> deck = ctx.getAttackerField().getDeck();
        if (deck == null || deck.isEmpty()) {
            return;
        }

        int count = Math.min(effect.getAmount(), deck.size());
        List<String> peekedInstanceIds = new ArrayList<>(deck.subList(0, count));

        Map<String, String> lookup = ctx.getAttackerField().getInstanceCardIds();
        List<String> peekedCardIds = peekedInstanceIds.stream()
                .map(id -> lookup.getOrDefault(id, id))
                .toList();

        ctx.addEvent(GameEvent.of(
                GameEventType.DECK_PEEKED,
                "Player peeked at the top " + count + " card(s) of their deck.",
                Map.of("playerId", ctx.getAttackerPlayerId(), "instanceIds", peekedInstanceIds)));

        if (effect.isReorder()) {
            ctx.getBoard().setPendingSelection(PendingSelection.builder()
                    .type(SelectionType.REORDER_DECK)
                    .ownerPlayerId(ctx.getAttackerPlayerId())
                    .validOptions(peekedInstanceIds)
                    .revealedCardIds(peekedCardIds)
                    .prompt("Choose the order for the top " + count + " cards of your deck.")
                    .build());
        }
    }
}
