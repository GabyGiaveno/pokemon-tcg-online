package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectionResolverTest {

    private SelectionResolver resolver;
    private BoardState board;
    private PlayerField ownerField;

    @BeforeEach
    void setUp() {
        resolver = new SelectionResolver(new KnockoutProcessor());

        ownerField = new PlayerField();
        ownerField.setPlayerId(1L);
        ownerField.setBench(new ArrayList<>(List.of(bench("bench-0"), bench("bench-1"))));
        ownerField.setActivePokemon(null); // just knocked out
        ownerField.setDeck(new ArrayList<>());
        ownerField.setHand(new ArrayList<>());
        ownerField.setDiscardPile(new ArrayList<>());

        PlayerField opponentField = new PlayerField();
        opponentField.setPlayerId(2L);

        board = new BoardState();
        board.setPlayer1Field(ownerField);
        board.setPlayer2Field(opponentField);
        board.setPendingSelection(PendingSelection.builder()
                .type(SelectionType.CHOOSE_ACTIVE_ON_KO)
                .ownerPlayerId(1L)
                .validOptions(List.of("bench-0", "bench-1"))
                .prompt("Choose which Benched Pokémon to promote to Active.")
                .build());
    }

    private BenchPokemon bench(String cardId) {
        BenchPokemon bp = new BenchPokemon();
        bp.setCardId(cardId);
        return bp;
    }

    private ActionRequest choose(Integer benchIndex) {
        return ActionRequest.builder()
                .type(ActionType.RESOLVE_SELECTION)
                .benchIndex(benchIndex)
                .build();
    }

    // ── REORDER_DECK helpers ──────────────────────────────────────────────────

    private PlayerField reorderOwner(List<String> deck) {
        PlayerField f = new PlayerField();
        f.setPlayerId(1L);
        f.setTurnFlags(new TurnFlags());
        f.setDeck(new ArrayList<>(deck));
        return f;
    }

    private void setPendingReorder(List<String> peeked) {
        board.setPendingSelection(PendingSelection.builder()
                .type(SelectionType.REORDER_DECK)
                .ownerPlayerId(1L)
                .validOptions(peeked)
                .prompt("Choose order.")
                .build());
    }

    private ActionRequest reorder(List<String> ordered) {
        return ActionRequest.builder()
                .type(ActionType.RESOLVE_SELECTION)
                .orderedInstanceIds(ordered)
                .build();
    }

    @Test
    void resolve_validChoice_promotesAndClearsSelection() {
        ActionResult result = resolver.resolve(board, 1L, choose(1));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getEvents().size());
        assertNull(board.getPendingSelection());                       // selection consumed
        assertNotNull(ownerField.getActivePokemon());
        assertEquals("bench-1", ownerField.getActivePokemon().getCardId()); // chosen one promoted
        assertEquals(1, ownerField.getBench().size());
        assertEquals("bench-0", ownerField.getBench().get(0).getCardId());  // the other stays
    }

    @Test
    void resolve_noPendingSelection_returnsFailure() {
        board.setPendingSelection(null);

        ActionResult result = resolver.resolve(board, 1L, choose(0));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("no selection"));
    }

    @Test
    void resolve_wrongPlayer_returnsFailure() {
        ActionResult result = resolver.resolve(board, 2L, choose(0));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("your selection"));
        assertNotNull(board.getPendingSelection());     // untouched
        assertNull(ownerField.getActivePokemon());      // not promoted
        assertEquals(2, ownerField.getBench().size());
    }

    @Test
    void resolve_indexOutOfRange_returnsFailure() {
        ActionResult result = resolver.resolve(board, 1L, choose(5));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid selection index"));
        assertNotNull(board.getPendingSelection());
        assertEquals(2, ownerField.getBench().size());
    }

    @Test
    void resolve_nullIndex_returnsFailure() {
        ActionResult result = resolver.resolve(board, 1L, choose(null));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid selection index"));
        assertNotNull(board.getPendingSelection());
    }

    // ── REORDER_DECK ──────────────────────────────────────────────────────────

    @Test
    void reorderDeck_validPermutation_reordersDeckTop() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C", "D", "E"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 1L, reorder(List.of("C", "A", "B")));

        assertTrue(result.isSuccess());
        assertNull(board.getPendingSelection());
        assertEquals(List.of("C", "A", "B", "D", "E"), owner.getDeck());
    }

    @Test
    void reorderDeck_sameOrder_deckUnchanged() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C", "D"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 1L, reorder(List.of("A", "B", "C")));

        assertTrue(result.isSuccess());
        assertEquals(List.of("A", "B", "C", "D"), owner.getDeck());
    }

    @Test
    void reorderDeck_wrongIds_returnsFailure() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C", "D"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 1L, reorder(List.of("A", "B", "X")));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("permutation"));
        // Deck must be untouched on failure.
        assertEquals(List.of("A", "B", "C", "D"), owner.getDeck());
    }

    @Test
    void reorderDeck_missingId_returnsFailure() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 1L, reorder(List.of("A", "B")));

        assertFalse(result.isSuccess());
        assertEquals(List.of("A", "B", "C"), owner.getDeck());
    }

    @Test
    void reorderDeck_nullOrderedList_returnsFailure() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 1L, reorder(null));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("orderedInstanceIds"));
    }

    @Test
    void reorderDeck_wrongPlayer_returnsFailure() {
        PlayerField owner = reorderOwner(List.of("A", "B", "C"));
        board.setPlayer1Field(owner);
        setPendingReorder(List.of("A", "B", "C"));

        ActionResult result = resolver.resolve(board, 2L, reorder(List.of("C", "A", "B")));

        assertFalse(result.isSuccess());
        assertNotNull(board.getPendingSelection());
        assertEquals(List.of("A", "B", "C"), owner.getDeck());
    }

    @Test
    void searchDeck_validSelection_movesChosenCardsToHandAndClearsSelection() {
        ownerField.setDeck(new ArrayList<>(List.of("deck-1", "deck-2", "deck-3")));
        ownerField.setHand(new ArrayList<>());
        setPending(SelectionType.SEARCH_DECK, List.of("deck-1", "deck-2", "deck-3"), 2);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("deck-2", "deck-3")));

        assertTrue(result.isSuccess());
        assertNull(board.getPendingSelection());
        assertEquals(List.of("deck-1"), ownerField.getDeck());
        assertEquals(List.of("deck-2", "deck-3"), ownerField.getHand());
    }

    @Test
    void searchDeck_tooManyCards_returnsFailureAndLeavesStateUntouched() {
        ownerField.setDeck(new ArrayList<>(List.of("deck-1", "deck-2", "deck-3")));
        ownerField.setHand(new ArrayList<>());
        setPending(SelectionType.SEARCH_DECK, List.of("deck-1", "deck-2", "deck-3"), 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("deck-1", "deck-2")));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Too many cards"));
        assertNotNull(board.getPendingSelection());
        assertEquals(List.of("deck-1", "deck-2", "deck-3"), ownerField.getDeck());
        assertTrue(ownerField.getHand().isEmpty());
    }

    @Test
    void searchDeck_invalidOption_returnsFailure() {
        setPending(SelectionType.SEARCH_DECK, List.of("deck-1"), 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("other")));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not valid options"));
    }

    @Test
    void switchPokemon_validIndex_swapsActiveWithBenchAndClearsConditions() {
        ownerField.setActivePokemon(ActivePokemon.builder()
                .instanceId("active-i")
                .cardId("active-card")
                .maxHp(70)
                .currentHp(50)
                .attachedEnergies(new ArrayList<>(List.of(attached("energy-a"))))
                .condition(SpecialCondition.PARALYZED)
                .isBurned(true)
                .isPoisoned(true)
                .build());
        ownerField.setBench(new ArrayList<>(List.of(bench("bench-0"), bench("bench-1"))));
        ownerField.getBench().get(1).setInstanceId("bench-i");
        ownerField.getBench().get(1).setAttachedEnergies(new ArrayList<>(List.of(attached("energy-b"))));
        setPending(SelectionType.SWITCH_POKEMON, List.of("bench-0", "bench-i"), 1);

        ActionResult result = resolver.resolve(board, 1L, choose(1));

        assertTrue(result.isSuccess());
        assertNull(board.getPendingSelection());
        assertEquals("bench-1", ownerField.getActivePokemon().getCardId());
        assertEquals("bench-i", ownerField.getActivePokemon().getInstanceId());
        assertEquals(SpecialCondition.NONE, ownerField.getActivePokemon().getCondition());
        assertFalse(ownerField.getActivePokemon().isBurned());
        assertFalse(ownerField.getActivePokemon().isPoisoned());
        assertEquals(2, ownerField.getBench().size());
        assertTrue(ownerField.getBench().stream().anyMatch(p -> "active-card".equals(p.getCardId())));
    }

    @Test
    void switchPokemon_missingIndex_returnsFailure() {
        setPending(SelectionType.SWITCH_POKEMON, List.of("bench-0"), 1);

        ActionResult result = resolver.resolve(board, 1L, choose(null));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("benchIndex is required"));
        assertNotNull(board.getPendingSelection());
    }

    @Test
    void switchPokemon_invalidBenchIndex_returnsFailure() {
        setPending(SelectionType.SWITCH_POKEMON, List.of("bench-0"), 1);

        ActionResult result = resolver.resolve(board, 1L, choose(99));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid bench index"));
    }

    @Test
    void placeOnBench_validChoice_resolvesCardAndPlacesPokemon() {
        ownerField.setDeck(new ArrayList<>(List.of("inst-1", "inst-2")));
        ownerField.getInstanceCardIds().put("inst-1", "xy1-1");
        setPending(SelectionType.PLACE_ON_BENCH, List.of("inst-1", "inst-2"), 1);
        Card card = basicCard("xy1-1", "Froakie", 60);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("inst-1")), id -> card);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertNull(board.getPendingSelection());
        assertFalse(ownerField.getDeck().contains("inst-1"));
        assertEquals("inst-1", ownerField.getBench().get(2).getInstanceId());
        assertEquals("xy1-1", ownerField.getBench().get(2).getCardId());
        assertEquals(60, ownerField.getBench().get(2).getCurrentHp());
    }

    @Test
    void placeOnBench_withoutLookup_returnsFailure() {
        ownerField.getInstanceCardIds().put("inst-1", "xy1-1");
        setPending(SelectionType.PLACE_ON_BENCH, List.of("inst-1"), 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("inst-1")));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Cannot resolve card"));
    }

    @Test
    void chooseFromDiscard_validChoice_revivesWithHalfHpMinimumOne() {
        ownerField.setDiscardPile(new ArrayList<>(List.of("discard-1")));
        ownerField.getInstanceCardIds().put("discard-1", "xy1-9");
        setPending(SelectionType.CHOOSE_FROM_DISCARD, List.of("discard-1"), 1);
        Card card = basicCard("xy1-9", "Tiny", 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("discard-1")), id -> card);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertFalse(ownerField.getDiscardPile().contains("discard-1"));
        BenchPokemon revived = ownerField.getBench().get(2);
        assertEquals("discard-1", revived.getInstanceId());
        assertEquals(1, revived.getCurrentHp());
    }

    @Test
    void shufflePokemonToDeck_validChoiceMovesPokemonAndAttachmentsToDeck() {
        ownerField.setDeck(new ArrayList<>(List.of("deck-1")));
        BenchPokemon target = bench("bench-card");
        target.setInstanceId("bench-inst");
        target.setAttachedEnergies(new ArrayList<>(List.of(attached("energy-inst"))));
        target.setTool(attached("tool-inst"));
        ownerField.setBench(new ArrayList<>(List.of(target)));
        setPending(SelectionType.SHUFFLE_POKEMON_TO_DECK, List.of("bench-inst"), 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("bench-inst")));

        assertTrue(result.isSuccess());
        assertTrue(ownerField.getBench().isEmpty());
        assertTrue(ownerField.getDeck().containsAll(List.of("deck-1", "bench-inst", "energy-inst", "tool-inst")));
        assertNull(board.getPendingSelection());
    }

    @Test
    void shufflePokemonToDeck_whenBenchPokemonNotFound_returnsFailure() {
        ownerField.setBench(new ArrayList<>(List.of(bench("bench-card"))));
        setPending(SelectionType.SHUFFLE_POKEMON_TO_DECK, List.of("missing-inst"), 1);

        ActionResult result = resolver.resolve(board, 1L, ordered(List.of("missing-inst")));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Bench Pokémon not found"));
        assertNotNull(board.getPendingSelection());
    }

    private void setPending(SelectionType type, List<String> validOptions, int selectionCount) {
        board.setPendingSelection(PendingSelection.builder()
                .type(type)
                .ownerPlayerId(1L)
                .validOptions(validOptions)
                .selectionCount(selectionCount)
                .prompt("Choose.")
                .build());
    }

    private ActionRequest ordered(List<String> ids) {
        return ActionRequest.builder()
                .type(ActionType.RESOLVE_SELECTION)
                .orderedInstanceIds(ids)
                .build();
    }

    private AttachedCard attached(String instanceId) {
        return AttachedCard.builder()
                .instanceId(instanceId)
                .cardId(instanceId)
                .type(CardType.BASIC_ENERGY)
                .build();
    }

    private Card basicCard(String id, String name, int hp) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Pokémon");
        card.setSubtypes(List.of("Basic"));
        card.setHp(hp);
        return card;
    }
}
