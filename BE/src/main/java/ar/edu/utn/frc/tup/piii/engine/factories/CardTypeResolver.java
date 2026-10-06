package ar.edu.utn.frc.tup.piii.engine.factories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;

/**
 * Pure logic to resolve a Card entity into its domain CardType.
 * No Spring dependencies.
 */
public class CardTypeResolver {

    private static final String POKEMON_SUPERTYPE = "Pokémon";
    private static final String ENERGY_SUPERTYPE = "Energy";
    private static final String TRAINER_SUPERTYPE = "Trainer";

    private static final String EX_SUFFIX = "-EX";
    private static final String MEGA_PREFIX = "M ";

    public static CardType resolve(Card card) {
        String supertype = card.getSupertype();
        if (POKEMON_SUPERTYPE.equalsIgnoreCase(supertype)) {
            if (card.getName() != null) {
                if (card.getName().startsWith(MEGA_PREFIX))
                    return CardType.MEGA_POKEMON;
                if (card.getName().endsWith(EX_SUFFIX))
                    return CardType.POKEMON_EX;
            }
            if (card.getSubtypes() != null) {
                if (card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Stage 2")))
                    return CardType.STAGE2;
                if (card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Stage 1")))
                    return CardType.STAGE1;
            }
            return CardType.BASIC_POKEMON;
        }

        if (ENERGY_SUPERTYPE.equalsIgnoreCase(supertype)) {
            if (card.getSubtypes() != null
                    && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Special"))) {
                return CardType.SPECIAL_ENERGY;
            }
            return CardType.BASIC_ENERGY;
        }

        if (TRAINER_SUPERTYPE.equalsIgnoreCase(supertype) && card.getSubtypes() != null) {
            if (card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Supporter")))
                return CardType.SUPPORTER;
            if (card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Stadium")))
                return CardType.STADIUM;
            if (card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Pokémon Tool")))
                return CardType.POKEMON_TOOL;
        }

        // Default fallback for trainers
        if (TRAINER_SUPERTYPE.equalsIgnoreCase(supertype)) {
            return CardType.ITEM;
        }

        throw new IllegalArgumentException("Unknown card type for: " + card.getName());
    }
}