package ar.edu.utn.frc.tup.piii.services.deck.validators;

import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExactSizeValidator implements DeckValidator {

    private static final int REQUIRED_DECK_SIZE = 60;
    private static final String ERROR_CODE = "WRONG_TOTAL";

    @Override
    public List<DeckValidationError> validate(List<Card> deckCards) {
        List<DeckValidationError> errors = new ArrayList<>();
        if (deckCards.size() != REQUIRED_DECK_SIZE) {
            errors.add(DeckValidationError.builder()
                    .code(ERROR_CODE)
                    .message("El mazo debe tener exactamente " + REQUIRED_DECK_SIZE + " cartas. Tiene: " + deckCards.size())
                    .build());
        }
        return errors;
    }
}
