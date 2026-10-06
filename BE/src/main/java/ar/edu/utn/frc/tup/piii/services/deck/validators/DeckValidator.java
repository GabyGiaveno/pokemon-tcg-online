package ar.edu.utn.frc.tup.piii.services.deck.validators;

import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.entities.Card;
import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for deck validation rules.
 */
public interface DeckValidator {
    /**
     * Validates a list of cards for a specific rule.
     * @param deckCards The list of cards in the deck.
     * @return A list of DeckValidationError, empty if valid.
     */
    List<DeckValidationError> validate(List<Card> deckCards);
}
