package ar.edu.utn.frc.tup.piii.services.deck.validators;

import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CopyLimitValidator implements DeckValidator {

    private static final String ERROR_CODE = "TOO_MANY_COPIES";
    private static final int MAX_COPIES = 4;
    private static final String BASIC_ENERGY_SUPERTYPE = "Energy";
    private static final String BASIC_ENERGY_SUBTYPE = "Basic";

    @Override
    public List<DeckValidationError> validate(List<Card> deckCards) {
        List<DeckValidationError> errors = new ArrayList<>();

        Map<String, Long> nameCounts = deckCards.stream()
                .filter(card -> !isBasicEnergy(card))
                .collect(Collectors.groupingBy(Card::getName, Collectors.counting()));

        nameCounts.forEach((name, count) -> {
            if (count > MAX_COPIES) {
                errors.add(DeckValidationError.builder()
                        .code(ERROR_CODE)
                        .message("No puedes tener más de " + MAX_COPIES + " copias de la carta '" + name + "'. Tienes: " + count)
                        // No asignamos cardId porque es un error a nivel de nombre, aplica a múltiples IDs potencialmente.
                        .build());
            }
        });

        return errors;
    }

    private boolean isBasicEnergy(Card card) {
        return BASIC_ENERGY_SUPERTYPE.equalsIgnoreCase(card.getSupertype()) &&
               card.getSubtypes() != null &&
               card.getSubtypes().stream().anyMatch(BASIC_ENERGY_SUBTYPE::equalsIgnoreCase);
    }
}
