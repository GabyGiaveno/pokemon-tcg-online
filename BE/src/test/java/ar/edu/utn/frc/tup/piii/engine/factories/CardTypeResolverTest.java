package ar.edu.utn.frc.tup.piii.engine.factories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardTypeResolverTest {

    @Test
    void testResolvePokemon() {
        // Basic
        Card basic = Card.builder().supertype("Pokémon").subtypes(List.of("Basic")).name("Pikachu").build();
        assertEquals(CardType.BASIC_POKEMON, CardTypeResolver.resolve(basic));

        // Stage 1
        Card stage1 = Card.builder().supertype("Pokémon").subtypes(List.of("Stage 1")).name("Raichu").build();
        assertEquals(CardType.STAGE1, CardTypeResolver.resolve(stage1));

        // Stage 2
        Card stage2 = Card.builder().supertype("Pokémon").subtypes(List.of("Stage 2")).name("Charizard").build();
        assertEquals(CardType.STAGE2, CardTypeResolver.resolve(stage2));

        // EX
        Card pokemonEx = Card.builder().supertype("Pokémon").name("Venusaur-EX").build();
        assertEquals(CardType.POKEMON_EX, CardTypeResolver.resolve(pokemonEx));

        // MEGA
        Card mega = Card.builder().supertype("Pokémon").name("M Venusaur-EX").build();
        assertEquals(CardType.MEGA_POKEMON, CardTypeResolver.resolve(mega));
    }

    @Test
    void testResolveEnergy() {
        Card basicEnergy = Card.builder().supertype("Energy").subtypes(List.of("Basic")).build();
        assertEquals(CardType.BASIC_ENERGY, CardTypeResolver.resolve(basicEnergy));

        Card specialEnergy = Card.builder().supertype("Energy").subtypes(List.of("Special")).build();
        assertEquals(CardType.SPECIAL_ENERGY, CardTypeResolver.resolve(specialEnergy));
    }

    @Test
    void testResolveTrainer() {
        Card item = Card.builder().supertype("Trainer").subtypes(List.of("Item")).build();
        assertEquals(CardType.ITEM, CardTypeResolver.resolve(item));

        Card supporter = Card.builder().supertype("Trainer").subtypes(List.of("Supporter")).build();
        assertEquals(CardType.SUPPORTER, CardTypeResolver.resolve(supporter));

        Card stadium = Card.builder().supertype("Trainer").subtypes(List.of("Stadium")).build();
        assertEquals(CardType.STADIUM, CardTypeResolver.resolve(stadium));

        Card tool = Card.builder().supertype("Trainer").subtypes(List.of("Pokémon Tool")).build();
        assertEquals(CardType.POKEMON_TOOL, CardTypeResolver.resolve(tool));
        
        // Fallback to ITEM if subtypes is null
        Card genericTrainer = Card.builder().supertype("Trainer").build();
        assertEquals(CardType.ITEM, CardTypeResolver.resolve(genericTrainer));
    }
    
    @Test
    void testUnknownTypeThrowsException() {
        Card unknown = Card.builder().supertype("Alien").name("Xenomorph").build();
        assertThrows(IllegalArgumentException.class, () -> CardTypeResolver.resolve(unknown));
    }
}
