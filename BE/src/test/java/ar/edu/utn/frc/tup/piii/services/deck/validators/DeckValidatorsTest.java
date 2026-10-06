package ar.edu.utn.frc.tup.piii.services.deck.validators;

import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckValidatorsTest {

    @Test
    void testExactSizeValidator() {
        ExactSizeValidator validator = new ExactSizeValidator();
        List<Card> cards = new ArrayList<>();
        
        // Test size 0
        List<DeckValidationError> errors = validator.validate(cards);
        assertEquals(1, errors.size());
        assertEquals("WRONG_TOTAL", errors.get(0).getCode());

        // Test size 60
        for(int i = 0; i < 60; i++) cards.add(new Card());
        errors = validator.validate(cards);
        assertTrue(errors.isEmpty());
        
        // Test size 61
        cards.add(new Card());
        errors = validator.validate(cards);
        assertEquals(1, errors.size());
    }

    @Test
    void testBasicPokemonValidator() {
        BasicPokemonValidator validator = new BasicPokemonValidator();
        
        // Empty deck
        List<Card> cards = new ArrayList<>();
        List<DeckValidationError> errors = validator.validate(cards);
        assertEquals(1, errors.size());
        assertEquals("NO_BASIC_POKEMON", errors.get(0).getCode());

        // Deck with no pokemon
        Card energy = Card.builder().supertype("Energy").subtypes(List.of("Basic")).build();
        cards.add(energy);
        errors = validator.validate(cards);
        assertEquals(1, errors.size());

        // Deck with Basic Pokemon
        Card basicPkmn = Card.builder().supertype("Pokémon").subtypes(List.of("Basic")).build();
        cards.add(basicPkmn);
        errors = validator.validate(cards);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testCopyLimitValidator() {
        CopyLimitValidator validator = new CopyLimitValidator();
        List<Card> cards = new ArrayList<>();
        
        // Add 5 identical normal cards
        for (int i = 0; i < 5; i++) {
            cards.add(Card.builder().name("Pikachu").supertype("Pokémon").subtypes(List.of("Basic")).build());
        }
        
        List<DeckValidationError> errors = validator.validate(cards);
        assertEquals(1, errors.size());
        assertEquals("TOO_MANY_COPIES", errors.get(0).getCode());
        
        cards.clear();
        
        // Add 10 identical Basic Energies
        for (int i = 0; i < 10; i++) {
            cards.add(Card.builder().name("Fire Energy").supertype("Energy").subtypes(List.of("Basic")).build());
        }
        
        errors = validator.validate(cards);
        assertTrue(errors.isEmpty(), "Basic Energy should have no limits");
    }

}
