package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.RecycleTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecycleTrainerLogic implements TrainerEffectLogic<RecycleTrainerEffect> {

    @Override
    public List<GameEvent> execute(RecycleTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        CardLookup cardLookup = ctx.getCardLookup();

        PlayerField actorField = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();

        List<String> hand = actorField.getHand();
        if (hand.size() < effectData.getDiscardCost()) {
            return List.of();
        }

        List<GameEvent> events = new ArrayList<>();
        for (int i = 0; i < effectData.getDiscardCost(); i++) {
            String discarded = hand.remove(hand.size() - 1);
            actorField.getDiscardPile().add(discarded);
            events.add(GameEvent.of(
                    GameEventType.CARD_DISCARDED,
                    "Player " + playerId + " discarded " + discarded + " to pay Recycle cost.",
                    Map.of("playerId", playerId, "instanceId", discarded)));
        }

        List<String> discardPile = actorField.getDiscardPile();
        List<String> pokemonInstanceIds = new ArrayList<>();
        List<String> pokemonCardIds = new ArrayList<>();

        for (String instanceId : discardPile) {
            String cardId = actorField.getInstanceCardIds().get(instanceId);
            if (cardId == null) continue;
            Card card = cardLookup != null ? safeFind(cardLookup, cardId) : null;
            if (card != null && "Pokémon".equals(card.getSupertype())) {
                pokemonInstanceIds.add(instanceId);
                pokemonCardIds.add(cardId);
            }
        }

        if (pokemonInstanceIds.isEmpty()) {
            return events;
        }

        board.setPendingSelection(PendingSelection.builder()
                .type(SelectionType.CHOOSE_FROM_DISCARD)
                .ownerPlayerId(playerId)
                .validOptions(pokemonInstanceIds)
                .revealedCardIds(pokemonCardIds)
                .selectionCount(1)
                .build());

        return events;
    }

    private Card safeFind(CardLookup lookup, String cardId) {
        try {
            return lookup.findById(cardId);
        } catch (Exception e) {
            return null;
        }
    }
}
