package ar.edu.utn.frc.tup.piii.engine.mappers;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.state.MainPhaseState;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import ar.edu.utn.frc.tup.piii.models.game.state.CardInstanceState;
import ar.edu.utn.frc.tup.piii.models.game.state.CardZone;
import ar.edu.utn.frc.tup.piii.models.game.state.GameBoardState;
import ar.edu.utn.frc.tup.piii.models.game.state.PlayerBoardState;
import ar.edu.utn.frc.tup.piii.models.game.state.PokemonInPlayState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Fase 5 ampliada (tasks 5.3, 5.5-5.7): the remaining persistence holes of the team's
 * option-B mapper. Every flag/id the engine relies on must survive the
 * domain → DB → domain round-trip, and attached cards must store REAL cardIds so they
 * stay resolvable after persistence.
 */
class EngineStateMapperRoundTripTest {

    private BoardState domain;
    private GameBoardState db;

    @BeforeEach
    void setUp() {
        domain = BoardState.builder()
                .matchState(GameStatus.ACTIVE)
                .currentPhase(TurnPhase.MAIN)
                .currentPlayerId(1L)
                .turnNumber(1)
                .player1Field(domainField(1L))
                .player2Field(domainField(2L))
                .build();

        db = GameBoardState.builder()
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L)
                .turnNumber(1)
                .phase(TurnPhase.MAIN)
                .matchState(GameStatus.ACTIVE)
                .player1State(PlayerBoardState.builder().playerId(1L).build())
                .player2State(PlayerBoardState.builder().playerId(2L).build())
                .build();
    }

    private PlayerField domainField(Long playerId) {
        return PlayerField.builder()
                .playerId(playerId)
                .hand(new ArrayList<>()).deck(new ArrayList<>())
                .prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .bench(new ArrayList<>())
                .turnFlags(TurnFlags.builder().build())
                .build();
    }

    private BoardState roundTrip(BoardState source) {
        EngineStateMapper.updateDbState(source, db);
        return EngineStateMapper.toDomainState(db);
    }

    @Test
    void toDomainState_nullInput_returnsNull() {
        assertNull(EngineStateMapper.toDomainState(null));
    }

    @Test
    void updateDbState_nullInputs_areIgnored() {
        assertDoesNotThrow(() -> EngineStateMapper.updateDbState(null, db));
        assertDoesNotThrow(() -> EngineStateMapper.updateDbState(domain, null));
    }

    @Test
    void toDomainState_mapsDefaultsFlagsZonesPokemonAndPendingSelection() {
        PendingSelection pending = PendingSelection.builder()
                .type(SelectionType.SEARCH_DECK)
                .ownerPlayerId(1L)
                .validOptions(List.of("deck-i"))
                .selectionCount(1)
                .build();
        GameBoardState source = GameBoardState.builder()
                .currentPlayerId(1L)
                .phase(TurnPhase.MAIN)
                .firstPlayerHasActed(true)
                .activeStadiumCardId("stadium-i")
                .pendingSelection(pending)
                .player1State(PlayerBoardState.builder()
                        .playerId(1L)
                        .activePokemon(PokemonInPlayState.builder()
                                .instanceId("active-i")
                                .cardId("active-card")
                                .maxHp(80)
                                .currentHp(50)
                                .condition(SpecialCondition.CONFUSED.name())
                                .burned(true)
                                .poisoned(true)
                                .enteredThisTurn(true)
                                .damageProtected(true)
                                .restrictions(List.of("RETREAT"))
                                .attachedEnergies(List.of(cardInstance("energy-i", "energy-card")))
                                .attachedTools(List.of(cardInstance("tool-i", "tool-card")))
                                .build())
                        .bench(List.of(PokemonInPlayState.builder()
                                .instanceId("bench-i")
                                .cardId("bench-card")
                                .maxHp(60)
                                .currentHp(40)
                                .attachedEnergies(List.of(cardInstance("bench-energy-i", "energy-card")))
                                .attachedTools(List.of(cardInstance("bench-tool-i", "tool-card")))
                                .enteredThisTurn(true)
                                .build()))
                        .hand(List.of(cardInstance("hand-i", "hand-card")))
                        .deck(List.of(cardInstance("deck-i", "deck-card")))
                        .prizeCards(List.of(cardInstance("prize-i", "prize-card")))
                        .discardPile(List.of(cardInstance("discard-i", "discard-card")))
                        .hasAttachedEnergyThisTurn(true)
                        .hasPlayedSupporterThisTurn(true)
                        .retreatedThisTurn(true)
                        .attackedThisTurn(true)
                        .abilitiesUsedThisTurn(Set.of("ACTIVE#Mystical Fire"))
                        .playerTurnCount(null)
                        .build())
                .player2State(PlayerBoardState.builder().playerId(2L).build())
                .build();

        BoardState mapped = EngineStateMapper.toDomainState(source);

        assertEquals(GameStatus.SETUP, mapped.getMatchState());
        assertEquals(1, mapped.getTurnNumber());
        assertTrue(mapped.isFirstPlayerHasActed());
        assertSame(pending, mapped.getPendingSelection());
        assertEquals("stadium-i", mapped.getActiveStadiumCardId());
        assertEquals("active-i", mapped.getPlayer1Field().getActivePokemon().getInstanceId());
        assertEquals(SpecialCondition.CONFUSED, mapped.getPlayer1Field().getActivePokemon().getCondition());
        assertTrue(mapped.getPlayer1Field().getActivePokemon().isBurned());
        assertTrue(mapped.getPlayer1Field().getActivePokemon().isPoisoned());
        assertTrue(mapped.getPlayer1Field().getActivePokemon().isDamageProtected());
        assertTrue(mapped.getPlayer1Field().getActivePokemon().getRestrictions().contains("RETREAT"));
        assertEquals("energy-card", mapped.getPlayer1Field().getActivePokemon().getAttachedEnergies().get(0).getCardId());
        assertEquals("tool-card", mapped.getPlayer1Field().getActivePokemon().getTool().getCardId());
        assertEquals("bench-card", mapped.getPlayer1Field().getBench().get(0).getCardId());
        assertEquals(List.of("hand-i"), mapped.getPlayer1Field().getHand());
        assertEquals(List.of("deck-i"), mapped.getPlayer1Field().getDeck());
        assertEquals(List.of("prize-i"), mapped.getPlayer1Field().getPrizeCards());
        assertEquals(List.of("discard-i"), mapped.getPlayer1Field().getDiscardPile());
        assertEquals("active-card", mapped.getPlayer1Field().getInstanceCardIds().get("active-i"));
        assertEquals("bench-card", mapped.getPlayer1Field().getInstanceCardIds().get("bench-i"));
        assertEquals(0, mapped.getPlayer1Field().getPlayerTurnCount());
        assertTrue(mapped.getPlayer1Field().getTurnFlags().isEnergyAttachedThisTurn());
        assertTrue(mapped.getPlayer1Field().getTurnFlags().isSupporterPlayedThisTurn());
        assertTrue(mapped.getPlayer1Field().getTurnFlags().isRetreatedThisTurn());
        assertTrue(mapped.getPlayer1Field().getTurnFlags().isAttackedThisTurn());
        assertTrue(mapped.getPlayer1Field().getTurnFlags().getAbilitiesUsedThisTurn().contains("ACTIVE#Mystical Fire"));
    }

    @Test
    void updateDbState_activeMatchWithoutFinishedReason_clearsPreviousReason() {
        db.setFinishedReason("OLD_REASON");
        domain.setMatchState(GameStatus.ACTIVE);

        EngineStateMapper.updateDbState(domain, db);

        assertNull(db.getFinishedReason());
    }

    @Test
    void updateDbState_finishedWithoutReason_usesFallbackMarker() {
        domain.setMatchState(GameStatus.FINISHED);
        domain.setFinishedReason(null);

        EngineStateMapper.updateDbState(domain, db);

        assertEquals("ENGINE_FINISHED", db.getFinishedReason());
    }

    @Test
    void updateDbState_writesActiveBenchAttachmentsFlagsAndRebuildsCardZones() {
        PlayerField p1 = domain.getPlayer1Field();
        p1.setPlayerTurnCount(3);
        p1.setHand(new ArrayList<>(List.of("hand-i")));
        p1.setDeck(new ArrayList<>(List.of("deck-i")));
        p1.setPrizeCards(new ArrayList<>(List.of("prize-i")));
        p1.setDiscardPile(new ArrayList<>(List.of("discard-i")));
        p1.getInstanceCardIds().put("hand-i", "hand-card");
        p1.getInstanceCardIds().put("deck-i", "deck-card");
        p1.getInstanceCardIds().put("prize-i", "prize-card");
        p1.getInstanceCardIds().put("discard-i", "discard-card");
        p1.setActivePokemon(ActivePokemon.builder()
                .cardId("active-card")
                .maxHp(80)
                .currentHp(30)
                .condition(SpecialCondition.PARALYZED)
                .isBurned(true)
                .isPoisoned(true)
                .enteredThisTurn(true)
                .damageProtected(true)
                .attachedEnergies(new ArrayList<>(List.of(attached("energy-card", CardType.BASIC_ENERGY))))
                .tool(attached("tool-card", CardType.POKEMON_TOOL))
                .build());
        p1.getActivePokemon().getRestrictions().add("ATTACK");
        p1.setBench(new ArrayList<>(List.of(BenchPokemon.builder()
                .cardId("bench-card")
                .maxHp(60)
                .currentHp(45)
                .attachedEnergies(new ArrayList<>(List.of(attached("bench-energy-card", CardType.BASIC_ENERGY))))
                .tool(attached("bench-tool-card", CardType.POKEMON_TOOL))
                .enteredThisTurn(true)
                .build())));
        p1.getTurnFlags().setEnergyAttachedThisTurn(true);
        p1.getTurnFlags().setSupporterPlayedThisTurn(true);
        p1.getTurnFlags().setRetreatedThisTurn(true);
        p1.getTurnFlags().setAttackedThisTurn(true);
        p1.getTurnFlags().setAbilitiesUsedThisTurn(new java.util.HashSet<>(Set.of("ACTIVE#Mystical Fire")));

        EngineStateMapper.updateDbState(domain, db);

        PlayerBoardState mapped = db.getPlayer1State();
        assertEquals(3, mapped.getPlayerTurnCount());
        assertTrue(mapped.isHasAttachedEnergyThisTurn());
        assertTrue(mapped.isHasPlayedSupporterThisTurn());
        assertTrue(mapped.isRetreatedThisTurn());
        assertTrue(mapped.isAttackedThisTurn());
        assertTrue(mapped.getAbilitiesUsedThisTurn().contains("ACTIVE#Mystical Fire"));
        assertEquals("active-card", mapped.getActivePokemon().getCardId());
        assertEquals(30, mapped.getActivePokemon().getCurrentHp());
        assertEquals(SpecialCondition.PARALYZED.name(), mapped.getActivePokemon().getCondition());
        assertTrue(mapped.getActivePokemon().isBurned());
        assertTrue(mapped.getActivePokemon().isPoisoned());
        assertTrue(mapped.getActivePokemon().isDamageProtected());
        assertEquals(List.of("ATTACK"), mapped.getActivePokemon().getRestrictions());
        assertEquals("energy-card", mapped.getActivePokemon().getAttachedEnergies().get(0).getCardId());
        assertEquals("tool-card", mapped.getActivePokemon().getAttachedTools().get(0).getCardId());
        assertEquals("bench-card", mapped.getBench().get(0).getCardId());
        assertEquals("bench-energy-card", mapped.getBench().get(0).getAttachedEnergies().get(0).getCardId());
        assertEquals("bench-tool-card", mapped.getBench().get(0).getAttachedTools().get(0).getCardId());
        assertEquals("hand-card", mapped.getHand().get(0).getCardId());
        assertEquals("deck-card", mapped.getDeck().get(0).getCardId());
        assertEquals("prize-card", mapped.getPrizeCards().get(0).getCardId());
        assertEquals("discard-card", mapped.getDiscardPile().get(0).getCardId());
        assertEquals(CardZone.DISCARD, mapped.getDiscardPile().get(0).getZone());
    }

    // ── 5.6 firstPlayerHasActed ──────────────────────────────────────────────

    @Test
    void firstPlayerHasActed_survivesRoundTrip() {
        // turnNumber=1 would derive 'false' with the old turnNumber>1 heuristic —
        // the real value must be persisted, not derived.
        domain.setFirstPlayerHasActed(true);
        domain.setTurnNumber(1);

        BoardState restored = roundTrip(domain);

        assertTrue(restored.isFirstPlayerHasActed());
    }

    @Test
    void firstPlayerHasActed_falseAlsoSurvives() {
        domain.setFirstPlayerHasActed(false);
        domain.setTurnNumber(5); // old heuristic would wrongly derive 'true'

        BoardState restored = roundTrip(domain);

        assertFalse(restored.isFirstPlayerHasActed());
    }

    // ── 5.7 abilitiesUsedThisTurn ────────────────────────────────────────────

    @Test
    void abilitiesUsedThisTurn_survivesRoundTrip() {
        domain.getPlayer1Field().getTurnFlags()
                .setAbilitiesUsedThisTurn(new java.util.HashSet<>(Set.of("ACTIVE#Mystical Fire")));

        BoardState restored = roundTrip(domain);

        assertTrue(restored.getPlayer1Field().getTurnFlags()
                .getAbilitiesUsedThisTurn().contains("ACTIVE#Mystical Fire"));
    }

    // ── setupCompletedPlayers (campo del equipo, hueco encontrado por el smoke E2E) ──

    @Test
    void setupCompletedPlayers_survivesRoundTrip() {
        // Sin esto la partida queda atrapada en SETUP: cada request arranca con la
        // lista vacía y nunca quedan "ambos confirmados".
        domain.getSetupCompletedPlayers().add(1L);

        BoardState restored = roundTrip(domain);

        assertEquals(java.util.List.of(1L), restored.getSetupCompletedPlayers());
    }

    // ── 5.3 finishedReason por motivo ────────────────────────────────────────

    @Test
    void finishedReason_fromDomain_isPersistedVerbatim() {
        domain.setMatchState(GameStatus.FINISHED);
        domain.setWinnerId(2L);
        domain.setFinishedReason("ALL_PRIZES_TAKEN");

        EngineStateMapper.updateDbState(domain, db);

        assertEquals("ALL_PRIZES_TAKEN", db.getFinishedReason());
    }

    @Test
    void concede_setsConcedeReasonOnDomain() {
        // The orchestrator must record WHY the game ended, not a generic marker.
        domain.getPlayer1Field().setActivePokemon(ActivePokemon.builder()
                .cardId("xy1-1").maxHp(60).currentHp(60)
                .attachedEnergies(new ArrayList<>()).build());
        domain.getPlayer2Field().setActivePokemon(ActivePokemon.builder()
                .cardId("xy1-2").maxHp(60).currentHp(60)
                .attachedEnergies(new ArrayList<>()).build());
        // prizes non-empty so the victory checker doesn't fire on its own
        domain.getPlayer1Field().getPrizeCards().add("p1");
        domain.getPlayer2Field().getPrizeCards().add("p2");

        TurnManager turnManager = new TurnManager();
        ActionRequest concede = ActionRequest.builder().type(ActionType.CONCEDE).build();
        CardLookup lookup = Mockito.mock(CardLookup.class);

        turnManager.processAction(concede, domain, 1L, lookup);

        assertEquals(GameStatus.FINISHED, domain.getMatchState());
        assertEquals("CONCEDE", domain.getFinishedReason());
    }

    // ── 5.5 attached cards store REAL cardIds ────────────────────────────────

    @Test
    void attachEnergy_storesRealCardId_notTheRequestInstanceId() {
        String instanceId = "3f8a2c1b-aaaa-bbbb-cccc-1234567890ab"; // what HTTP sends
        Card energyCard = new Card();
        energyCard.setId("xy1-132"); // the real card behind that instance
        energyCard.setName("Lightning Energy");
        energyCard.setSupertype("Energy");
        energyCard.setSubtypes(List.of("Basic"));

        CardLookup lookup = Mockito.mock(CardLookup.class);
        when(lookup.findById(instanceId)).thenReturn(energyCard);

        PlayerField field = domainField(1L);
        field.getHand().add(instanceId);
        field.setActivePokemon(ActivePokemon.builder()
                .cardId("xy1-1").maxHp(60).currentHp(60)
                .attachedEnergies(new ArrayList<>()).build());

        BoardState board = BoardState.builder()
                .matchState(GameStatus.ACTIVE)
                .currentPhase(TurnPhase.MAIN)
                .currentPlayerId(1L)
                .player1Field(field)
                .player2Field(domainField(2L))
                .build();

        ActionRequest attach = ActionRequest.builder()
                .type(ActionType.ATTACH_ENERGY)
                .cardId(instanceId)
                .targetPosition("ACTIVE")
                .build();

        new MainPhaseState().handle(attach, board, 1L, lookup);

        assertEquals(1, field.getActivePokemon().getAttachedEnergies().size());
        assertEquals("xy1-132",
                field.getActivePokemon().getAttachedEnergies().get(0).getCardId(),
                "AttachedCard must store the REAL cardId so it stays resolvable after persistence");
    }

    // ── Bug #5: KO'd Pokémon must keep its real cardId in discard (no orphan UUID) ──

    @Test
    void knockedOutPokemon_keepsRealCardIdInDiscard_notOrphanUuid() {
        // When a Pokémon is knocked out, KnockoutProcessor pushes its instanceId to the discard
        // pile and clears the Active slot. By the time the card-list write-back runs, the Active
        // slot is already null, so the Pokémon is no longer indexed in `existing`. Without the
        // fix, rebuildZone fabricated a CardInstanceState with cardId == instanceId (an orphan
        // UUID). The fix recovers the real cardId from the load-time instanceId→cardId map.
        String pokeInstanceId = "poke-1-uuid";
        db.getPlayer1State().setDiscardPile(new ArrayList<>());
        db.getPlayer1State().setActivePokemon(PokemonInPlayState.builder()
                .instanceId(pokeInstanceId)
                .cardId("xy1-100")
                .currentHp(0).maxHp(90)
                .attachedEnergies(new ArrayList<>())
                .attachedTools(new ArrayList<>())
                .build());

        // Load into the domain so instanceCardIds captures poke-1-uuid -> xy1-100.
        BoardState loaded = EngineStateMapper.toDomainState(db);

        // Simulate the KO write-back: the Pokémon moves to discard, the Active slot is cleared.
        loaded.getPlayer1Field().getDiscardPile().add(pokeInstanceId);
        loaded.getPlayer1Field().setActivePokemon(null);

        EngineStateMapper.updateDbState(loaded, db);

        var discard = db.getPlayer1State().getDiscardPile();
        assertEquals(1, discard.size());
        assertEquals(pokeInstanceId, discard.get(0).getInstanceId());
        assertEquals("xy1-100", discard.get(0).getCardId(),
                "KO'd Pokémon must keep its real cardId in discard, not an orphan UUID");
    }

    private CardInstanceState cardInstance(String instanceId, String cardId) {
        return CardInstanceState.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .build();
    }

    private AttachedCard attached(String cardId, CardType type) {
        return AttachedCard.builder()
                .cardId(cardId)
                .type(type)
                .build();
    }
}
