package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.SearchDeckTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchDeckTrainerLogic implements TrainerEffectLogic<SearchDeckTrainerEffect> {

    @Override
    public List<GameEvent> execute(SearchDeckTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        CardLookup cardLookup = ctx.getCardLookup();

        PlayerField actorField = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();

        List<String> deck = actorField.getDeck();
        List<String> candidates = effectData.getLookAtCount() == -1
                ? new ArrayList<>(deck)
                : new ArrayList<>(deck.subList(0, Math.min(effectData.getLookAtCount(), deck.size())));

        List<String> matchingInstanceIds = new ArrayList<>();
        List<String> matchingCardIds = new ArrayList<>();

        for (String instanceId : candidates) {
            String cardId = actorField.getInstanceCardIds().get(instanceId);
            if (cardId == null) continue;
            Card card = cardLookup != null ? safeFind(cardLookup, cardId) : null;
            if (card != null && matches(card, effectData.getFilter())) {
                matchingInstanceIds.add(instanceId);
                matchingCardIds.add(cardId);
            }
        }

        if (matchingInstanceIds.isEmpty()) {
            return List.of();
        }

        SelectionType selectionType = "BENCH".equalsIgnoreCase(effectData.getDestination())
                ? SelectionType.PLACE_ON_BENCH
                : SelectionType.SEARCH_DECK;

        int count = Math.min(effectData.getTakeCount(), matchingInstanceIds.size());

        board.setPendingSelection(PendingSelection.builder()
                .type(selectionType)
                .ownerPlayerId(playerId)
                .validOptions(matchingInstanceIds)
                .revealedCardIds(matchingCardIds)
                .selectionCount(count)
                .build());

        return List.of(GameEvent.of(
                GameEventType.DECK_SEARCHED,
                "Player " + playerId + " searched deck; " + matchingInstanceIds.size() + " match(es) found.",
                Map.of("playerId", playerId, "matches", matchingInstanceIds.size())));
    }

    static boolean matches(Card card, String filter) {
        if (filter == null || "ANY".equalsIgnoreCase(filter)) return true;
        String supertype = card.getSupertype();
        List<String> subtypes = card.getSubtypes();
        return switch (filter.toUpperCase()) {
            case "POKEMON" -> "Pokémon".equals(supertype);
            case "POKEMON_EVOLUTION" -> "Pokémon".equals(supertype)
                    && subtypes != null
                    && (subtypes.contains("Stage 1") || subtypes.contains("Stage 2"));
            case "ENERGY_BASIC" -> "Energy".equals(supertype)
                    && subtypes != null
                    && subtypes.contains("Basic");
            default -> false;
        };
    }

    private Card safeFind(CardLookup lookup, String cardId) {
        try {
            return lookup.findById(cardId);
        } catch (Exception e) {
            return null;
        }
    }
}
