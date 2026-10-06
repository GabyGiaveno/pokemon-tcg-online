package ar.edu.utn.frc.tup.piii.engine.factories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for initializing a PlayerField at the start of a match.
 * Responsible for shuffling the deck and drawing the initial hand and prize cards.
 * No Spring dependencies.
 */
public class PlayerFieldBuilder {

    private final SecureRandom secureRandom = new SecureRandom();

    private Long playerId;
    private List<Card> initialCards;
    private int prizeCardsCount = 6; // Default to 6, can be 1 for Sudden Death

    public PlayerFieldBuilder withPlayerId(Long playerId) {
        this.playerId = playerId;
        return this;
    }

    public PlayerFieldBuilder withCards(List<Card> cards) {
        if (cards == null || cards.size() != 60) {
            throw new IllegalArgumentException("A standard deck must have exactly 60 cards.");
        }
        this.initialCards = new ArrayList<>(cards);
        return this;
    }

    public PlayerFieldBuilder withSuddenDeath() {
        this.prizeCardsCount = 1;
        return this;
    }

    public PlayerField build() {
        if (playerId == null) {
            throw new IllegalStateException("playerId must be set.");
        }
        if (initialCards == null) {
            throw new IllegalStateException("initialCards must be set.");
        }

        // 1. Shuffle deck
        Collections.shuffle(initialCards, secureRandom);

        // Map to IDs
        List<String> deckIds = initialCards.stream()
                .map(Card::getId)
                .collect(Collectors.toList());

        // 2. Draw 7 for hand
        List<String> hand = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            hand.add(deckIds.remove(0)); // remove from top
        }

        // 3. Draw prize cards
        List<String> prizeCards = new ArrayList<>();
        for (int i = 0; i < prizeCardsCount; i++) {
            prizeCards.add(deckIds.remove(0)); // remove from top
        }

        // 4. Initialize field
        return PlayerField.builder()
                .playerId(playerId)
                .activePokemon(null)
                .bench(new ArrayList<>())
                .hand(hand)
                .deck(deckIds) // the remaining cards
                .prizeCards(prizeCards)
                .discardPile(new ArrayList<>())
                .turnFlags(new TurnFlags())
                .build();
    }
}
