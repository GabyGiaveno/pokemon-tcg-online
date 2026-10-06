package ar.edu.utn.frc.tup.piii.services.deck.validators;

import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BasicPokemonValidator implements DeckValidator {

    private static final String ERROR_CODE = "NO_BASIC_POKEMON";
    private static final String POKEMON_SUPERTYPE = "Pokémon";
    private static final String BASIC_SUBTYPE = "Basic";

    @Override
    public List<DeckValidationError> validate(List<Card> deckCards) {
        List<DeckValidationError> errors = new ArrayList<>();
        
        boolean hasBasic = deckCards.stream().anyMatch(card -> 
            POKEMON_SUPERTYPE.equalsIgnoreCase(card.getSupertype()) && 
            card.getSubtypes() != null && 
            card.getSubtypes().stream().anyMatch(BASIC_SUBTYPE::equalsIgnoreCase)
        );

        if (!hasBasic) {
            errors.add(DeckValidationError.builder()
                    .code(ERROR_CODE)
                    .message("El mazo debe contener al menos 1 Pokémon Básico.")
                    .build());
        }
        return errors;
    }
}
