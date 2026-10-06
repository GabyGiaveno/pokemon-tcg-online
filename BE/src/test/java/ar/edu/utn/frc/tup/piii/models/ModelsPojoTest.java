package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.cards.*;
import ar.edu.utn.frc.tup.piii.models.cards.effects.*;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.*;
import ar.edu.utn.frc.tup.piii.models.game.*;
import ar.edu.utn.frc.tup.piii.models.game.state.*;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelsPojoTest {

    // ================================================================ game models

    @Test
    void attachedCard_noArgConstructor() {
        AttachedCard ac = new AttachedCard();
        assertNull(ac.getCardId());
        assertNull(ac.getType());
    }

    @Test
    void attachedCard_settersAndGetters() {
        AttachedCard ac = new AttachedCard();
        ac.setCardId("xy1-1");
        ac.setType(CardType.BASIC_ENERGY);
        assertEquals("xy1-1", ac.getCardId());
        assertEquals(CardType.BASIC_ENERGY, ac.getType());
    }

    @Test
    void attachedCard_builder() {
        AttachedCard ac = AttachedCard.builder().cardId("xy1-2").type(CardType.POKEMON_TOOL).build();
        assertEquals("xy1-2", ac.getCardId());
        assertEquals(CardType.POKEMON_TOOL, ac.getType());
        assertNotNull(ac.toString());
    }

    @Test
    void attachedCard_equalsAndHashCode() {
        AttachedCard a = AttachedCard.builder().cardId("xy1-1").type(CardType.BASIC_ENERGY).build();
        AttachedCard b = AttachedCard.builder().cardId("xy1-1").type(CardType.BASIC_ENERGY).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void activePokemon_noArgConstructor() {
        ActivePokemon ap = new ActivePokemon();
        assertNull(ap.getCardId());
        assertNull(ap.getCondition());
    }

    @Test
    void activePokemon_settersAndGetters() {
        ActivePokemon ap = new ActivePokemon();
        ap.setCardId("xy1-1");
        ap.setMaxHp(180);
        ap.setCurrentHp(80);
        ap.setAttachedEnergies(new ArrayList<>());
        ap.setTool(null);
        ap.setCondition(SpecialCondition.POISONED);
        ap.setBurned(true);
        ap.setPoisoned(true);
        ap.setEnteredThisTurn(false);
        assertEquals("xy1-1", ap.getCardId());
        assertEquals(180, ap.getMaxHp());
        assertEquals(80, ap.getCurrentHp());
        assertNotNull(ap.getAttachedEnergies());
        assertNull(ap.getTool());
        assertEquals(SpecialCondition.POISONED, ap.getCondition());
        assertTrue(ap.isBurned());
        assertTrue(ap.isPoisoned());
        assertFalse(ap.isEnteredThisTurn());
    }

    @Test
    void activePokemon_builder() {
        ActivePokemon ap = ActivePokemon.builder()
                .cardId("xy1-1").maxHp(180).currentHp(100)
                .attachedEnergies(new ArrayList<>())
                .condition(SpecialCondition.NONE)
                .isBurned(false).isPoisoned(false).enteredThisTurn(true)
                .build();
        assertEquals("xy1-1", ap.getCardId());
        assertTrue(ap.isEnteredThisTurn());
        assertNotNull(ap.toString());
    }

    @Test
    void activePokemon_equalsAndHashCode() {
        ActivePokemon a = ActivePokemon.builder().cardId("xy1-1").maxHp(180).currentHp(180).build();
        ActivePokemon b = ActivePokemon.builder().cardId("xy1-1").maxHp(180).currentHp(180).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void benchPokemon_noArgConstructor() {
        BenchPokemon bp = new BenchPokemon();
        assertNull(bp.getCardId());
    }

    @Test
    void benchPokemon_settersAndGetters() {
        BenchPokemon bp = new BenchPokemon();
        bp.setCardId("xy1-2");
        bp.setMaxHp(100);
        bp.setCurrentHp(60);
        bp.setAttachedEnergies(new ArrayList<>());
        bp.setTool(null);
        bp.setEnteredThisTurn(true);
        assertEquals("xy1-2", bp.getCardId());
        assertEquals(100, bp.getMaxHp());
        assertEquals(60, bp.getCurrentHp());
        assertTrue(bp.isEnteredThisTurn());
    }

    @Test
    void benchPokemon_builder() {
        BenchPokemon bp = BenchPokemon.builder()
                .cardId("xy1-3").maxHp(80).currentHp(80).attachedEnergies(new ArrayList<>()).build();
        assertEquals("xy1-3", bp.getCardId());
        assertNotNull(bp.toString());
    }

    @Test
    void turnFlags_noArgConstructor() {
        TurnFlags tf = new TurnFlags();
        assertFalse(tf.isEnergyAttachedThisTurn());
        assertFalse(tf.isRetreatedThisTurn());
        assertFalse(tf.isSupporterPlayedThisTurn());
        assertFalse(tf.isAttackedThisTurn());
        assertNotNull(tf.getAbilitiesUsedThisTurn());
    }

    @Test
    void turnFlags_settersAndGetters() {
        TurnFlags tf = new TurnFlags();
        tf.setEnergyAttachedThisTurn(true);
        tf.setRetreatedThisTurn(true);
        tf.setSupporterPlayedThisTurn(true);
        tf.setAttackedThisTurn(true);
        Set<String> abilities = new HashSet<>();
        abilities.add("ACTIVE#Swift Swim");
        tf.setAbilitiesUsedThisTurn(abilities);
        assertTrue(tf.isEnergyAttachedThisTurn());
        assertTrue(tf.isRetreatedThisTurn());
        assertTrue(tf.isSupporterPlayedThisTurn());
        assertTrue(tf.isAttackedThisTurn());
        assertEquals(1, tf.getAbilitiesUsedThisTurn().size());
    }

    @Test
    void turnFlags_builder() {
        TurnFlags tf = TurnFlags.builder()
                .energyAttachedThisTurn(true).retreatedThisTurn(false)
                .supporterPlayedThisTurn(true)
                .attackedThisTurn(true).build();
        assertTrue(tf.isEnergyAttachedThisTurn());
        assertFalse(tf.isRetreatedThisTurn());
        assertNotNull(tf.toString());
    }

    @Test
    void playerField_noArgConstructor() {
        PlayerField pf = new PlayerField();
        assertNull(pf.getPlayerId());
    }

    @Test
    void playerField_settersAndGetters() {
        PlayerField pf = new PlayerField();
        pf.setPlayerId(1L);
        pf.setActivePokemon(null);
        pf.setBench(new ArrayList<>());
        pf.setHand(new ArrayList<>());
        pf.setDeck(new ArrayList<>());
        pf.setPrizeCards(new ArrayList<>());
        pf.setDiscardPile(new ArrayList<>());
        pf.setTurnFlags(new TurnFlags());
        pf.setPlayerTurnCount(3);
        assertEquals(1L, pf.getPlayerId());
        assertNotNull(pf.getBench());
        assertEquals(3, pf.getPlayerTurnCount());
    }

    @Test
    void playerField_builder() {
        PlayerField pf = PlayerField.builder()
                .playerId(1L).bench(new ArrayList<>()).hand(List.of("xy1-1")).deck(new ArrayList<>())
                .prizeCards(new ArrayList<>()).discardPile(new ArrayList<>()).playerTurnCount(1).build();
        assertEquals(1L, pf.getPlayerId());
        assertEquals(1, pf.getHand().size());
        assertNotNull(pf.toString());
    }

    @Test
    void boardState_noArgConstructor() {
        BoardState bs = new BoardState();
        assertNull(bs.getMatchState());
        assertNotNull(bs.getSetupCompletedPlayers());
    }

    @Test
    void boardState_settersAndGetters() {
        BoardState bs = new BoardState();
        bs.setMatchState(GameStatus.ACTIVE);
        bs.setCurrentPhase(TurnPhase.MAIN);
        bs.setCurrentPlayerId(1L);
        bs.setTurnNumber(5);
        bs.setFirstPlayerHasActed(true);
        bs.setWinnerId(2L);
        bs.setFinishedReason("DECK_OUT");
        bs.setPlayer1Field(new PlayerField());
        bs.setPlayer2Field(new PlayerField());
        bs.setActiveStadiumCardId("xy1-5");
        bs.setPendingSelection(null);
        bs.setSetupCompletedPlayers(new ArrayList<>());
        assertEquals(GameStatus.ACTIVE, bs.getMatchState());
        assertEquals(TurnPhase.MAIN, bs.getCurrentPhase());
        assertEquals(1L, bs.getCurrentPlayerId());
        assertEquals(5, bs.getTurnNumber());
        assertTrue(bs.isFirstPlayerHasActed());
        assertEquals(2L, bs.getWinnerId());
        assertEquals("DECK_OUT", bs.getFinishedReason());
        assertNotNull(bs.getPlayer1Field());
        assertEquals("xy1-5", bs.getActiveStadiumCardId());
    }

    @Test
    void boardState_builder() {
        BoardState bs = BoardState.builder()
                .matchState(GameStatus.WAITING).currentPhase(TurnPhase.DRAW).turnNumber(1).build();
        assertEquals(GameStatus.WAITING, bs.getMatchState());
        assertNotNull(bs.toString());
    }

    @Test
    void pendingSelection_builderAndGetters() {
        PendingSelection ps = PendingSelection.builder()
                .type(SelectionType.CHOOSE_ACTIVE_ON_KO)
                .ownerPlayerId(1L)
                .validOptions(List.of("xy1-1", "xy1-2"))
                .prompt("Choose a Pokémon")
                .build();
        assertEquals(SelectionType.CHOOSE_ACTIVE_ON_KO, ps.getType());
        assertEquals(1L, ps.getOwnerPlayerId());
        assertEquals(2, ps.getValidOptions().size());
        assertEquals("Choose a Pokémon", ps.getPrompt());
        assertNotNull(ps.toString());
    }

    @Test
    void pendingSelection_noArgConstructor() {
        PendingSelection ps = new PendingSelection();
        assertNull(ps.getType());
    }

    @Test
    void pendingSelection_setters() {
        PendingSelection ps = new PendingSelection();
        ps.setType(SelectionType.CHOOSE_ACTIVE_ON_KO);
        ps.setOwnerPlayerId(2L);
        ps.setValidOptions(new ArrayList<>());
        ps.setPrompt("p");
        assertEquals(SelectionType.CHOOSE_ACTIVE_ON_KO, ps.getType());
        assertEquals(2L, ps.getOwnerPlayerId());
    }

    // ================================================================ game.state models

    @Test
    void cardZone_allValues() {
        CardZone[] values = CardZone.values();
        assertTrue(values.length > 0);
        assertEquals(CardZone.DECK, CardZone.valueOf("DECK"));
        assertEquals(CardZone.HAND, CardZone.valueOf("HAND"));
        assertEquals(CardZone.IN_PLAY, CardZone.valueOf("IN_PLAY"));
        assertEquals(CardZone.PRIZE, CardZone.valueOf("PRIZE"));
        assertEquals(CardZone.DISCARD, CardZone.valueOf("DISCARD"));
        assertEquals(CardZone.LOST_ZONE, CardZone.valueOf("LOST_ZONE"));
    }

    @Test
    void cardInstanceState_builderAndGetters() {
        CardInstanceState cs = CardInstanceState.builder()
                .instanceId("inst-1").cardId("xy1-1").name("Venusaur-EX")
                .subtypes(List.of("Basic", "EX")).zone(CardZone.HAND).build();
        assertEquals("inst-1", cs.getInstanceId());
        assertEquals("xy1-1", cs.getCardId());
        assertEquals("Venusaur-EX", cs.getName());
        assertEquals(CardZone.HAND, cs.getZone());
        assertNotNull(cs.toString());
    }

    @Test
    void cardInstanceState_settersAndGetters() {
        CardInstanceState cs = new CardInstanceState();
        cs.setInstanceId("inst-2");
        cs.setCardId("xy1-2");
        cs.setName("Charizard-EX");
        cs.setSubtypes(new ArrayList<>());
        cs.setZone(CardZone.DECK);
        assertEquals("inst-2", cs.getInstanceId());
        assertEquals(CardZone.DECK, cs.getZone());
    }

    @Test
    void pokemonInPlayState_builderAndGetters() {
        PokemonInPlayState ps = PokemonInPlayState.builder()
                .instanceId("inst-1").cardId("xy1-1").name("Venusaur-EX")
                .currentHp(100).maxHp(180)
                .attachedEnergies(new ArrayList<>())
                .attachedTools(new ArrayList<>())
                .evolutionCards(new ArrayList<>())
                .statusConditions(List.of("POISONED"))
                .condition("POISONED")
                .burned(true).poisoned(true).enteredThisTurn(false)
                .build();
        assertEquals("inst-1", ps.getInstanceId());
        assertEquals("xy1-1", ps.getCardId());
        assertEquals(100, ps.getCurrentHp());
        assertEquals(180, ps.getMaxHp());
        assertTrue(ps.isBurned());
        assertTrue(ps.isPoisoned());
        assertFalse(ps.isEnteredThisTurn());
        assertEquals("POISONED", ps.getCondition());
        assertEquals(1, ps.getStatusConditions().size());
        assertNotNull(ps.toString());
    }

    @Test
    void pokemonInPlayState_settersAndGetters() {
        PokemonInPlayState ps = new PokemonInPlayState();
        ps.setInstanceId("inst-2");
        ps.setCardId("xy1-2");
        ps.setName("Charizard");
        ps.setCurrentHp(50);
        ps.setMaxHp(150);
        ps.setAttachedEnergies(new ArrayList<>());
        ps.setAttachedTools(new ArrayList<>());
        ps.setEvolutionCards(new ArrayList<>());
        ps.setStatusConditions(new ArrayList<>());
        ps.setCondition("NONE");
        ps.setBurned(false);
        ps.setPoisoned(false);
        ps.setEnteredThisTurn(true);
        assertEquals("inst-2", ps.getInstanceId());
        assertTrue(ps.isEnteredThisTurn());
    }

    @Test
    void playerBoardState_builderAndGetters() {
        PlayerBoardState pbs = PlayerBoardState.builder()
                .playerId(1L)
                .activePokemon(null)
                .bench(new ArrayList<>())
                .deck(new ArrayList<>())
                .hand(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .hasAttachedEnergyThisTurn(false)
                .hasPlayedSupporterThisTurn(false)
                .playerTurnCount(1)
                .retreatedThisTurn(false)
                .attackedThisTurn(false)
                .abilitiesUsedThisTurn(new HashSet<>())
                .build();
        assertEquals(1L, pbs.getPlayerId());
        assertFalse(pbs.isHasAttachedEnergyThisTurn());
        assertEquals(1, pbs.getPlayerTurnCount());
        assertNotNull(pbs.toString());
    }

    @Test
    void playerBoardState_settersAndGetters() {
        PlayerBoardState pbs = new PlayerBoardState();
        pbs.setPlayerId(2L);
        pbs.setHasAttachedEnergyThisTurn(true);
        pbs.setHasPlayedSupporterThisTurn(true);
        pbs.setPlayerTurnCount(3);
        pbs.setRetreatedThisTurn(true);
        pbs.setAttackedThisTurn(true);
        pbs.setAbilitiesUsedThisTurn(new HashSet<>());
        assertEquals(2L, pbs.getPlayerId());
        assertTrue(pbs.isHasAttachedEnergyThisTurn());
        assertTrue(pbs.isRetreatedThisTurn());
        assertTrue(pbs.isAttackedThisTurn());
        assertEquals(3, pbs.getPlayerTurnCount());
    }

    @Test
    void gameBoardState_builderAndGetters() {
        GameBoardState gbs = GameBoardState.builder()
                .gameId("game-1")
                .player1Id(1L)
                .player2Id(2L)
                .currentPlayerId(1L)
                .turnNumber(3)
                .phase(TurnPhase.MAIN)
                .player1State(new PlayerBoardState())
                .player2State(new PlayerBoardState())
                .activeStadiumCardId(null)
                .matchState(GameStatus.ACTIVE)
                .firstPlayerHasActed(true)
                .setupCompletedPlayers(new ArrayList<>())
                .pendingSelection(null)
                .winnerPlayerId(null)
                .finishedReason(null)
                .build();
        assertEquals("game-1", gbs.getGameId());
        assertEquals(1L, gbs.getPlayer1Id());
        assertEquals(2L, gbs.getPlayer2Id());
        assertEquals(TurnPhase.MAIN, gbs.getPhase());
        assertEquals(GameStatus.ACTIVE, gbs.getMatchState());
        assertTrue(gbs.isFirstPlayerHasActed());
        assertNotNull(gbs.toString());
    }

    @Test
    void gameBoardState_settersAndGetters() {
        GameBoardState gbs = new GameBoardState();
        gbs.setGameId("game-2");
        gbs.setPlayer1Id(3L);
        gbs.setPlayer2Id(4L);
        gbs.setCurrentPlayerId(3L);
        gbs.setTurnNumber(1);
        gbs.setPhase(TurnPhase.DRAW);
        gbs.setMatchState(GameStatus.SETUP);
        gbs.setFirstPlayerHasActed(false);
        gbs.setWinnerPlayerId(null);
        gbs.setFinishedReason(null);
        gbs.setActiveStadiumCardId(null);
        gbs.setPendingSelection(null);
        assertEquals("game-2", gbs.getGameId());
        assertEquals(TurnPhase.DRAW, gbs.getPhase());
    }

    // ================================================================ enums

    @Test
    void cardType_allValues() {
        assertNotNull(CardType.valueOf("BASIC_ENERGY"));
        assertNotNull(CardType.valueOf("POKEMON_TOOL"));
        assertTrue(CardType.values().length > 0);
    }

    @Test
    void energyType_allValues() {
        assertTrue(EnergyType.values().length > 0);
        assertNotNull(EnergyType.valueOf("FIRE"));
    }

    @Test
    void gameStatus_allValues() {
        assertNotNull(GameStatus.valueOf("ACTIVE"));
        assertNotNull(GameStatus.valueOf("FINISHED"));
        assertTrue(GameStatus.values().length > 0);
    }

    @Test
    void specialCondition_allValues() {
        assertNotNull(SpecialCondition.valueOf("POISONED"));
        assertNotNull(SpecialCondition.valueOf("ASLEEP"));
        assertNotNull(SpecialCondition.valueOf("NONE"));
        assertTrue(SpecialCondition.values().length > 0);
    }

    @Test
    void actionType_allValues() {
        assertNotNull(ActionType.valueOf("END_TURN"));
        assertNotNull(ActionType.valueOf("USE_ATTACK"));
        assertNotNull(ActionType.valueOf("PLAY_BASIC_POKEMON"));
        assertTrue(ActionType.values().length > 0);
    }

    @Test
    void turnPhase_allValues() {
        assertNotNull(TurnPhase.valueOf("DRAW"));
        assertNotNull(TurnPhase.valueOf("MAIN"));
        assertTrue(TurnPhase.values().length > 0);
    }

    @Test
    void selectionType_allValues() {
        assertNotNull(SelectionType.valueOf("CHOOSE_ACTIVE_ON_KO"));
        assertTrue(SelectionType.values().length > 0);
    }

    @Test
    void victoryReason_allValues() {
        assertTrue(VictoryReason.values().length > 0);
        assertNotNull(VictoryReason.values()[0]);
    }

    // ================================================================ effects models

    @Test
    void addDamageEffect_settersAndGetters() {
        AddDamageEffect e = new AddDamageEffect();
        e.setAmount(30);
        e.setCondition("ALWAYS");
        e.setTarget("DEFENDER");
        e.setOptional(false);
        assertEquals(30, e.getAmount());
        assertEquals("ALWAYS", e.getCondition());
        assertEquals("DEFENDER", e.getTarget());
        assertFalse(e.getOptional());
        assertNotNull(e.toString());
    }

    @Test
    void addDamageEffect_equalsAndHashCode() {
        AddDamageEffect a = new AddDamageEffect();
        a.setAmount(10);
        AddDamageEffect b = new AddDamageEffect();
        b.setAmount(10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void applyConditionEffect_settersAndGetters() {
        ApplyConditionEffect e = new ApplyConditionEffect();
        e.setCondition("PARALYZED");
        e.setTarget("DEFENDER");
        assertEquals("PARALYZED", e.getCondition());
        assertEquals("DEFENDER", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void healEffect_settersAndGetters() {
        HealEffect e = new HealEffect();
        e.setAmount(30);
        e.setTarget("SELF");
        assertEquals(30, e.getAmount());
        assertEquals("SELF", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void multiplierDamageEffect_settersAndGetters() {
        MultiplierDamageEffect e = new MultiplierDamageEffect();
        e.setAmountPerUnit(20);
        e.setUnitType("ENERGY");
        e.setFlips(2);
        assertEquals(20, e.getAmountPerUnit());
        assertEquals("ENERGY", e.getUnitType());
        assertEquals(2, e.getFlips());
        assertNotNull(e.toString());
    }

    @Test
    void discardEnergyEffect_settersAndGetters() {
        DiscardEnergyEffect e = new DiscardEnergyEffect();
        e.setAmount(1);
        e.setTarget("SELF");
        assertEquals(1, e.getAmount());
        assertEquals("SELF", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void damageToBenchEffect_settersAndGetters() {
        DamageToBenchEffect e = new DamageToBenchEffect();
        e.setAmount(10);
        e.setTargetCount(3);
        e.setTarget("OPPONENT");
        assertEquals(10, e.getAmount());
        assertEquals(3, e.getTargetCount());
        assertEquals("OPPONENT", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void preventDamageEffect_settersAndGetters() {
        PreventDamageEffect e = new PreventDamageEffect();
        e.setTarget("SELF");
        assertEquals("SELF", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void damageCountersEffect_settersAndGetters() {
        DamageCountersEffect e = new DamageCountersEffect();
        e.setAmount(2);
        e.setTarget("BENCH");
        assertEquals(2, e.getAmount());
        assertEquals("BENCH", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void switchPokemonEffect_settersAndGetters() {
        SwitchPokemonEffect e = new SwitchPokemonEffect();
        e.setTarget("OPPONENT");
        e.setForce(true);
        assertEquals("OPPONENT", e.getTarget());
        assertTrue(e.isForce());
        assertNotNull(e.toString());
    }

    @Test
    void searchDeckEffect_settersAndGetters() {
        SearchDeckEffect e = new SearchDeckEffect();
        e.setFilter("BASIC");
        e.setDestination("HAND");
        e.setAmount(1);
        assertEquals("BASIC", e.getFilter());
        assertEquals("HAND", e.getDestination());
        assertEquals(1, e.getAmount());
        assertNotNull(e.toString());
    }

    @Test
    void restrictEffect_settersAndGetters() {
        RestrictEffect e = new RestrictEffect();
        e.setRestriction("NO_ATTACK");
        e.setTarget("OPPONENT");
        e.setDuration("NEXT_TURN");
        assertEquals("NO_ATTACK", e.getRestriction());
        assertEquals("OPPONENT", e.getTarget());
        assertEquals("NEXT_TURN", e.getDuration());
        assertNotNull(e.toString());
    }

    @Test
    void lookAtDeckEffect_settersAndGetters() {
        LookAtDeckEffect e = new LookAtDeckEffect();
        e.setAmount(5);
        e.setTarget("SELF");
        e.setReorder(true);
        assertEquals(5, e.getAmount());
        assertEquals("SELF", e.getTarget());
        assertTrue(e.isReorder());
        assertNotNull(e.toString());
    }

    @Test
    void drawUntilHandSizeEffect_settersAndGetters() {
        DrawUntilHandSizeEffect e = new DrawUntilHandSizeEffect();
        e.setAmount(6);
        assertEquals(6, e.getAmount());
        assertNotNull(e.toString());
    }

    @Test
    void shuffleHandEffect_settersAndGetters() {
        ShuffleHandEffect e = new ShuffleHandEffect();
        e.setTarget("SELF");
        e.setDrawAmount(4);
        assertEquals("SELF", e.getTarget());
        assertEquals(4, e.getDrawAmount());
        assertNotNull(e.toString());
    }

    @Test
    void unknownEffect_settersAndGetters() {
        UnknownEffect e = new UnknownEffect();
        e.setType("UNKNOWN_TYPE");
        assertEquals("UNKNOWN_TYPE", e.getType());
        assertNotNull(e.toString());
    }

    @Test
    void coinFlipEffect_settersAndGetters() {
        CoinFlipEffect e = new CoinFlipEffect();
        e.setIfHeads(new ArrayList<>());
        e.setIfTails(new ArrayList<>());
        assertNotNull(e.getIfHeads());
        assertNotNull(e.getIfTails());
        assertNotNull(e.toString());
    }

    @Test
    void coinFlipDamageEffect_settersAndGetters() {
        CoinFlipDamageEffect e = new CoinFlipDamageEffect();
        AddDamageEffect inner = new AddDamageEffect();
        inner.setAmount(50);
        e.setHeadsCondition(inner);
        assertEquals(inner, e.getHeadsCondition());
        assertNotNull(e.toString());
    }

    @Test
    void reduceDamageEffect_settersAndGetters() {
        ReduceDamageEffect e = new ReduceDamageEffect();
        e.setAmount(20);
        e.setTarget("SELF");
        assertEquals(20, e.getAmount());
        assertEquals("SELF", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void restrictItemsEffect_settersAndGetters() {
        RestrictItemsEffect e = new RestrictItemsEffect();
        e.setTarget("OPPONENT");
        assertEquals("OPPONENT", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void immuneToConditionsEffect_settersAndGetters() {
        ImmuneToConditionsEffect e = new ImmuneToConditionsEffect();
        e.setTarget("SELF");
        assertEquals("SELF", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void passiveAbilityEffect_settersAndGetters() {
        PassiveAbilityEffect e = new PassiveAbilityEffect();
        e.setTrigger("CONTINUOUS");
        e.setEffect(new AddDamageEffect());
        e.setStackable(false);
        e.setConditions(new ArrayList<>());
        assertEquals("CONTINUOUS", e.getTrigger());
        assertNotNull(e.getEffect());
        assertFalse(e.isStackable());
        assertNotNull(e.getConditions());
        assertNotNull(e.toString());
    }

    @Test
    void abilityCondition_allArgsConstructorAndGetters() {
        AbilityCondition ac = new AbilityCondition("IS_ACTIVE", "SELF", "true");
        assertEquals("IS_ACTIVE", ac.getType());
        assertEquals("SELF", ac.getTarget());
        assertEquals("true", ac.getValue());

        AbilityCondition ac2 = new AbilityCondition();
        ac2.setType("HAS_ENERGY");
        ac2.setTarget("SELF");
        ac2.setValue("FIRE");
        assertEquals("HAS_ENERGY", ac2.getType());
        assertNotNull(ac.toString());
        assertEquals(ac, ac);
    }

    // ================================================================ trainer effects

    @Test
    void drawCardsTrainerEffect_settersAndGetters() {
        DrawCardsTrainerEffect e = new DrawCardsTrainerEffect();
        e.setAmount(3);
        assertEquals(3, e.getAmount());
        assertNotNull(e.toString());
    }

    @Test
    void healTrainerEffect_settersAndGetters() {
        HealTrainerEffect e = new HealTrainerEffect();
        e.setAmount(30);
        e.setTarget("ACTIVE");
        assertEquals(30, e.getAmount());
        assertEquals("ACTIVE", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void coinFlipTrainerEffect_settersAndGetters() {
        CoinFlipTrainerEffect e = new CoinFlipTrainerEffect();
        e.setIfHeads(new ArrayList<>());
        e.setIfTails(new ArrayList<>());
        assertNotNull(e.getIfHeads());
        assertNotNull(e.getIfTails());
        assertNotNull(e.toString());
    }

    @Test
    void discardEnergyTrainerEffect_settersAndGetters() {
        DiscardEnergyTrainerEffect e = new DiscardEnergyTrainerEffect();
        e.setAmount(1);
        e.setTarget("OPPONENT_ACTIVE");
        assertEquals(1, e.getAmount());
        assertEquals("OPPONENT_ACTIVE", e.getTarget());
        assertNotNull(e.toString());
    }

    @Test
    void discardHandDrawTrainerEffect_settersAndGetters() {
        DiscardHandDrawTrainerEffect e = new DiscardHandDrawTrainerEffect();
        e.setAmount(7);
        assertEquals(7, e.getAmount());
        assertNotNull(e.toString());
    }

    @Test
    void shuffleHandTrainerEffect_settersAndGetters() {
        ShuffleHandTrainerEffect e = new ShuffleHandTrainerEffect();
        e.setTarget("SELF");
        e.setDrawAmount(4);
        assertEquals("SELF", e.getTarget());
        assertEquals(4, e.getDrawAmount());
        assertNotNull(e.toString());
    }

    @Test
    void unknownTrainerEffect_settersAndGetters() {
        UnknownTrainerEffect e = new UnknownTrainerEffect();
        e.setType("WEIRD_TYPE");
        assertEquals("WEIRD_TYPE", e.getType());
        assertNotNull(e.toString());
    }

    // ================================================================ GameEvent

    @Test
    void gameEvent_factoryMethods() {
        GameEvent e1 = GameEvent.of(GameEventType.CARD_DRAWN, "Card drawn");
        assertEquals(GameEventType.CARD_DRAWN, e1.getType());
        assertEquals("Card drawn", e1.getDescription());
        assertNotNull(e1.getData());
        assertTrue(e1.getTimestamp() > 0);

        Map<String, Object> data = Map.of("cardId", "xy1-1");
        GameEvent e2 = GameEvent.of(GameEventType.DAMAGE_DEALT, "Damage dealt", data);
        assertEquals(GameEventType.DAMAGE_DEALT, e2.getType());
        assertEquals("xy1-1", e2.getData().get("cardId"));

        UUID gameId = UUID.randomUUID();
        GameEvent e3 = GameEvent.of(gameId, GameEventType.GAME_FINISHED, "Game over", data);
        assertEquals(gameId, e3.getGameId());
        assertEquals(GameEventType.GAME_FINISHED, e3.getType());
    }

    @Test
    void gameEvent_builderAndGetters() {
        UUID gameId = UUID.randomUUID();
        GameEvent e = GameEvent.builder()
                .gameId(gameId)
                .type(GameEventType.TURN_ENDED)
                .description("Turn ended")
                .data(Map.of())
                .timestamp(System.currentTimeMillis())
                .build();
        assertEquals(gameId, e.getGameId());
        assertEquals(GameEventType.TURN_ENDED, e.getType());
        assertNotNull(e.toString());
    }

    @Test
    void gameEvent_setters() {
        GameEvent e = new GameEvent();
        e.setGameId(UUID.randomUUID());
        e.setType(GameEventType.PHASE_CHANGED);
        e.setDescription("desc");
        e.setData(Map.of());
        e.setTimestamp(123L);
        assertEquals(GameEventType.PHASE_CHANGED, e.getType());
        assertEquals(123L, e.getTimestamp());
    }

    @Test
    void gameEventType_allValues() {
        assertTrue(GameEventType.values().length > 0);
        assertNotNull(GameEventType.valueOf("CARD_DRAWN"));
        assertNotNull(GameEventType.valueOf("GAME_FINISHED"));
        assertNotNull(GameEventType.valueOf("POKEMON_KNOCKED_OUT"));
    }
}
