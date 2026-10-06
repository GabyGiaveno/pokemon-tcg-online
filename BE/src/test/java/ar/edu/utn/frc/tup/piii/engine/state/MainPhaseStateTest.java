package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
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
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MainPhaseState}.
 *
 * NOTA: MainPhaseState.handle() devuelve ActionResult, NO List&lt;GameEvent&gt; directamente.
 * PlayerField.hand es List&lt;String&gt; (card IDs), NO List&lt;CardInstanceState&gt;.
 */
class MainPhaseStateTest {

    private MainPhaseState mainPhaseState;
    private BoardState board;
    private PlayerField ownerField;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        mainPhaseState = new MainPhaseState();
        board = new BoardState();
        board.setCurrentPlayerId(1L);

        ownerField = new PlayerField();
        ownerField.setPlayerId(1L);
        ownerField.setHand(new ArrayList<>());
        ownerField.setBench(new ArrayList<>());
        ownerField.setDeck(new ArrayList<>());
        ownerField.setDiscardPile(new ArrayList<>());
        ownerField.setPrizeCards(new ArrayList<>());
        ownerField.setTurnFlags(new TurnFlags());
        ownerField.setPlayerTurnCount(2);

        board.setPlayer1Field(ownerField);
        board.setPlayer2Field(PlayerField.builder()
                .playerId(2L)
                .hand(new ArrayList<>())
                .bench(new ArrayList<>())
                .deck(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .turnFlags(new TurnFlags())
                .build());
        board.setFirstPlayerHasActed(true);

        cardLookup = Mockito.mock(CardLookup.class);
    }

    @Test
    void handle_playBasicPokemon_movesCardFromHandToBench() {
        ownerField.getHand().add("basic-1");
        when(cardLookup.findById("basic-1")).thenReturn(basicPokemon("basic-1", "Froakie", 60));

        ActionResult result = mainPhaseState.handle(request(ActionType.PLAY_BASIC_POKEMON, "basic-1"), board, 1L,
                cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertFalse(ownerField.getHand().contains("basic-1"));
        assertEquals(1, ownerField.getBench().size());
        assertEquals("basic-1", ownerField.getBench().get(0).getCardId());
        assertTrue(ownerField.getBench().get(0).isEnteredThisTurn());
    }

    @Test
    void handle_playBasicPokemon_whenBenchFull_failsWithoutRemovingCard() {
        ownerField.getHand().add("basic-1");
        for (int i = 0; i < 5; i++) {
            ownerField.getBench().add(bench("bench-" + i, 60));
        }
        when(cardLookup.findById("basic-1")).thenReturn(basicPokemon("basic-1", "Froakie", 60));

        ActionResult result = mainPhaseState.handle(request(ActionType.PLAY_BASIC_POKEMON, "basic-1"), board, 1L,
                cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("BENCH_FULL"));
        assertTrue(ownerField.getHand().contains("basic-1"));
        assertEquals(5, ownerField.getBench().size());
    }

    @Test
    void handle_evolveActive_preservesDamageAndAttachmentsAndClearsConditions() {
        ownerField.getHand().add("stage-1");
        ownerField.setActivePokemon(ActivePokemon.builder()
                .cardId("basic-1")
                .maxHp(60)
                .currentHp(40)
                .attachedEnergies(new ArrayList<>(List.of(attached("energy-1", CardType.BASIC_ENERGY))))
                .tool(attached("tool-1", CardType.POKEMON_TOOL))
                .condition(SpecialCondition.ASLEEP)
                .isBurned(true)
                .isPoisoned(true)
                .enteredThisTurn(false)
                .build());
        when(cardLookup.findById("stage-1")).thenReturn(stage1("stage-1", "Frogadier", "Froakie", 80));
        when(cardLookup.findById("basic-1")).thenReturn(basicPokemon("basic-1", "Froakie", 60));

        ActionRequest req = request(ActionType.EVOLVE_POKEMON, "stage-1");
        req.setTargetPosition("ACTIVE");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertEquals("stage-1", ownerField.getActivePokemon().getCardId());
        assertEquals(60, ownerField.getActivePokemon().getCurrentHp());
        assertEquals(1, ownerField.getActivePokemon().getAttachedEnergies().size());
        assertEquals("tool-1", ownerField.getActivePokemon().getTool().getCardId());
        assertEquals(SpecialCondition.NONE, ownerField.getActivePokemon().getCondition());
        assertFalse(ownerField.getActivePokemon().isBurned());
        assertFalse(ownerField.getActivePokemon().isPoisoned());
        assertTrue(ownerField.getDiscardPile().contains("basic-1"));
    }

    @Test
    void handle_evolveBench_invalidBenchPosition_fails() {
        ownerField.getHand().add("stage-1");
        when(cardLookup.findById("stage-1")).thenReturn(stage1("stage-1", "Frogadier", "Froakie", 80));

        ActionRequest req = request(ActionType.EVOLVE_POKEMON, "stage-1");
        req.setTargetPosition("BENCH_0");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("INVALID_TARGET"));
        assertTrue(ownerField.getHand().contains("stage-1"));
    }

    @Test
    void handle_attachEnergy_toBench_setsFlagAndStoresRealCardId() {
        ownerField.getHand().add("energy-instance");
        ownerField.getBench().add(bench("basic-1", 60));
        Card energy = energy("energy-real", "Fire Energy");
        when(cardLookup.findById("energy-instance")).thenReturn(energy);

        ActionRequest req = request(ActionType.ATTACH_ENERGY, "energy-instance");
        req.setTargetPosition("BENCH_0");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertEquals("energy-real", ownerField.getBench().get(0).getAttachedEnergies().get(0).getCardId());
        assertTrue(ownerField.getTurnFlags().isEnergyAttachedThisTurn());
        assertFalse(ownerField.getHand().contains("energy-instance"));
    }

    @Test
    void handle_attachTool_toActiveAndRejectSecondTool() {
        ownerField.getHand().add("tool-1");
        ownerField.getHand().add("tool-2");
        ownerField.setActivePokemon(active("basic-1", 60));
        when(cardLookup.findById("tool-1")).thenReturn(tool("tool-1", "Hard Charm"));
        when(cardLookup.findById("tool-2")).thenReturn(tool("tool-2", "Muscle Band"));

        ActionRequest first = request(ActionType.ATTACH_TOOL, "tool-1");
        first.setTargetPosition("ACTIVE");
        ActionRequest second = request(ActionType.ATTACH_TOOL, "tool-2");
        second.setTargetPosition("ACTIVE");

        assertTrue(mainPhaseState.handle(first, board, 1L, cardLookup).isSuccess());
        ActionResult secondResult = mainPhaseState.handle(second, board, 1L, cardLookup);

        assertFalse(secondResult.isSuccess());
        assertTrue(secondResult.getError().contains("POKEMON_ALREADY_HAS_TOOL"));
        assertEquals("tool-1", ownerField.getActivePokemon().getTool().getCardId());
        assertTrue(ownerField.getHand().contains("tool-2"));
    }

    @Test
    void handle_retreat_promotesBenchAndDiscardsRetreatCostEnergy() {
        ownerField.setActivePokemon(active("active-1", 70));
        ownerField.getActivePokemon().setAttachedEnergies(new ArrayList<>(List.of(
                attached("energy-1", CardType.BASIC_ENERGY),
                attached("energy-2", CardType.BASIC_ENERGY))));
        ownerField.getBench().add(bench("bench-1", 60));
        Card activeCard = basicPokemon("active-1", "Active", 70);
        activeCard.setRetreatCost(List.of("Colorless"));
        when(cardLookup.findById("active-1")).thenReturn(activeCard);

        ActionRequest req = request(ActionType.RETREAT, null);
        req.setBenchIndex(0);
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertEquals("bench-1", ownerField.getActivePokemon().getCardId());
        assertTrue(ownerField.getTurnFlags().isRetreatedThisTurn());
        assertTrue(ownerField.getDiscardPile().contains("energy-2"));
        assertEquals(1, ownerField.getBench().size());
        assertEquals("active-1", ownerField.getBench().get(0).getCardId());
    }

    @Test
    void handle_retreat_withoutActivePokemon_fails() {
        ownerField.getBench().add(bench("bench-1", 60));

        ActionRequest req = request(ActionType.RETREAT, null);
        req.setBenchIndex(0);
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NO_ACTIVE_POKEMON"));
    }

    @Test
    void handle_playItem_withoutParsedEffects_failsWithoutConsumingCard() {
        ownerField.getHand().add("item-1");
        Card item = item("item-1", "Potion");
        item.setParsedEffects(null);
        when(cardLookup.findById("item-1")).thenReturn(item);

        ActionResult result = mainPhaseState.handle(request(ActionType.PLAY_ITEM, "item-1"), board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("CARD_NOT_SUPPORTED"));
        assertTrue(ownerField.getHand().contains("item-1"));
        assertFalse(ownerField.getDiscardPile().contains("item-1"));
    }

    @Test
    void handle_disallowedActionInMainPhase_returnsFailure() {
        ActionResult result = mainPhaseState.handle(request(ActionType.DRAW_CARD, null), board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not valid in MAIN phase"));
    }

    @Test
    void handle_playSupporter_setsSupporterFlag() {
        ActionRequest req = new ActionRequest();
        req.setType(ActionType.PLAY_SUPPORTER);
        req.setCardId("supporter-1");

        ownerField.getHand().add("supporter-1");

        Card mockCard = new Card();
        mockCard.setSupertype("Trainer");
        mockCard.setSubtypes(List.of("Supporter"));
        mockCard.setParsedEffects("{}");
        when(cardLookup.findById("supporter-1")).thenReturn(mockCard);

        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertNotNull(result.getEvents());
        assertFalse(result.getEvents().isEmpty());
        assertTrue(ownerField.getTurnFlags().isSupporterPlayedThisTurn());
    }

    @Test
    void handle_playSupporter_twiceInSameTurn_fails() {
        ActionRequest first = new ActionRequest();
        first.setType(ActionType.PLAY_SUPPORTER);
        first.setCardId("supporter-1");

        ActionRequest second = new ActionRequest();
        second.setType(ActionType.PLAY_SUPPORTER);
        second.setCardId("supporter-2");

        ownerField.getHand().add("supporter-1");
        ownerField.getHand().add("supporter-2");

        Card supporter1 = new Card();
        supporter1.setSupertype("Trainer");
        supporter1.setSubtypes(List.of("Supporter"));
        supporter1.setName("Supporter One");
        supporter1.setParsedEffects("{}");

        Card supporter2 = new Card();
        supporter2.setSupertype("Trainer");
        supporter2.setSubtypes(List.of("Supporter"));
        supporter2.setName("Supporter Two");
        supporter2.setParsedEffects("{}");

        when(cardLookup.findById("supporter-1")).thenReturn(supporter1);
        when(cardLookup.findById("supporter-2")).thenReturn(supporter2);

        ActionResult firstResult = mainPhaseState.handle(first, board, 1L, cardLookup);
        ActionResult secondResult = mainPhaseState.handle(second, board, 1L, cardLookup);

        assertTrue(firstResult.isSuccess());
        assertFalse(secondResult.isSuccess());
        assertTrue(secondResult.getError().contains("SUPPORTER_ALREADY_PLAYED"));
    }

    @Test
    void handle_playStadium_replacesActiveStadium() {
        board.setActiveStadiumCardId("old-stadium");
        ownerField.getHand().add("stadium-1");

        ActionRequest req = new ActionRequest();
        req.setType(ActionType.PLAY_STADIUM);
        req.setCardId("stadium-1");

        Card stadium = new Card();
        stadium.setSupertype("Trainer");
        stadium.setSubtypes(List.of("Stadium"));
        stadium.setName("New Stadium");
        when(cardLookup.findById("stadium-1")).thenReturn(stadium);

        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertEquals("stadium-1", board.getActiveStadiumCardId());
        assertTrue(ownerField.getDiscardPile().contains("old-stadium"));
        assertFalse(ownerField.getHand().contains("stadium-1"));
    }

    @Test
    void handle_evolveActive_nullActive_fails() {
        ownerField.getHand().add("stage-1");
        when(cardLookup.findById("stage-1")).thenReturn(stage1("stage-1", "Frogadier", "Froakie", 80));

        ActionRequest req = request(ActionType.EVOLVE_POKEMON, "stage-1");
        req.setTargetPosition("ACTIVE");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NO_TARGET"));
    }

    @Test
    void handle_attachEnergy_toActive_succeeds() {
        ownerField.getHand().add("energy-1");
        ownerField.setActivePokemon(active("basic-1", 60));
        Card energyCard = energy("energy-1", "Fire Energy");
        when(cardLookup.findById("energy-1")).thenReturn(energyCard);

        ActionRequest req = request(ActionType.ATTACH_ENERGY, "energy-1");
        req.setTargetPosition("ACTIVE");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertEquals(1, ownerField.getActivePokemon().getAttachedEnergies().size());
        assertTrue(ownerField.getTurnFlags().isEnergyAttachedThisTurn());
        assertFalse(ownerField.getHand().contains("energy-1"));
    }

    @Test
    void handle_attachEnergy_toActiveWhenNoActive_fails() {
        ownerField.getHand().add("energy-1");
        Card energyCard = energy("energy-1", "Fire Energy");
        when(cardLookup.findById("energy-1")).thenReturn(energyCard);

        ActionRequest req = request(ActionType.ATTACH_ENERGY, "energy-1");
        req.setTargetPosition("ACTIVE");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NO_ACTIVE_POKEMON"));
    }

    @Test
    void handle_attachEnergy_toBenchInvalidPosition_fails() {
        ownerField.getHand().add("energy-1");
        Card energyCard = energy("energy-1", "Fire Energy");
        when(cardLookup.findById("energy-1")).thenReturn(energyCard);

        ActionRequest req = request(ActionType.ATTACH_ENERGY, "energy-1");
        req.setTargetPosition("BENCH_0");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("INVALID_TARGET"));
    }

    @Test
    void handle_attachTool_toBench_succeeds() {
        ownerField.getHand().add("tool-1");
        ownerField.setActivePokemon(active("basic-1", 60));
        ownerField.getBench().add(bench("basic-2", 60));
        when(cardLookup.findById("tool-1")).thenReturn(tool("tool-1", "Eviolite"));

        ActionRequest req = request(ActionType.ATTACH_TOOL, "tool-1");
        req.setTargetPosition("BENCH_0");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertNotNull(ownerField.getBench().get(0).getTool());
        assertEquals("tool-1", ownerField.getBench().get(0).getTool().getCardId());
        assertFalse(ownerField.getHand().contains("tool-1"));
    }

    @Test
    void handle_attachTool_toBenchInvalidPosition_fails() {
        ownerField.getHand().add("tool-1");
        ownerField.setActivePokemon(active("basic-1", 60));
        when(cardLookup.findById("tool-1")).thenReturn(tool("tool-1", "Eviolite"));

        ActionRequest req = request(ActionType.ATTACH_TOOL, "tool-1");
        req.setTargetPosition("BENCH_0");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("INVALID_TARGET"));
    }

    @Test
    void handle_attachTool_toActiveNoPokemon_fails() {
        ownerField.getHand().add("tool-1");
        when(cardLookup.findById("tool-1")).thenReturn(tool("tool-1", "Eviolite"));

        ActionRequest req = request(ActionType.ATTACH_TOOL, "tool-1");
        req.setTargetPosition("ACTIVE");
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NO_ACTIVE_POKEMON"));
    }

    @Test
    void handle_playSupporter_withRestrictions_fails() {
        ownerField.getHand().add("supporter-1");
        ownerField.setPlayerRestrictions(new java.util.HashSet<>(List.of("SUPPORTER")));
        Card supporter = new Card();
        supporter.setSupertype("Trainer");
        supporter.setSubtypes(List.of("Supporter"));
        when(cardLookup.findById("supporter-1")).thenReturn(supporter);

        ActionResult result = mainPhaseState.handle(
                request(ActionType.PLAY_SUPPORTER, "supporter-1"), board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("RESTRICTED_CANNOT_PLAY_SUPPORTER"));
    }

    @Test
    void handle_retreat_withStadiumEffect_modifiesCost() {
        ownerField.setActivePokemon(active("active-1", 70));
        // 2 energies for 2 retreat cost
        ownerField.getActivePokemon().setAttachedEnergies(new ArrayList<>(List.of(
                attached("energy-1", CardType.BASIC_ENERGY),
                attached("energy-2", CardType.BASIC_ENERGY))));
        ownerField.getBench().add(bench("bench-1", 60));
        Card activeCard = basicPokemon("active-1", "Active", 70);
        activeCard.setRetreatCost(List.of("Colorless", "Colorless"));
        when(cardLookup.findById("active-1")).thenReturn(activeCard);

        // Set a stadium — may or may not be in PassiveEffectRegistry, but code path is exercised
        board.setActiveStadiumCardId("xy1-999");

        ActionRequest req = request(ActionType.RETREAT, null);
        req.setBenchIndex(0);
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        // Stadium effect lookup may not modify cost, but retreat should still succeed
        assertTrue(result.isSuccess());
        assertEquals("bench-1", ownerField.getActivePokemon().getCardId());
        // 2 energies should be discarded for 2-cost retreat
        assertEquals(0, ownerField.getActivePokemon().getAttachedEnergies().size());
    }

    @Test
    void handle_useAbility_invalidIndex_fails() {
        ownerField.setActivePokemon(active("ability-1", 80));
        Card abilityCard = new Card();
        abilityCard.setId("ability-1");
        abilityCard.setName("Delphox");
        abilityCard.setSupertype("Pok\u00e9mon");
        abilityCard.setParsedEffects("""
            {"abilities":[{"name":"Mystical Fire","text":"...","parsedEffects":[
              {"type":"PASSIVE_ABILITY","trigger":"ON_PLAY","conditions":[],
               "effect":{"type":"DRAW_UNTIL_HAND_SIZE","amount":6},"stackable":false}]}]}
            """);
        when(cardLookup.findById("ability-1")).thenReturn(abilityCard);

        ActionRequest req = new ActionRequest();
        req.setType(ActionType.USE_ABILITY);
        req.setTargetPosition("ACTIVE");
        req.setAbilityIndex(99);
        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("INVALID_ABILITY_INDEX"));
    }

    @Test
    void handle_playSupporter_withoutParsedEffects_fails() {
        ownerField.getHand().add("supporter-1");
        Card supporter = new Card();
        supporter.setSupertype("Trainer");
        supporter.setSubtypes(List.of("Supporter"));
        supporter.setParsedEffects(null);
        when(cardLookup.findById("supporter-1")).thenReturn(supporter);

        ActionResult result = mainPhaseState.handle(
                request(ActionType.PLAY_SUPPORTER, "supporter-1"), board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("CARD_NOT_SUPPORTED"));
    }

    @Test
    void handle_attachEnergy_withWrongCardType_fails() {
        ActionRequest req = new ActionRequest();
        req.setType(ActionType.ATTACH_ENERGY);
        req.setCardId("trainer-1");
        req.setTargetPosition("ACTIVE");

        ownerField.getHand().add("trainer-1");

        Card trainer = new Card();
        trainer.setSupertype("Trainer");
        trainer.setSubtypes(List.of("Item"));
        trainer.setName("Trainer Item");
        when(cardLookup.findById("trainer-1")).thenReturn(trainer);

        ActionResult result = mainPhaseState.handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NOT_ENERGY_CARD"));
    }

    private ActionRequest request(ActionType type, String cardId) {
        ActionRequest req = new ActionRequest();
        req.setType(type);
        req.setCardId(cardId);
        return req;
    }

    private ActivePokemon active(String cardId, int hp) {
        return ActivePokemon.builder()
                .cardId(cardId)
                .maxHp(hp)
                .currentHp(hp)
                .attachedEnergies(new ArrayList<>())
                .condition(SpecialCondition.NONE)
                .build();
    }

    private BenchPokemon bench(String cardId, int hp) {
        return BenchPokemon.builder()
                .cardId(cardId)
                .maxHp(hp)
                .currentHp(hp)
                .attachedEnergies(new ArrayList<>())
                .build();
    }

    private AttachedCard attached(String cardId, CardType type) {
        return AttachedCard.builder().cardId(cardId).type(type).build();
    }

    private Card basicPokemon(String id, String name, int hp) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Pokémon");
        card.setSubtypes(List.of("Basic"));
        card.setHp(hp);
        return card;
    }

    private Card stage1(String id, String name, String evolvesFrom, int hp) {
        Card card = basicPokemon(id, name, hp);
        card.setSubtypes(List.of("Stage 1"));
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private Card energy(String id, String name) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Energy");
        card.setSubtypes(List.of("Basic"));
        return card;
    }

    private Card item(String id, String name) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Trainer");
        card.setSubtypes(List.of("Item"));
        return card;
    }

    private Card tool(String id, String name) {
        Card card = item(id, name);
        card.setSubtypes(List.of("Pokémon Tool"));
        return card;
    }
}
