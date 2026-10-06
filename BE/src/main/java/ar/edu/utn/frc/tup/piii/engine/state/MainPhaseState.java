package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.factories.CardTypeResolver;
import ar.edu.utn.frc.tup.piii.engine.factories.PokemonFactory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerResolutionChain;

/**
 * Handles all player-driven actions during the MAIN phase.
 *
 * <p>Supported actions:
 * PLAY_BASIC_POKEMON, EVOLVE_POKEMON, ATTACH_ENERGY, ATTACH_TOOL,
 * RETREAT, PLAY_ITEM, PLAY_SUPPORTER, PLAY_STADIUM.
 *
 * <p>USE_ATTACK, END_TURN, and CONCEDE are intercepted by
 * {@link ar.edu.utn.frc.tup.piii.engine.TurnManager} before reaching this class.
 */
public class MainPhaseState implements GamePhaseState {

    private final TrainerResolutionChain trainerChain = new TrainerResolutionChain();

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.MAIN;
    }

    @Override
    public ActionResult handle(ActionRequest action, BoardState board,
                               Long playerId, CardLookup cardLookup) {
        PlayerField actorField = getActiveField(board);
        try {
            return switch (action.getType()) {
                case PLAY_BASIC_POKEMON -> handlePlayBasicPokemon(action, actorField, cardLookup);
                case EVOLVE_POKEMON     -> handleEvolvePokemon(action, actorField, board, cardLookup);
                case ATTACH_ENERGY      -> handleAttachEnergy(action, actorField, cardLookup);
                case ATTACH_TOOL        -> handleAttachTool(action, actorField, cardLookup);
                case RETREAT            -> handleRetreat(action, actorField, board, cardLookup);
                case PLAY_ITEM          -> handlePlayItem(action, actorField, board, cardLookup);
                case PLAY_SUPPORTER     -> handlePlaySupporter(action, actorField, board, cardLookup);
                case PLAY_STADIUM       -> handlePlayStadium(action, actorField, board, cardLookup);
                case USE_ABILITY        -> handleUseAbility(action, actorField, cardLookup);
                default -> ActionResult.failure(
                        "Action " + action.getType() + " is not valid in MAIN phase.");
            };
        } catch (InvalidActionException e) {
            return ActionResult.failure("[" + e.getErrorCode() + "] " + e.getMessage());
        }
    }

    // =========================================================================
    // Action handlers
    // =========================================================================

    private ActionResult handlePlayBasicPokemon(ActionRequest action,
                                                 PlayerField field,
                                                 CardLookup cardLookup) {
        String cardId = action.getCardId();
        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);
        RuleValidator.validatePlayBasicPokemon(card, field);

        field.getHand().remove(cardId);
        BenchPokemon bench = PokemonFactory.getInstance().createBenchPokemon(card);
        // enteredThisTurn = true set by PokemonFactory
        field.getBench().add(bench);

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.POKEMON_PLAYED_TO_BENCH,
                safeCardName(card) + " played to bench.",
                Map.of("cardId", cardId, "pokemonName", safeCardName(card)))));
    }

    private ActionResult handleEvolvePokemon(ActionRequest action,
                                              PlayerField field,
                                              BoardState board,
                                              CardLookup cardLookup) {
        String cardId        = action.getCardId();
        String targetPosition = action.getTargetPosition();

        RuleValidator.requireCardInHand(cardId, field);
        Card evolutionCard = cardLookup.findById(cardId);

        boolean targetIsActive = "ACTIVE".equals(targetPosition);
        int     benchIdx       = parseBenchIndex(targetPosition);

        // Resolve the target's current card info
        boolean       targetEnteredThisTurn;
        Card          targetCard;
        String        targetCardId;
        int           oldMaxHp;
        int           oldCurrentHp;
        List<AttachedCard> attachedEnergies;
        AttachedCard  tool;

        if (targetIsActive) {
            ActivePokemon target = field.getActivePokemon();
            if (target == null)
                throw new InvalidActionException("NO_TARGET", "No Active Pokémon to evolve.");
            targetEnteredThisTurn = target.isEnteredThisTurn();
            targetCardId          = target.getCardId();
            targetCard            = cardLookup.findById(targetCardId);
            oldMaxHp              = target.getMaxHp();
            oldCurrentHp          = target.getCurrentHp();
            attachedEnergies      = target.getAttachedEnergies();
            tool                  = target.getTool();
        } else {
            if (benchIdx < 0 || benchIdx >= field.getBench().size())
                throw new InvalidActionException("INVALID_TARGET", "Invalid bench position.");
            BenchPokemon target = field.getBench().get(benchIdx);
            targetEnteredThisTurn = target.isEnteredThisTurn();
            targetCardId          = target.getCardId();
            targetCard            = cardLookup.findById(targetCardId);
            oldMaxHp              = target.getMaxHp();
            oldCurrentHp          = target.getCurrentHp();
            attachedEnergies      = target.getAttachedEnergies();
            tool                  = target.getTool();
        }

        // isGlobalFirstTurn: true only for the very first player of the game on their first turn.
        // The second player may evolve on their own turn 1.
        boolean isGlobalFirstTurn = !board.isFirstPlayerHasActed();
        RuleValidator.validateEvolvePokemon(evolutionCard, targetCard,
                targetEnteredThisTurn, field, isGlobalFirstTurn);

        // Damage counters carry over; HP scales to the new max
        int damageTaken = oldMaxHp - oldCurrentHp;
        int newHp       = Math.max(0, evolutionCard.getHp() - damageTaken);

        field.getHand().remove(cardId);
        // Previous stage card is discarded
        // TODO: track full evolution stack so all stages are discarded on KO
        field.getDiscardPile().add(targetCardId);

        if (targetIsActive) {
            ActivePokemon evolved = ActivePokemon.builder()
                    .cardId(evolutionCard.getId())
                    .maxHp(evolutionCard.getHp())
                    .currentHp(newHp)
                    .attachedEnergies(new ArrayList<>(attachedEnergies))
                    .tool(tool)
                    .condition(SpecialCondition.NONE)   // conditions cleared on evolve
                    .isBurned(false)
                    .isPoisoned(false)
                    .enteredThisTurn(false)
                    .build();
            field.setActivePokemon(evolved);
        } else {
            BenchPokemon evolved = BenchPokemon.builder()
                    .cardId(evolutionCard.getId())
                    .maxHp(evolutionCard.getHp())
                    .currentHp(newHp)
                    .attachedEnergies(new ArrayList<>(attachedEnergies))
                    .tool(tool)
                    .enteredThisTurn(false)
                    .build();
            field.getBench().set(benchIdx, evolved);
        }

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.POKEMON_EVOLVED,
                targetCard.getName() + " evolved into " + evolutionCard.getName() + ".",
                Map.of("fromCardId", targetCardId, "toCardId", evolutionCard.getId()))));
    }

    private ActionResult handleAttachEnergy(ActionRequest action,
                                             PlayerField field,
                                             CardLookup cardLookup) {
        String cardId        = action.getCardId();
        String targetPosition = action.getTargetPosition();

        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);
        RuleValidator.validateAttachEnergy(card, field);

        CardType     type       = CardTypeResolver.resolve(card);
        // Store the REAL cardId (request may carry an instanceId): attached cards must stay
        // resolvable through CardLookup after a persistence round-trip (tasks 5.5)
        String realCardId = card.getId() != null ? card.getId() : cardId;
        AttachedCard energyCard = AttachedCard.builder().cardId(realCardId).type(type).build();

        if ("ACTIVE".equals(targetPosition)) {
            if (field.getActivePokemon() == null)
                throw new InvalidActionException("NO_ACTIVE_POKEMON", "No Active Pokémon.");
            field.getActivePokemon().getAttachedEnergies().add(energyCard);
        } else {
            int idx = parseBenchIndex(targetPosition);
            if (idx < 0 || idx >= field.getBench().size())
                throw new InvalidActionException("INVALID_TARGET", "Invalid bench position.");
            field.getBench().get(idx).getAttachedEnergies().add(energyCard);
        }

        field.getHand().remove(cardId);
        field.getTurnFlags().setEnergyAttachedThisTurn(true);

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.ENERGY_ATTACHED,
                card.getName() + " attached to " + targetPosition + ".",
                Map.of("cardId", cardId, "target", targetPosition))));
    }

    private ActionResult handleAttachTool(ActionRequest action,
                                           PlayerField field,
                                           CardLookup cardLookup) {
        String cardId        = action.getCardId();
        String targetPosition = action.getTargetPosition();

        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);

        boolean alreadyHasTool;
        if ("ACTIVE".equals(targetPosition)) {
            if (field.getActivePokemon() == null)
                throw new InvalidActionException("NO_ACTIVE_POKEMON", "No Active Pokémon.");
            alreadyHasTool = field.getActivePokemon().getTool() != null;
        } else {
            int idx = parseBenchIndex(targetPosition);
            if (idx < 0 || idx >= field.getBench().size())
                throw new InvalidActionException("INVALID_TARGET", "Invalid bench position.");
            alreadyHasTool = field.getBench().get(idx).getTool() != null;
        }

        RuleValidator.validateAttachTool(card, alreadyHasTool);

        CardType     type     = CardTypeResolver.resolve(card);
        String realCardId = card.getId() != null ? card.getId() : cardId;
        AttachedCard toolCard = AttachedCard.builder().cardId(realCardId).type(type).build();

        if ("ACTIVE".equals(targetPosition)) {
            field.getActivePokemon().setTool(toolCard);
        } else {
            int idx = parseBenchIndex(targetPosition);
            field.getBench().get(idx).setTool(toolCard);
        }

        field.getHand().remove(cardId);

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.TOOL_ATTACHED,
                card.getName() + " attached to " + targetPosition + ".",
                Map.of("cardId", cardId, "target", targetPosition))));
    }

    private ActionResult handleRetreat(ActionRequest action,
                                        PlayerField field,
                                        BoardState board,
                                        CardLookup cardLookup) {
        int newActiveIndex = action.getBenchIndex() != null ? action.getBenchIndex() : -1;
        ActivePokemon active = field.getActivePokemon();

        // Early null check: validateRetreat also guards this, but we check first so the
        // static analyzer knows 'active' is non-null in the lines that follow.
        if (active == null) {
            throw new InvalidActionException("NO_ACTIVE_POKEMON", "No Active Pokémon to retreat.");
        }

        Card activeCard = cardLookup.findById(active.getCardId());
        int retreatCostSize = (activeCard.getRetreatCost() != null)
                ? activeCard.getRetreatCost().size() : 0;

        String stadiumId = board.getActiveStadiumCardId();
        if (stadiumId != null) {
            ar.edu.utn.frc.tup.piii.engine.passive.StadiumEffect stadiumEffect =
                    ar.edu.utn.frc.tup.piii.engine.passive.PassiveEffectRegistry.getInstance().getStadiumEffect(stadiumId);
            if (stadiumEffect != null) {
                retreatCostSize = stadiumEffect.modifyRetreatCost(retreatCostSize, active, cardLookup);
            }
        }

        RuleValidator.validateRetreat(active, retreatCostSize, newActiveIndex, field);

        // Discard retreat-cost energies from the end of the list
        List<AttachedCard> energies = active.getAttachedEnergies();
        for (int i = 0; i < retreatCostSize; i++) {
            AttachedCard discarded = energies.remove(energies.size() - 1);
            field.getDiscardPile().add(discarded.getCardId());
        }

        // Move current Active to bench — all conditions are cleared (spec §3.2)
        BenchPokemon retreated = BenchPokemon.builder()
                .cardId(active.getCardId())
                .maxHp(active.getMaxHp())
                .currentHp(active.getCurrentHp())
                .attachedEnergies(new ArrayList<>(active.getAttachedEnergies()))
                .tool(active.getTool())
                .enteredThisTurn(false)
                .build();

        BenchPokemon promoted = field.getBench().remove(newActiveIndex);
        field.getBench().add(retreated);

        // Promote chosen bench Pokémon to Active (conditions stay NONE — bench has none)
        ActivePokemon newActive = ActivePokemon.builder()
                .cardId(promoted.getCardId())
                .maxHp(promoted.getMaxHp())
                .currentHp(promoted.getCurrentHp())
                .attachedEnergies(new ArrayList<>(promoted.getAttachedEnergies()))
                .tool(promoted.getTool())
                .condition(SpecialCondition.NONE)
                .isBurned(false)
                .isPoisoned(false)
                .enteredThisTurn(false) // was already in play on the bench
                .build();

        field.setActivePokemon(newActive);
        field.getTurnFlags().setRetreatedThisTurn(true);

        return ActionResult.success(List.of(GameEvent.of(
                GameEventType.POKEMON_RETREATED,
                active.getCardId() + " retreated; " + promoted.getCardId() + " promoted to Active.",
                Map.of("retreatedCardId", active.getCardId(),
                        "promotedCardId", promoted.getCardId()))));
    }

    private ActionResult handlePlayItem(ActionRequest action,
                                         PlayerField field,
                                         BoardState board,
                                         CardLookup cardLookup) {
        String cardId = action.getCardId();
        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);
        RuleValidator.validatePlayItem(card);

        // Continuous ability guard: an opposing Active with an Item lock (Forest's Curse)
        // blocks Items for as long as it stays Active (design D9).
        if (ar.edu.utn.frc.tup.piii.engine.effects.abilities.ContinuousAbilityQuery
                .itemsLockedBy(getOpponentField(board), cardLookup)) {
            throw new InvalidActionException("ITEMS_LOCKED",
                    "An opposing ability prevents you from playing Item cards.");
        }

        // Reject unsupported cards before consuming them — parsedEffects null means no logic registered.
        if (card.getParsedEffects() == null || card.getParsedEffects().isBlank()) {
            throw new InvalidActionException("CARD_NOT_SUPPORTED",
                    safeCardName(card) + " is not supported yet.");
        }

        List<GameEvent> effectEvents = trainerChain.resolve(card, board, field.getPlayerId(), cardLookup);
        
        field.getHand().remove(cardId);
        field.getDiscardPile().add(cardId);

        List<GameEvent> result = new ArrayList<>();
        result.add(GameEvent.of(
                GameEventType.ITEM_PLAYED,
                safeCardName(card) + " played.",
                Map.of("cardId", cardId, "cardName", safeCardName(card))));
        result.addAll(effectEvents);

        return ActionResult.success(result);

    }


    private ActionResult handlePlaySupporter(ActionRequest action,
                                              PlayerField field,
                                              BoardState board,
                                              CardLookup cardLookup) {
        String cardId = action.getCardId();
        if (field.getPlayerRestrictions() != null && field.getPlayerRestrictions().contains("SUPPORTER")) {
            throw new InvalidActionException("RESTRICTED_CANNOT_PLAY_SUPPORTER",
                    "You cannot play Supporter cards this turn.");
        }
        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);
        RuleValidator.validatePlaySupporter(card, field);

        if (card.getParsedEffects() == null || card.getParsedEffects().isBlank()) {
            throw new InvalidActionException("CARD_NOT_SUPPORTED",
                    safeCardName(card) + " is not supported yet.");
        }

        List<GameEvent> effectEvents = trainerChain.resolve(card, board, field.getPlayerId(), cardLookup);

        field.getHand().remove(cardId);
        field.getDiscardPile().add(cardId);
        
        field.getTurnFlags().setSupporterPlayedThisTurn(true);

        List<GameEvent> result = new ArrayList<>();
        String cardName = card.getName() != null ? card.getName() : "";
        result.add(GameEvent.of(
                GameEventType.SUPPORTER_PLAYED,
                cardName + " played.",
                Map.of("cardId", cardId, "cardName", cardName)));
        result.addAll(effectEvents);

        return ActionResult.success(result);
    }

    private ActionResult handlePlayStadium(ActionRequest action,
                                            PlayerField field,
                                            BoardState board,
                                            CardLookup cardLookup) {
        String cardId = action.getCardId();
        RuleValidator.requireCardInHand(cardId, field);
        Card card = cardLookup.findById(cardId);
        RuleValidator.validatePlayStadium(card);

        if (board.getActiveStadiumCardId() != null) {
            field.getDiscardPile().add(board.getActiveStadiumCardId());
        }
        board.setActiveStadiumCardId(card.getId() != null ? card.getId() : cardId);

        List<GameEvent> effectEvents = trainerChain.resolve(card, board, field.getPlayerId(), cardLookup);
        field.getHand().remove(cardId);

        List<GameEvent> result = new ArrayList<>();
        result.add(GameEvent.of(
                GameEventType.STADIUM_PLAYED,
                safeCardName(card) + " played.",
                Map.of("cardId", cardId, "cardName", safeCardName(card))));
        result.addAll(effectEvents);

        return ActionResult.success(result);
    }

    private ActionResult handleUseAbility(ActionRequest action,
                                           PlayerField field,
                                           CardLookup cardLookup) {
        String position = action.getTargetPosition() != null ? action.getTargetPosition() : "ACTIVE";
        boolean sourceIsActive = "ACTIVE".equals(position);

        // Resolve the source Pokémon's cardId
        String sourceCardId;
        ActivePokemon evaluated = null;
        if (sourceIsActive) {
            if (field.getActivePokemon() == null)
                throw new InvalidActionException("NO_ACTIVE_POKEMON", "No Active Pokémon.");
            evaluated = field.getActivePokemon();
            if (evaluated.getRestrictions() != null && evaluated.getRestrictions().contains("ABILITY")) {
                throw new InvalidActionException("RESTRICTED_CANNOT_USE_ABILITY",
                        "Your Active Pokémon cannot use abilities this turn.");
            }
            sourceCardId = evaluated.getCardId();
        } else {
            int idx = parseBenchIndex(position);
            if (idx < 0 || idx >= field.getBench().size())
                throw new InvalidActionException("INVALID_TARGET", "Invalid bench position.");
            sourceCardId = field.getBench().get(idx).getCardId();
        }

        Card sourceCard = cardLookup.findById(sourceCardId);
        var abilities = ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityParser
                .parse(sourceCard != null ? sourceCard.getParsedEffects() : null);
        if (abilities.isEmpty()) {
            throw new InvalidActionException("NO_ABILITY",
                    safeCardName(sourceCard) + " has no ability to use.");
        }

        int abilityIdx = action.getAbilityIndex() != null ? action.getAbilityIndex() : 0;
        if (abilityIdx < 0 || abilityIdx >= abilities.size()) {
            throw new InvalidActionException("INVALID_ABILITY_INDEX",
                    "Invalid ability index: " + abilityIdx + ".");
        }
        var ability = abilities.get(abilityIdx);

        // "Once during your turn" — positional key within the turn (design D7)
        String usageKey = position + "#" + ability.getName();
        if (field.getTurnFlags().getAbilitiesUsedThisTurn().contains(usageKey)) {
            throw new InvalidActionException("ABILITY_ALREADY_USED",
                    ability.getName() + " was already used this turn.");
        }

        var passives = ability.passiveEffects();
        if (passives.isEmpty()) {
            throw new InvalidActionException("NO_ABILITY",
                    ability.getName() + " has no executable effect.");
        }

        List<GameEvent> events = new ArrayList<>();
        var activationResolver =
                new ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityActivationResolver();
        for (var passive : passives) {
            boolean conditionsHold = ar.edu.utn.frc.tup.piii.engine.effects.abilities
                    .AbilityConditionEvaluator.allHold(
                            passive.getConditions(), sourceIsActive, evaluated, cardLookup);
            if (!conditionsHold) {
                throw new InvalidActionException("ABILITY_CONDITIONS_NOT_MET",
                        ability.getName() + " conditions are not met.");
            }
            events.addAll(activationResolver.activate(field, ability.getName(), passive.getEffect()));
        }

        field.getTurnFlags().getAbilitiesUsedThisTurn().add(usageKey);
        return ActionResult.success(events);
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private PlayerField getActiveField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
    }

    private PlayerField getOpponentField(BoardState board) {
        return board.getPlayer1Field().getPlayerId().equals(board.getCurrentPlayerId())
                ? board.getPlayer2Field()
                : board.getPlayer1Field();
    }

    /**
     * Parses {@code "BENCH_X"} → int X. Returns -1 for any non-bench or malformed value.
     */
    private int parseBenchIndex(String targetPosition) {
        if (targetPosition != null && targetPosition.startsWith("BENCH_")) {
            try {
                return Integer.parseInt(targetPosition.substring(6));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /** Safely returns the card name, defaulting to empty string. */
    private static String safeCardName(Card card) {
        return card != null && card.getName() != null ? card.getName() : "";
    }
}
