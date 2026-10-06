package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link RuleValidator} static methods. */
class RuleValidatorTest {

    private BoardState board;
    private PlayerField ownerField;

    @BeforeEach
    void setUp() {
        ownerField = new PlayerField();
        ownerField.setPlayerId(1L);
        ownerField.setHand(new ArrayList<>());
        ownerField.setBench(new ArrayList<>());
        ownerField.setTurnFlags(new TurnFlags());
        ownerField.setPlayerTurnCount(2);

        board = new BoardState();
        board.setCurrentPlayerId(1L);
        board.setFirstPlayerHasActed(true);
    }

    // -- General --------------------------------------------------------------

    @Test
    void shouldAllowCardWhenItIsInHand() {
        ownerField.getHand().add("card-1");

        assertDoesNotThrow(() -> RuleValidator.requireCardInHand("card-1", ownerField));
    }

    @Test
    void shouldRejectCardWhenItIsNotInHand() {
        InvalidActionException ex = assertInvalidAction("CARD_NOT_IN_HAND",
                () -> RuleValidator.requireCardInHand("missing", ownerField));

        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void shouldRejectNullCardAsNotInHand() {
        assertInvalidAction("CARD_NOT_IN_HAND",
                () -> RuleValidator.requireCardInHand(null, ownerField));
    }

    // -- PLAY_BASIC_POKEMON ---------------------------------------------------

    @Test
    void shouldAllowValidBasicPokemonWhenBenchHasRoom() {
        assertDoesNotThrow(() -> RuleValidator.validatePlayBasicPokemon(
                pokemon("xy1-1", "Pikachu", List.of("Basic")), ownerField));
    }

    @Test
    void shouldAllowPokemonExAsBasicPokemonWhenBenchHasRoom() {
        assertDoesNotThrow(() -> RuleValidator.validatePlayBasicPokemon(
                pokemon("xy1-2", "Venusaur-EX", List.of("EX")), ownerField));
    }

    @Test
    void shouldRejectNonBasicPokemonWhenPlayingBasicPokemon() {
        InvalidActionException ex = assertInvalidAction("NOT_BASIC_POKEMON",
                () -> RuleValidator.validatePlayBasicPokemon(
                        pokemon("xy1-3", "Raichu", List.of("Stage 1")), ownerField));

        assertTrue(ex.getMessage().contains("Raichu"));
    }

    @Test
    void shouldRejectBasicPokemonWhenBenchIsFull() {
        fillBench(5);

        assertInvalidAction("BENCH_FULL",
                () -> RuleValidator.validatePlayBasicPokemon(
                        pokemon("xy1-4", "Froakie", List.of("Basic")), ownerField));
    }

    // -- EVOLVE_POKEMON -------------------------------------------------------

    @Test
    void shouldAllowValidEvolutionOverMatchingPokemon() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");

        assertDoesNotThrow(() -> RuleValidator.validateEvolvePokemon(
                evolution,
                pokemon("xy1-1", "Pikachu", List.of("Basic")),
                false,
                ownerField,
                false));
    }

    @Test
    void shouldRejectCardThatIsNotEvolutionCard() {
        assertInvalidAction("NOT_EVOLUTION_CARD",
                () -> RuleValidator.validateEvolvePokemon(
                        pokemon("xy1-1", "Pikachu", List.of("Basic")),
                        pokemon("xy1-6", "Pichu", List.of("Basic")),
                        false,
                        ownerField,
                        false));
    }

    @Test
    void shouldRejectEvolutionOverWrongPokemon() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");

        InvalidActionException ex = assertInvalidAction("WRONG_EVOLUTION_TARGET",
                () -> RuleValidator.validateEvolvePokemon(
                        evolution,
                        pokemon("xy1-7", "Froakie", List.of("Basic")),
                        false,
                        ownerField,
                        false));

        assertTrue(ex.getMessage().contains("does not evolve from Froakie"));
    }

    @Test
    void shouldRejectEvolutionWhenGlobalFirstTurnIsBlocked() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");
        ownerField.setPlayerTurnCount(1);

        assertInvalidAction("CANNOT_EVOLVE_FIRST_TURN",
                () -> RuleValidator.validateEvolvePokemon(
                        evolution,
                        pokemon("xy1-1", "Pikachu", List.of("Basic")),
                        false,
                        ownerField,
                        true));
    }

    @Test
    void shouldAllowEvolutionOnPlayersFirstTurnWhenItIsNotGlobalFirstTurn() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");
        ownerField.setPlayerTurnCount(1);

        assertDoesNotThrow(() -> RuleValidator.validateEvolvePokemon(
                evolution,
                pokemon("xy1-1", "Pikachu", List.of("Basic")),
                false,
                ownerField,
                false));
    }

    @Test
    void shouldRejectEvolutionWhenPokemonEnteredThisTurn() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");

        assertInvalidAction("POKEMON_JUST_ENTERED",
                () -> RuleValidator.validateEvolvePokemon(
                        evolution,
                        pokemon("xy1-1", "Pikachu", List.of("Basic")),
                        true,
                        ownerField,
                        false));
    }

    @Test
    void shouldAllowMegaEvolutionOverMatchingPokemonName() {
        Card evolution = pokemon("xy1-8", "M Venusaur-EX", List.of("MEGA"));
        evolution.setEvolvesFrom("Venusaur-EX");

        assertDoesNotThrow(() -> RuleValidator.validateEvolvePokemon(
                evolution,
                pokemon("xy1-2", "Venusaur-EX", List.of("EX")),
                false,
                ownerField,
                false));
    }

    @Test
    void shouldAllowStage2EvolutionOverMatchingPokemon() {
        Card evolution = pokemon("xy1-9", "Greninja", List.of("Stage 2"));
        evolution.setEvolvesFrom("Frogadier");

        assertDoesNotThrow(() -> RuleValidator.validateEvolvePokemon(
                evolution,
                pokemon("xy1-10", "Frogadier", List.of("Stage 1")),
                false,
                ownerField,
                false));
    }

    @Test
    void shouldRejectEvolutionCardWithoutEvolvesFrom() {
        assertInvalidAction("WRONG_EVOLUTION_TARGET",
                () -> RuleValidator.validateEvolvePokemon(
                        pokemon("xy1-5", "Raichu", List.of("Stage 1")),
                        pokemon("xy1-1", "Pikachu", List.of("Basic")),
                        false,
                        ownerField,
                        false));
    }

    @Test
    void shouldAllowEvolutionOnGlobalFirstTurnAfterPlayerPassedFirstTurnCount() {
        Card evolution = pokemon("xy1-5", "Raichu", List.of("Stage 1"));
        evolution.setEvolvesFrom("Pikachu");
        ownerField.setPlayerTurnCount(2);

        assertDoesNotThrow(() -> RuleValidator.validateEvolvePokemon(
                evolution,
                pokemon("xy1-1", "Pikachu", List.of("Basic")),
                false,
                ownerField,
                true));
    }

    // -- ATTACH_ENERGY --------------------------------------------------------

    @Test
    void shouldAllowBasicEnergyAttachmentWhenNoEnergyWasAttachedThisTurn() {
        assertDoesNotThrow(() -> RuleValidator.validateAttachEnergy(
                energy("xy1-energy-1", List.of("Basic")), ownerField));
    }

    @Test
    void shouldAllowSpecialEnergyAttachmentWhenNoEnergyWasAttachedThisTurn() {
        assertDoesNotThrow(() -> RuleValidator.validateAttachEnergy(
                energy("xy1-energy-2", List.of("Special")), ownerField));
    }

    @Test
    void shouldRejectNonEnergyCardWhenAttachingEnergy() {
        assertInvalidAction("NOT_ENERGY_CARD",
                () -> RuleValidator.validateAttachEnergy(
                        trainer("xy1-trainer-1", "Potion", List.of("Item")), ownerField));
    }

    @Test
    void shouldRejectEnergyAttachmentWhenEnergyAlreadyAttachedThisTurn() {
        ownerField.getTurnFlags().setEnergyAttachedThisTurn(true);

        assertInvalidAction("ENERGY_ALREADY_ATTACHED",
                () -> RuleValidator.validateAttachEnergy(
                        energy("xy1-energy-1", List.of("Basic")), ownerField));
    }

    // -- ATTACH_TOOL ----------------------------------------------------------

    @Test
    void shouldAllowValidPokemonToolToTargetWithoutTool() {
        assertDoesNotThrow(() -> RuleValidator.validateAttachTool(
                trainer("xy1-tool-1", "Muscle Band", List.of("Pokémon Tool")), false));
    }

    @Test
    void shouldRejectCardThatIsNotPokemonTool() {
        assertInvalidAction("NOT_TOOL_CARD",
                () -> RuleValidator.validateAttachTool(
                        trainer("xy1-trainer-1", "Potion", List.of("Item")), false));
    }

    @Test
    void shouldRejectToolWhenTargetAlreadyHasTool() {
        assertInvalidAction("POKEMON_ALREADY_HAS_TOOL",
                () -> RuleValidator.validateAttachTool(
                        trainer("xy1-tool-1", "Muscle Band", List.of("Pokémon Tool")), true));
    }

    // -- RETREAT --------------------------------------------------------------

    @Test
    void shouldAllowRetreatWithEnoughEnergyAndBenchPokemon() {
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertDoesNotThrow(() -> RuleValidator.validateRetreat(
                ownerField.getActivePokemon(), 2, 0, ownerField));
    }

    @Test
    void shouldAllowRetreatWhenRestrictionsAreNull() {
        ownerField.setActivePokemon(activeWithEnergies(1));
        ownerField.getActivePokemon().setRestrictions(null);
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertDoesNotThrow(() -> RuleValidator.validateRetreat(
                ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenAlreadyRetreatedThisTurn() {
        ownerField.getTurnFlags().setRetreatedThisTurn(true);
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("ALREADY_RETREATED",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenThereIsNoActivePokemon() {
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("NO_ACTIVE_POKEMON",
                () -> RuleValidator.validateRetreat(null, 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenActivePokemonIsParalyzed() {
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getActivePokemon().setCondition(SpecialCondition.PARALYZED);
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("PARALYZED_CANNOT_RETREAT",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenActivePokemonIsAsleep() {
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getActivePokemon().setCondition(SpecialCondition.ASLEEP);
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("ASLEEP_CANNOT_RETREAT",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenBenchIndexDoesNotExist() {
        ownerField.setActivePokemon(activeWithEnergies(2));

        assertInvalidAction("INVALID_BENCH_INDEX",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenBenchIndexIsNegative() {
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("INVALID_BENCH_INDEX",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, -1, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenPokemonHasRetreatRestriction() {
        ownerField.setActivePokemon(activeWithEnergies(2));
        ownerField.getActivePokemon().setRestrictions(new HashSet<>(List.of("RETREAT")));
        ownerField.getBench().add(benchPokemon("bench-1"));

        assertInvalidAction("RESTRICTED_CANNOT_RETREAT",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 1, 0, ownerField));
    }

    @Test
    void shouldRejectRetreatWhenEnergyIsInsufficient() {
        ownerField.setActivePokemon(activeWithEnergies(1));
        ownerField.getBench().add(benchPokemon("bench-1"));

        InvalidActionException ex = assertInvalidAction("NOT_ENOUGH_ENERGY_TO_RETREAT",
                () -> RuleValidator.validateRetreat(ownerField.getActivePokemon(), 2, 0, ownerField));

        assertTrue(ex.getMessage().contains("2 Energy"));
    }

    // -- PLAY_ITEM / PLAY_SUPPORTER / PLAY_STADIUM ---------------------------

    @Test
    void shouldAllowItemCard() {
        assertDoesNotThrow(() -> RuleValidator.validatePlayItem(
                trainer("xy1-trainer-1", "Potion", List.of("Item"))));
    }

    @Test
    void shouldRejectNonItemCard() {
        assertInvalidAction("NOT_ITEM_CARD",
                () -> RuleValidator.validatePlayItem(
                        trainer("xy1-supporter-1", "Shauna", List.of("Supporter"))));
    }

    @Test
    void shouldAllowSupporterWhenNotPlayedThisTurn() {
        assertDoesNotThrow(() -> RuleValidator.validatePlaySupporter(
                trainer("xy1-supporter-1", "Shauna", List.of("Supporter")), ownerField));
    }

    @Test
    void shouldRejectSupporterWhenAlreadyPlayedThisTurn() {
        ownerField.getTurnFlags().setSupporterPlayedThisTurn(true);

        assertInvalidAction("SUPPORTER_ALREADY_PLAYED",
                () -> RuleValidator.validatePlaySupporter(
                        trainer("xy1-supporter-1", "Shauna", List.of("Supporter")), ownerField));
    }

    @Test
    void shouldRejectNonSupporterCardWhenPlayingSupporter() {
        assertInvalidAction("NOT_SUPPORTER_CARD",
                () -> RuleValidator.validatePlaySupporter(
                        trainer("xy1-trainer-1", "Potion", List.of("Item")), ownerField));
    }

    @Test
    void shouldAllowStadiumCard() {
        assertDoesNotThrow(() -> RuleValidator.validatePlayStadium(
                trainer("xy1-stadium-1", "Shadow Circle", List.of("Stadium"))));
    }

    @Test
    void shouldRejectNonStadiumCard() {
        assertInvalidAction("NOT_STADIUM_CARD",
                () -> RuleValidator.validatePlayStadium(
                        trainer("xy1-trainer-1", "Potion", List.of("Item"))));
    }

    // -- USE_ATTACK -----------------------------------------------------------

    @Test
    void shouldAllowAttackWithActivePokemonWhenNoBlockingRuleApplies() {
        ownerField.setActivePokemon(activeWithEnergies(0));

        assertDoesNotThrow(() -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldAllowAttackWhenRestrictionsAreNull() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.getActivePokemon().setRestrictions(null);

        assertDoesNotThrow(() -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldAllowAttackAfterFirstPlayerHasActedEvenWithPlayerTurnCountOne() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.setPlayerTurnCount(1);
        board.setFirstPlayerHasActed(true);

        assertDoesNotThrow(() -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldAllowAttackBeforeFirstPlayerHasActedWhenItIsNotPlayersFirstTurn() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.setPlayerTurnCount(2);
        board.setFirstPlayerHasActed(false);

        assertDoesNotThrow(() -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackWhenThereIsNoActivePokemon() {
        assertInvalidAction("NO_ACTIVE_POKEMON",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackWhenAlreadyAttackedThisTurn() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.getTurnFlags().setAttackedThisTurn(true);

        assertInvalidAction("ALREADY_ATTACKED",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackOnFirstPlayersFirstTurn() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.setPlayerTurnCount(1);
        board.setFirstPlayerHasActed(false);

        assertInvalidAction("CANNOT_ATTACK_FIRST_TURN",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackWhenActivePokemonIsParalyzed() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.getActivePokemon().setCondition(SpecialCondition.PARALYZED);

        assertInvalidAction("PARALYZED_CANNOT_ATTACK",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackWhenActivePokemonIsAsleep() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.getActivePokemon().setCondition(SpecialCondition.ASLEEP);

        assertInvalidAction("ASLEEP_CANNOT_ATTACK",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    @Test
    void shouldRejectAttackWhenPokemonHasAttackRestriction() {
        ownerField.setActivePokemon(activeWithEnergies(0));
        ownerField.getActivePokemon().setRestrictions(new HashSet<>(List.of("ATTACK")));

        assertInvalidAction("RESTRICTED_CANNOT_ATTACK",
                () -> RuleValidator.validateUseAttack(board, ownerField));
    }

    private InvalidActionException assertInvalidAction(String errorCode, ExecutableAction action) {
        InvalidActionException ex = assertThrows(InvalidActionException.class, action::execute);
        assertEquals(errorCode, ex.getErrorCode());
        return ex;
    }

    private Card pokemon(String id, String name, List<String> subtypes) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Pokémon");
        card.setSubtypes(subtypes);
        return card;
    }

    private Card trainer(String id, String name, List<String> subtypes) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Trainer");
        card.setSubtypes(subtypes);
        return card;
    }

    private Card energy(String id, List<String> subtypes) {
        Card card = new Card();
        card.setId(id);
        card.setName("Energy");
        card.setSupertype("Energy");
        card.setSubtypes(subtypes);
        return card;
    }

    private ActivePokemon activeWithEnergies(int energyCount) {
        ActivePokemon active = new ActivePokemon();
        active.setInstanceId("active-1");
        active.setCardId("xy1-active");
        active.setAttachedEnergies(new ArrayList<>());
        active.setRestrictions(new HashSet<>());
        for (int i = 0; i < energyCount; i++) {
            active.getAttachedEnergies().add(AttachedCard.builder()
                    .instanceId("energy-" + i)
                    .cardId("xy1-energy-" + i)
                    .type(CardType.BASIC_ENERGY)
                    .build());
        }
        return active;
    }

    private BenchPokemon benchPokemon(String instanceId) {
        BenchPokemon benchPokemon = new BenchPokemon();
        benchPokemon.setInstanceId(instanceId);
        benchPokemon.setCardId("xy1-bench");
        benchPokemon.setAttachedEnergies(new ArrayList<>());
        return benchPokemon;
    }

    private void fillBench(int count) {
        for (int i = 0; i < count; i++) {
            ownerField.getBench().add(benchPokemon("bench-" + i));
        }
    }

    @FunctionalInterface
    private interface ExecutableAction {
        void execute();
    }
}
