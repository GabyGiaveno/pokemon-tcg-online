package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.factories.CardTypeResolver;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

/**
 * Pure-static validator for all player actions.
 * Throws {@link InvalidActionException} with a machine-readable errorCode when a rule is violated.
 * Never mutates state — only reads and throws.
 */
public class RuleValidator {

    private RuleValidator() {}

    // -------------------------------------------------------------------------
    // General
    // -------------------------------------------------------------------------

    /** Ensures the given card is currently in the player's hand. */
    public static void requireCardInHand(String cardId, PlayerField field) {
        if (cardId == null || !field.getHand().contains(cardId)) {
            throw new InvalidActionException("CARD_NOT_IN_HAND",
                    "Card " + cardId + " is not in your hand.");
        }
    }

    // -------------------------------------------------------------------------
    // PLAY_BASIC_POKEMON
    // -------------------------------------------------------------------------

    /**
     * Validates that {@code card} is a Basic Pokémon (or Pokémon-EX, which are Basic)
     * and that the bench has room.
     */
    public static void validatePlayBasicPokemon(Card card, PlayerField field) {
        CardType type = CardTypeResolver.resolve(card);
        if (type != CardType.BASIC_POKEMON && type != CardType.POKEMON_EX) {
            throw new InvalidActionException("NOT_BASIC_POKEMON",
                    card.getName() + " is not a Basic Pokémon.");
        }
        if (field.getBench().size() >= 5) {
            throw new InvalidActionException("BENCH_FULL",
                    "Bench is full (maximum 5 Pokémon).");
        }
    }

    // -------------------------------------------------------------------------
    // EVOLVE_POKEMON
    // -------------------------------------------------------------------------

    /**
     * Validates all timing and lineage rules for evolution.
     *
     * @param evolutionCard          the card being played as the evolution
     * @param targetCard             the card currently occupying the target slot
     * @param targetEnteredThisTurn  whether the target Pokémon was placed this turn
     * @param actorField             the active player's field
     * @param isGlobalFirstTurn      true only on the very first turn of the game (first player,
     *                               first turn). The second player may evolve on their own first
     *                               turn; only the first player of the game may not.
     */
    public static void validateEvolvePokemon(Card evolutionCard, Card targetCard,
                                             boolean targetEnteredThisTurn,
                                             PlayerField actorField,
                                             boolean isGlobalFirstTurn) {
        CardType type = CardTypeResolver.resolve(evolutionCard);
        if (type != CardType.STAGE1 && type != CardType.STAGE2 && type != CardType.MEGA_POKEMON) {
            throw new InvalidActionException("NOT_EVOLUTION_CARD",
                    evolutionCard.getName() + " cannot be used to evolve a Pokémon.");
        }
        if (evolutionCard.getEvolvesFrom() == null
                || !evolutionCard.getEvolvesFrom().equalsIgnoreCase(targetCard.getName())) {
            throw new InvalidActionException("WRONG_EVOLUTION_TARGET",
                    evolutionCard.getName() + " does not evolve from " + targetCard.getName() + ".");
        }
        // Only the first player of the entire game cannot evolve on their first turn.
        // The second player may evolve on their own first turn.
        if (isGlobalFirstTurn && actorField.getPlayerTurnCount() <= 1) {
            throw new InvalidActionException("CANNOT_EVOLVE_FIRST_TURN",
                    "You cannot evolve on the first turn of the game.");
        }
        if (targetEnteredThisTurn) {
            throw new InvalidActionException("POKEMON_JUST_ENTERED",
                    "You cannot evolve a Pokémon that just entered play this turn.");
        }
    }

    // -------------------------------------------------------------------------
    // ATTACH_ENERGY
    // -------------------------------------------------------------------------

    /** Validates the card is an energy and only one energy has been attached this turn. */
    public static void validateAttachEnergy(Card card, PlayerField field) {
        CardType type = CardTypeResolver.resolve(card);
        if (type != CardType.BASIC_ENERGY && type != CardType.SPECIAL_ENERGY) {
            throw new InvalidActionException("NOT_ENERGY_CARD",
                    card.getName() + " is not an Energy card.");
        }
        if (field.getTurnFlags().isEnergyAttachedThisTurn()) {
            throw new InvalidActionException("ENERGY_ALREADY_ATTACHED",
                    "You can only attach one Energy per turn.");
        }
    }

    // -------------------------------------------------------------------------
    // ATTACH_TOOL
    // -------------------------------------------------------------------------

    /** Validates the card is a Pokémon Tool and the target has no tool already. */
    public static void validateAttachTool(Card card, boolean targetAlreadyHasTool) {
        CardType type = CardTypeResolver.resolve(card);
        if (type != CardType.POKEMON_TOOL) {
            throw new InvalidActionException("NOT_TOOL_CARD",
                    card.getName() + " is not a Pokémon Tool.");
        }
        if (targetAlreadyHasTool) {
            throw new InvalidActionException("POKEMON_ALREADY_HAS_TOOL",
                    "That Pokémon already has a Pokémon Tool attached.");
        }
    }

    // -------------------------------------------------------------------------
    // RETREAT
    // -------------------------------------------------------------------------

    /**
     * Validates all retreat conditions.
     *
     * @param active          the current Active Pokémon
     * @param retreatCostSize number of energies required to retreat
     * @param benchIndex      bench slot chosen to promote
     * @param field           the active player's field
     */
    public static void validateRetreat(ActivePokemon active, int retreatCostSize,
                                       int benchIndex, PlayerField field) {
        if (field.getTurnFlags().isRetreatedThisTurn()) {
            throw new InvalidActionException("ALREADY_RETREATED",
                    "You can only retreat once per turn.");
        }
        if (active == null) {
            throw new InvalidActionException("NO_ACTIVE_POKEMON",
                    "No Active Pokémon to retreat.");
        }
        SpecialCondition cond = active.getCondition();
        if (cond == SpecialCondition.PARALYZED) {
            throw new InvalidActionException("PARALYZED_CANNOT_RETREAT",
                    "Your Active Pokémon is Paralyzed and cannot retreat.");
        }
        if (cond == SpecialCondition.ASLEEP) {
            throw new InvalidActionException("ASLEEP_CANNOT_RETREAT",
                    "Your Active Pokémon is Asleep and cannot retreat.");
        }
        if (benchIndex < 0 || benchIndex >= field.getBench().size()) {
            throw new InvalidActionException("INVALID_BENCH_INDEX",
                    "Invalid bench index: " + benchIndex + ".");
        }
        if (active.getRestrictions() != null && active.getRestrictions().contains("RETREAT")) {
            throw new InvalidActionException("RESTRICTED_CANNOT_RETREAT",
                    "Your Active Pokémon cannot retreat this turn.");
        }
        if (active.getAttachedEnergies().size() < retreatCostSize) {
            throw new InvalidActionException("NOT_ENOUGH_ENERGY_TO_RETREAT",
                    "Retreat requires " + retreatCostSize + " Energy; "
                            + active.getAttachedEnergies().size() + " attached.");
        }
    }

    // -------------------------------------------------------------------------
    // PLAY_ITEM
    // -------------------------------------------------------------------------

    /** Validates the card is an Item trainer. */
    public static void validatePlayItem(Card card) {
        CardType type = CardTypeResolver.resolve(card);
        if (type != CardType.ITEM) {
            throw new InvalidActionException("NOT_ITEM_CARD",
                    card.getName() + " is not an Item card.");
        }
    }

    // -------------------------------------------------------------------------
    // PLAY_SUPPORTER
    // -------------------------------------------------------------------------

    /** Validates the card is a Supporter and the limit hasn't been hit this turn. */
    public static void validatePlaySupporter(Card card, PlayerField field) {
        CardType type = CardTypeResolver.resolve(card);

        if (type == CardType.SUPPORTER) {
            if (field.getTurnFlags().isSupporterPlayedThisTurn()) {
                throw new InvalidActionException("SUPPORTER_ALREADY_PLAYED",
                        "You can only play one Supporter per turn.");
            }
        } else {
            throw new InvalidActionException("NOT_SUPPORTER_CARD",
                    card.getName() + " is not a Supporter card.");
        }
    }

    // -------------------------------------------------------------------------
    // PLAY_STADIUM
    // -------------------------------------------------------------------------

    /** Validates the card is a Stadium. */
    public static void validatePlayStadium(Card card) {
        CardType type = CardTypeResolver.resolve(card);
        if (type != CardType.STADIUM) {
            throw new InvalidActionException("NOT_STADIUM_CARD",
                    card.getName() + " is not a Stadium card.");
        }
    }

    // -------------------------------------------------------------------------
    // USE_ATTACK
    // -------------------------------------------------------------------------

    /**
     * Validates that the player may declare an attack this turn.
     * Energy validation is handled inside the attack-resolution pipeline.
     */
    public static void validateUseAttack(BoardState board, PlayerField actorField) {
        ActivePokemon active = actorField.getActivePokemon();
        if (active == null) {
            throw new InvalidActionException("NO_ACTIVE_POKEMON",
                    "No Active Pokémon to attack with.");
        }
        if (actorField.getTurnFlags().isAttackedThisTurn()) {
            throw new InvalidActionException("ALREADY_ATTACKED",
                    "You can only attack once per turn.");
        }
        // First player's first turn: cannot attack
        if (!board.isFirstPlayerHasActed() && actorField.getPlayerTurnCount() == 1) {
            throw new InvalidActionException("CANNOT_ATTACK_FIRST_TURN",
                    "The first player cannot attack on their first turn.");
        }
        SpecialCondition cond = active.getCondition();
        if (cond == SpecialCondition.PARALYZED) {
            throw new InvalidActionException("PARALYZED_CANNOT_ATTACK",
                    "Your Active Pokémon is Paralyzed and cannot attack.");
        }
        if (cond == SpecialCondition.ASLEEP) {
            throw new InvalidActionException("ASLEEP_CANNOT_ATTACK",
                    "Your Active Pokémon is Asleep and cannot attack.");
        }
        if (active.getRestrictions() != null && active.getRestrictions().contains("ATTACK")) {
            throw new InvalidActionException("RESTRICTED_CANNOT_ATTACK",
                    "Your Active Pokémon cannot attack this turn.");
        }
    }
}
