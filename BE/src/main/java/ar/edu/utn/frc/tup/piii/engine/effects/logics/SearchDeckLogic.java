package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.SearchDeckEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchDeckLogic implements EffectLogic<SearchDeckEffect> {

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(SearchDeckEffect effect, AttackContext ctx) {
        PlayerField actorField = ctx.getAttackerField();
        if (actorField == null || actorField.getDeck() == null || actorField.getDeck().isEmpty()) return;

        String filter = effect.getFilter() != null ? effect.getFilter().toUpperCase() : "ANY";
        String destination = effect.getDestination() != null ? effect.getDestination().toUpperCase() : "HAND";

        List<String> deck = actorField.getDeck();
        CardLookup lookup = ctx.getCardLookup();

        // Resolve instanceId → cardId using the player's map; fall back to treating the id as cardId.
        List<String> matchingInstanceIds = new ArrayList<>();
        for (String instanceId : deck) {
            String cardId = actorField.getInstanceCardIds().getOrDefault(instanceId, instanceId);
            Card card = lookup.findById(cardId);
            if (matches(card, filter)) {
                matchingInstanceIds.add(instanceId);
            }
        }

        if (matchingInstanceIds.isEmpty()) return;

        if ("ATTACH".equalsIgnoreCase(destination)) {
            // Auto-attach the first matching energy to the attacker's Active — no player choice.
            String instanceId = matchingInstanceIds.get(0);
            String cardId = actorField.getInstanceCardIds().getOrDefault(instanceId, instanceId);
            deck.remove(instanceId);
            ActivePokemon active = ctx.getAttackerPokemon();
            if (active != null) {
                if (active.getAttachedEnergies() == null) active.setAttachedEnergies(new ArrayList<>());
                active.getAttachedEnergies().add(new AttachedCard(instanceId, cardId, null));
                ctx.addEvent(GameEvent.of(
                        GameEventType.ENERGY_ATTACHED,
                        cardId + " attached to " + active.getCardId() + " from deck.",
                        Map.of("cardId", cardId, "targetCardId", active.getCardId())));
            }
            return;
        }

        // HAND destination — ask player to choose up to selectionCount cards.
        if (ctx.getBoard().getPendingSelection() != null) return; // another selection in progress

        int count = Math.min(effect.getAmount() > 0 ? effect.getAmount() : 1, matchingInstanceIds.size());
        List<String> revealedCardIds = matchingInstanceIds.stream()
                .map(id -> actorField.getInstanceCardIds().getOrDefault(id, id))
                .toList();

        ctx.getBoard().setPendingSelection(PendingSelection.builder()
                .type(SelectionType.SEARCH_DECK)
                .ownerPlayerId(actorField.getPlayerId())
                .validOptions(matchingInstanceIds)
                .revealedCardIds(revealedCardIds)
                .selectionCount(count)
                .prompt("Search your deck and choose up to " + count + " card(s) to take.")
                .build());

        ctx.addEvent(GameEvent.of(
                GameEventType.DECK_PEEKED,
                "Player " + actorField.getPlayerId() + " is searching their deck.",
                Map.of("playerId", actorField.getPlayerId(), "selectionCount", count)));
    }

    /** Returns true if the card matches the given filter string. */
    static boolean matches(Card card, String filter) {
        if (card == null || "ANY".equals(filter)) return true;

        // POKEMON_<TYPE> — e.g. POKEMON_GRASS
        if (filter.startsWith("POKEMON_")) {
            if (!"Pokémon".equalsIgnoreCase(card.getSupertype()) && !"Pokemon".equalsIgnoreCase(card.getSupertype())) return false;
            String type = filter.substring("POKEMON_".length());
            return card.getTypes() != null && card.getTypes().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(type));
        }

        // ENERGY_<TYPE> — e.g. ENERGY_FIRE, ENERGY_BASIC
        if (filter.startsWith("ENERGY_")) {
            if (!"Energy".equalsIgnoreCase(card.getSupertype())) return false;
            String energyType = filter.substring("ENERGY_".length());
            if ("BASIC".equalsIgnoreCase(energyType)) {
                return card.getSubtypes() != null && card.getSubtypes().stream()
                        .anyMatch(s -> s.equalsIgnoreCase("Basic"));
            }
            return card.getTypes() != null && card.getTypes().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(energyType));
        }

        // SUPPORTER — Trainer subtype
        if ("SUPPORTER".equals(filter)) {
            return card.getSubtypes() != null && card.getSubtypes().stream()
                    .anyMatch(s -> s.equalsIgnoreCase("Supporter"));
        }

        return false;
    }
}
