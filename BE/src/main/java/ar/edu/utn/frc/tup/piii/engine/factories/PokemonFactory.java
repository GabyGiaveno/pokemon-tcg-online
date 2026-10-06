package ar.edu.utn.frc.tup.piii.engine.factories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;

import java.util.ArrayList;

/**
 * Singleton Factory for creating game model instances from JPA {@link Card}
 * entities.
 * Kept independent of Spring to allow easy mocking in engine unit tests.
 *
 * <p>
 * Both factory methods set {@code enteredThisTurn = true} because a newly
 * created
 * Pokémon model represents a card that just entered play from the hand.
 */
public class PokemonFactory {

    private static final PokemonFactory INSTANCE = new PokemonFactory();

    private PokemonFactory() {
    }

    public static PokemonFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Creates an {@link ActivePokemon} from a card entity.
     * The Pokémon starts at full HP with no conditions and
     * {@code enteredThisTurn = true}.
     */
    public ActivePokemon createActivePokemon(Card card) {
        if (card.getHp() == null) {
            throw new IllegalArgumentException(
                    "Cannot create a Pokémon from a card with no HP: " + card.getName());
        }
        return ActivePokemon.builder()
                .cardId(card.getId())
                .maxHp(card.getHp())
                .currentHp(card.getHp())
                .attachedEnergies(new ArrayList<>())
                .tool(null)
                .condition(SpecialCondition.NONE)
                .isBurned(false)
                .isPoisoned(false)
                .enteredThisTurn(true) // just placed from hand
                .build();
    }

    /**
     * Creates a {@link BenchPokemon} from a card entity.
     * The Pokémon starts at full HP and {@code enteredThisTurn = true}.
     */
    public BenchPokemon createBenchPokemon(Card card) {
        if (card.getHp() == null) {
            throw new IllegalArgumentException(
                    "Cannot create a Pokémon from a card with no HP: " + card.getName());
        }
        return BenchPokemon.builder()
                .cardId(card.getId())
                .maxHp(card.getHp())
                .currentHp(card.getHp())
                .attachedEnergies(new ArrayList<>())
                .tool(null)
                .enteredThisTurn(true) // just placed from hand
                .build();
    }
}
