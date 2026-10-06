package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.request.*;
import ar.edu.utn.frc.tup.piii.dtos.response.*;
import ar.edu.utn.frc.tup.piii.dtos.ws.GameEventMessage;
import ar.edu.utn.frc.tup.piii.dtos.ws.GameStateChangedMessage;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtosPojoTest {

    // ======================================================= REQUEST DTOs

    @Test
    void loginRequest_gettersSetters() {
        LoginRequest r = new LoginRequest();
        r.setUsername("ash");
        r.setPassword("pikachu");
        assertEquals("ash", r.getUsername());
        assertEquals("pikachu", r.getPassword());
    }

    @Test
    void registerRequest_gettersSetters() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("ash");
        r.setEmail("ash@test.com");
        r.setPassword("pikachu");
        assertEquals("ash", r.getUsername());
        assertEquals("ash@test.com", r.getEmail());
        assertEquals("pikachu", r.getPassword());
    }

    @Test
    void createGameRequest_gettersSetters() {
        CreateGameRequest r = new CreateGameRequest();
        r.setDeckId(1L);
        assertEquals(1L, r.getDeckId());
    }

    @Test
    void joinGameRequest_gettersSetters() {
        JoinGameRequest r = new JoinGameRequest();
        r.setDeckId(2L);
        assertEquals(2L, r.getDeckId());
    }

    @Test
    void forgotPasswordRequest_gettersSetters() {
        ForgotPasswordRequest r = new ForgotPasswordRequest();
        r.setEmail("ash@test.com");
        assertEquals("ash@test.com", r.getEmail());
    }

    @Test
    void resetPasswordRequest_gettersSetters() {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setToken("tok123");
        r.setNewPassword("newpass");
        assertEquals("tok123", r.getToken());
        assertEquals("newpass", r.getNewPassword());
    }

    @Test
    void equipSkinRequest_gettersSetters() {
        EquipSkinRequest r = new EquipSkinRequest();
        r.setSkinId("default");
        assertEquals("default", r.getSkinId());
    }

    @Test
    void updateProfileRequest_gettersSetters() {
        UpdateProfileRequest r = new UpdateProfileRequest();
        r.setBio("bio text");
        r.setFavoriteRegion("Kanto");
        r.setFavoritePokemon("Pikachu");
        assertEquals("bio text", r.getBio());
        assertEquals("Kanto", r.getFavoriteRegion());
        assertEquals("Pikachu", r.getFavoritePokemon());
    }

    @Test
    void actionRequest_gettersSettersAndBuilder() {
        ActionRequest r = new ActionRequest();
        r.setType(ActionType.USE_ATTACK);
        r.setCardId("xy1-1");
        r.setTargetPosition("ACTIVE");
        r.setAttackIndex(0);
        r.setBenchIndex(1);
        r.setPrizeIndex(2);
        r.setAbilityIndex(0);
        assertEquals(ActionType.USE_ATTACK, r.getType());
        assertEquals("xy1-1", r.getCardId());
        assertEquals("ACTIVE", r.getTargetPosition());
        assertEquals(0, r.getAttackIndex());
        assertEquals(1, r.getBenchIndex());
        assertEquals(2, r.getPrizeIndex());
        assertEquals(0, r.getAbilityIndex());

        ActionRequest built = ActionRequest.builder()
                .type(ActionType.END_TURN).cardId("xy1-2").targetPosition("BENCH_0")
                .attackIndex(1).benchIndex(0).prizeIndex(0).abilityIndex(1).build();
        assertEquals(ActionType.END_TURN, built.getType());
        assertNotNull(built.toString());
    }

    @Test
    void gameActionApiRequest_gettersSettersAndBuilder() {
        GameActionApiRequest r = new GameActionApiRequest();
        r.setType(ActionType.RETREAT);
        r.setCardInstanceId("inst-1");
        r.setCardId("xy1-1");
        r.setTargetPosition("BENCH_0");
        r.setAttackIndex(0);
        r.setBenchIndex(1);
        r.setPrizeIndex(2);
        assertEquals(ActionType.RETREAT, r.getType());
        assertEquals("inst-1", r.getCardInstanceId());
        assertEquals("xy1-1", r.getCardId());

        GameActionApiRequest built = GameActionApiRequest.builder()
                .type(ActionType.END_TURN).cardInstanceId("i2").build();
        assertEquals(ActionType.END_TURN, built.getType());
        assertNotNull(built.toString());
    }

    @Test
    void createDeckRequest_gettersSetters() {
        CreateDeckRequest r = new CreateDeckRequest();
        r.setName("Fire Deck");
        List<CreateDeckRequest.CardEntry> cards = List.of(new CreateDeckRequest.CardEntry("xy1-1", 4));
        r.setCards(cards);
        assertEquals("Fire Deck", r.getName());
        assertEquals(1, r.getCards().size());
        assertEquals("xy1-1", r.getCards().get(0).getCardId());
        assertEquals(4, r.getCards().get(0).getQuantity());
    }

    @Test
    void createDeckRequest_cardEntry_gettersSetters() {
        CreateDeckRequest.CardEntry e = new CreateDeckRequest.CardEntry();
        e.setCardId("xy1-5");
        e.setQuantity(2);
        assertEquals("xy1-5", e.getCardId());
        assertEquals(2, e.getQuantity());
        assertNotNull(e.toString());
    }

    // ======================================================= RESPONSE DTOs

    @Test
    void playerResponse_gettersSetters() {
        PlayerResponse r = new PlayerResponse();
        r.setId(1L);
        r.setUsername("ash");
        r.setToken("jwt-token");
        assertEquals(1L, r.getId());
        assertEquals("ash", r.getUsername());
        assertEquals("jwt-token", r.getToken());
    }

    @Test
    void playerPrivateDTO_gettersSetters() {
        PlayerPrivateDTO r = new PlayerPrivateDTO();
        r.setHand(List.of("xy1-1", "xy1-2"));
        r.setPrizeCards(List.of("xy1-3"));
        r.setDeckSize(55);
        assertEquals(2, r.getHand().size());
        assertEquals(1, r.getPrizeCards().size());
        assertEquals(55, r.getDeckSize());
    }

    @Test
    void passwordResetResponse_gettersSetters() {
        PasswordResetResponse r = new PasswordResetResponse();
        r.setMessage("Success");
        assertEquals("Success", r.getMessage());

        PasswordResetResponse r2 = new PasswordResetResponse("Done");
        assertEquals("Done", r2.getMessage());
    }

    @Test
    void syncResponse_gettersSetters() {
        SyncResponse r = new SyncResponse();
        r.setMessage("synced");
        r.setSet("xy1");
        r.setCardsImported(50);
        r.setError(null);
        assertEquals("synced", r.getMessage());
        assertEquals("xy1", r.getSet());
        assertEquals(50, r.getCardsImported());
        assertNull(r.getError());

        SyncResponse r2 = new SyncResponse("m", "s");
        assertEquals("m", r2.getMessage());
        SyncResponse r3 = new SyncResponse("m", "s", 10);
        assertEquals(10, r3.getCardsImported());
    }

    @Test
    void gameSessionResponse_gettersSetters() {
        GameSessionResponse r = new GameSessionResponse();
        UUID id = UUID.randomUUID();
        r.setGameId(id);
        r.setStatus(GameStatus.ACTIVE);
        r.setPlayer1Username("ash");
        r.setPlayer2Username("misty");
        r.setCreatedAt(LocalDateTime.now());
        r.setPrizeCardsCount(6);
        assertEquals(id, r.getGameId());
        assertEquals(GameStatus.ACTIVE, r.getStatus());
        assertEquals("ash", r.getPlayer1Username());
        assertEquals("misty", r.getPlayer2Username());
        assertEquals(6, r.getPrizeCardsCount());
        assertNotNull(r.getCreatedAt());
    }

    @Test
    void cardPageResponse_gettersSetters() {
        CardPageResponse r = new CardPageResponse();
        r.setData(new ArrayList<>());
        r.setTotal(100L);
        r.setPage(1);
        r.setSize(20);
        assertNotNull(r.getData());
        assertEquals(100L, r.getTotal());
        assertEquals(1, r.getPage());
        assertEquals(20, r.getSize());
    }

    @Test
    void cardResponse_builderAndGetters() {
        CardResponse r = CardResponse.builder()
                .id("xy1-1")
                .name("Venusaur-EX")
                .supertype("Pokémon")
                .subtypes(List.of("Basic", "EX"))
                .hp(180)
                .types(List.of("Grass"))
                .cardSetId("xy1")
                .cardSetName("XY")
                .imageUrlSmall("url-small")
                .imageUrlLarge("url-large")
                .evolvesFrom(null)
                .attacks("[]")
                .weaknesses("Fire×2")
                .resistances("")
                .retreatCost(List.of("C", "C", "C", "C"))
                .build();
        assertEquals("xy1-1", r.getId());
        assertEquals("Venusaur-EX", r.getName());
        assertEquals(180, r.getHp());
        assertNotNull(r.toString());
    }

    @Test
    void cardResponse_equalsAndHashCode() {
        CardResponse a = CardResponse.builder().id("xy1-1").name("V").build();
        CardResponse b = CardResponse.builder().id("xy1-1").name("V").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void deckCardResponse_builderAndGetters() {
        DeckCardResponse r = DeckCardResponse.builder()
                .cardId("xy1-1").cardName("Venusaur-EX").quantity(2)
                .supertype("Pokémon").types(List.of("Grass")).subtypes(List.of("Basic"))
                .imageUrlSmall("url").build();
        assertEquals("xy1-1", r.getCardId());
        assertEquals("Venusaur-EX", r.getCardName());
        assertEquals(2, r.getQuantity());
        assertNotNull(r.toString());
    }

    @Test
    void deckResponse_builderAndGetters() {
        DeckResponse r = DeckResponse.builder()
                .id(1L).name("Fire Deck").valid(true).cardCount(60)
                .cards(new ArrayList<>()).validationErrors(new ArrayList<>())
                .createdAt(LocalDateTime.now()).build();
        assertEquals(1L, r.getId());
        assertEquals("Fire Deck", r.getName());
        assertTrue(r.isValid());
        assertEquals(60, r.getCardCount());
        assertNotNull(r.getCards());
        assertNotNull(r.toString());
    }

    @Test
    void deckValidationError_builderAndGetters() {
        DeckValidationError e = DeckValidationError.builder()
                .code("OVER_LIMIT").message("Too many copies").cardId("xy1-1").build();
        assertEquals("OVER_LIMIT", e.getCode());
        assertEquals("Too many copies", e.getMessage());
        assertEquals("xy1-1", e.getCardId());
        assertNotNull(e.toString());

        DeckValidationError e2 = new DeckValidationError();
        e2.setCode("ERR");
        e2.setMessage("msg");
        e2.setCardId("id");
        assertEquals("ERR", e2.getCode());
    }

    @Test
    void deckValidationResponse_builderAndGetters() {
        DeckValidationResponse r = DeckValidationResponse.builder()
                .valid(true).errors(new ArrayList<>()).cardCount(60).build();
        assertTrue(r.isValid());
        assertNotNull(r.getErrors());
        assertEquals(60, r.getCardCount());
        assertNotNull(r.toString());
    }

    @Test
    void actionResult_factoryMethods() {
        ActionResult ok = ActionResult.success(new ArrayList<>());
        assertTrue(ok.isSuccess());
        assertNull(ok.getError());
        assertNotNull(ok.getEvents());

        ActionResult fail = ActionResult.failure("Not your turn");
        assertFalse(fail.isSuccess());
        assertEquals("Not your turn", fail.getError());
        assertNotNull(fail.getEvents());

        ActionResult okNull = ActionResult.success(null);
        assertTrue(okNull.isSuccess());
        assertNotNull(okNull.getEvents());
    }

    @Test
    void actionResult_builderAndGetters() {
        ActionResult r = ActionResult.builder().success(true).error(null).events(new ArrayList<>()).build();
        assertTrue(r.isSuccess());
        assertNull(r.getError());
        assertNotNull(r.toString());

        ActionResult r2 = new ActionResult();
        r2.setSuccess(false);
        r2.setError("err");
        r2.setEvents(new ArrayList<>());
        assertFalse(r2.isSuccess());
    }

    @Test
    void gameActionApiResponse_factoryMethods() {
        GameActionApiResponse ok = GameActionApiResponse.ok(ActionType.END_TURN, new ArrayList<>());
        assertTrue(ok.isSuccess());
        assertEquals(ActionType.END_TURN, ok.getActionType());
        assertNotNull(ok.getEvents());

        GameActionApiResponse fail = GameActionApiResponse.fail(ActionType.USE_ATTACK, "error msg");
        assertFalse(fail.isSuccess());
        assertEquals("error msg", fail.getError());

        GameActionApiResponse okNull = GameActionApiResponse.ok(ActionType.DRAW_CARD, null);
        assertTrue(okNull.isSuccess());
        assertNotNull(okNull.getEvents());
    }

    @Test
    void gameActionApiResponse_builderAndGetters() {
        GameActionApiResponse r = GameActionApiResponse.builder()
                .success(true).actionType(ActionType.END_TURN).events(new ArrayList<>()).build();
        assertTrue(r.isSuccess());
        assertEquals(ActionType.END_TURN, r.getActionType());
        assertNotNull(r.toString());

        GameActionApiResponse r2 = new GameActionApiResponse();
        r2.setSuccess(false);
        r2.setActionType(ActionType.RETREAT);
        r2.setEvents(new ArrayList<>());
        r2.setError("err");
        assertFalse(r2.isSuccess());
    }

    @Test
    void gameEventDTO_gettersSetters() {
        GameEventDTO r = new GameEventDTO();
        r.setType("CARD_DRAWN");
        r.setPayload("payload");
        r.setTimestamp(LocalDateTime.now());
        assertEquals("CARD_DRAWN", r.getType());
        assertEquals("payload", r.getPayload());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void pendingSelectionDTO_builderAndGetters() {
        PendingSelectionDTO r = PendingSelectionDTO.builder()
                .type("CHOOSE_ACTIVE_ON_KO")
                .ownerPlayerId(1L)
                .validOptions(List.of("xy1-1", "xy1-2"))
                .prompt("Choose a Pokemon")
                .build();
        assertEquals("CHOOSE_ACTIVE_ON_KO", r.getType());
        assertEquals(1L, r.getOwnerPlayerId());
        assertEquals(2, r.getValidOptions().size());
        assertEquals("Choose a Pokemon", r.getPrompt());
        assertNotNull(r.toString());

        PendingSelectionDTO r2 = new PendingSelectionDTO();
        r2.setType("TYPE");
        r2.setOwnerPlayerId(2L);
        r2.setValidOptions(new ArrayList<>());
        r2.setPrompt("prompt");
        assertEquals("TYPE", r2.getType());
    }

    @Test
    void profileResponse_gettersSetters() {
        ProfileResponse r = new ProfileResponse();
        r.setId(1L);
        r.setUsername("ash");
        r.setEmail("ash@test.com");
        r.setCreatedAt(LocalDateTime.now());
        r.setDecksCount(3);
        r.setXpPercent(75);
        r.setBio("Trainer");
        r.setLevel(10);
        r.setFavoriteRegion("Kanto");
        r.setFavoritePokemon("Pikachu");
        r.setTotalCards(50);
        r.setWins(20);
        r.setLosses(5);
        r.setStreak(3);
        r.setTournamentsWon(1);
        r.setPacksOpened(10);
        r.setDecksCreated(4);
        assertEquals(1L, r.getId());
        assertEquals("ash", r.getUsername());
        assertEquals("ash@test.com", r.getEmail());
        assertEquals(3, r.getDecksCount());
        assertEquals(75, r.getXpPercent());
        assertEquals("Trainer", r.getBio());
        assertEquals(10, r.getLevel());
        assertEquals("Kanto", r.getFavoriteRegion());
        assertEquals("Pikachu", r.getFavoritePokemon());
        assertEquals(50, r.getTotalCards());
        assertEquals(20, r.getWins());
        assertEquals(5, r.getLosses());
        assertEquals(3, r.getStreak());
        assertEquals(1, r.getTournamentsWon());
        assertEquals(10, r.getPacksOpened());
        assertEquals(4, r.getDecksCreated());
    }

    @Test
    void achievementResponse_gettersSetters() {
        AchievementResponse r = new AchievementResponse();
        r.setId("first_win");
        r.setName("First Win");
        r.setDescription("Win your first game");
        r.setIcon("🏆");
        r.setUnlocked(true);
        assertEquals("first_win", r.getId());
        assertEquals("First Win", r.getName());
        assertTrue(r.isUnlocked());
    }

    @Test
    void badgeResponse_gettersSetters() {
        BadgeResponse r = new BadgeResponse();
        r.setId("boulder");
        r.setLabel("Boulder Badge");
        r.setIcon("🪨");
        r.setUnlocked(true);
        r.setDescription("Desc");
        r.setHowToUnlock("Win 10 games");
        assertEquals("boulder", r.getId());
        assertEquals("Boulder Badge", r.getLabel());
        assertTrue(r.isUnlocked());
        assertEquals("Desc", r.getDescription());
        assertEquals("Win 10 games", r.getHowToUnlock());
    }

    @Test
    void customizationItemResponse_gettersSetters() {
        CustomizationItemResponse r = new CustomizationItemResponse();
        r.setId("hat1");
        r.setName("Red Hat");
        r.setCategory("HAT");
        r.setUnlocked(true);
        r.setColor("#FF0000");
        assertEquals("hat1", r.getId());
        assertEquals("Red Hat", r.getName());
        assertEquals("HAT", r.getCategory());
        assertTrue(r.isUnlocked());
        assertEquals("#FF0000", r.getColor());
    }

    @Test
    void skinResponse_gettersSetters() {
        SkinResponse r = new SkinResponse();
        r.setId("default");
        r.setName("Default Trainer");
        r.setHatColor("#FF0000");
        r.setShirtColor("#00FF00");
        r.setPantsColor("#0000FF");
        r.setSkinTone("#FFCCAA");
        r.setEquipped(true);
        assertEquals("default", r.getId());
        assertEquals("Default Trainer", r.getName());
        assertEquals("#FF0000", r.getHatColor());
        assertEquals("#00FF00", r.getShirtColor());
        assertEquals("#0000FF", r.getPantsColor());
        assertEquals("#FFCCAA", r.getSkinTone());
        assertTrue(r.isEquipped());
    }

    @Test
    void boardStateDTO_gettersSetters() {
        BoardStateDTO r = new BoardStateDTO();
        r.setGameId("game-1");
        r.setCurrentPlayerId(1L);
        r.setPhase(TurnPhase.MAIN);
        r.setTurnNumber(3);
        r.setMyTurn(true);
        r.setStatus(GameStatus.ACTIVE);
        r.setWinnerId(null);
        r.setFinishedReason(null);
        r.setPendingSelection(null);
        r.setMyField(new PlayerFieldDTO());
        r.setOpponentField(new OpponentFieldDTO());
        assertEquals("game-1", r.getGameId());
        assertEquals(1L, r.getCurrentPlayerId());
        assertEquals(TurnPhase.MAIN, r.getPhase());
        assertEquals(3, r.getTurnNumber());
        assertTrue(r.isMyTurn());
        assertEquals(GameStatus.ACTIVE, r.getStatus());
        assertNull(r.getWinnerId());
        assertNull(r.getFinishedReason());
        assertNull(r.getPendingSelection());
        assertNotNull(r.getMyField());
        assertNotNull(r.getOpponentField());
    }

    @Test
    void playerFieldDTO_gettersSetters() {
        PlayerFieldDTO r = new PlayerFieldDTO();
        r.setActivePokemon(new ActivePokemonDTO());
        r.setBench(new ArrayList<>());
        r.setHand(new ArrayList<>());
        r.setDeckSize(40);
        r.setPrizeCards(new ArrayList<>());
        r.setDiscardPile(new ArrayList<>());
        assertNotNull(r.getActivePokemon());
        assertNotNull(r.getBench());
        assertNotNull(r.getHand());
        assertEquals(40, r.getDeckSize());
        assertNotNull(r.getPrizeCards());
        assertNotNull(r.getDiscardPile());
    }

    @Test
    void opponentFieldDTO_gettersSetters() {
        OpponentFieldDTO r = new OpponentFieldDTO();
        r.setActivePokemon(new ActivePokemonDTO());
        r.setBench(new ArrayList<>());
        r.setHandSize(7);
        r.setDeckSize(40);
        r.setPrizeCards(new ArrayList<>());
        r.setDiscardPile(new ArrayList<>());
        assertNotNull(r.getActivePokemon());
        assertEquals(7, r.getHandSize());
        assertEquals(40, r.getDeckSize());
    }

    @Test
    void activePokemonDTO_gettersSetters() {
        ActivePokemonDTO r = new ActivePokemonDTO();
        r.setInstanceId("inst-1");
        r.setCardId("xy1-1");
        r.setHp(80);
        r.setMaxHp(180);
        r.setAttachedEnergies(new ArrayList<>());
        r.setToolCard(null);
        r.setConditions(List.of("POISONED"));
        assertEquals("inst-1", r.getInstanceId());
        assertEquals("xy1-1", r.getCardId());
        assertEquals(80, r.getHp());
        assertEquals(180, r.getMaxHp());
        assertNotNull(r.getAttachedEnergies());
        assertNull(r.getToolCard());
        assertEquals(1, r.getConditions().size());
    }

    @Test
    void benchPokemonDTO_gettersSetters() {
        BenchPokemonDTO r = new BenchPokemonDTO();
        r.setInstanceId("inst-2");
        r.setCardId("xy1-2");
        r.setHp(60);
        r.setMaxHp(100);
        r.setAttachedEnergies(new ArrayList<>());
        assertEquals("inst-2", r.getInstanceId());
        assertEquals("xy1-2", r.getCardId());
        assertEquals(60, r.getHp());
        assertEquals(100, r.getMaxHp());
        assertNotNull(r.getAttachedEnergies());
    }

    @Test
    void cardInstanceDTO_constructors() {
        CardInstanceDTO r1 = new CardInstanceDTO();
        assertNull(r1.getInstanceId());

        CardInstanceDTO r2 = new CardInstanceDTO("inst-1", "xy1-1", "Venusaur-EX");
        assertEquals("inst-1", r2.getInstanceId());
        assertEquals("xy1-1", r2.getCardId());
        assertEquals("Venusaur-EX", r2.getName());

        CardInstanceDTO r3 = new CardInstanceDTO("inst-2", "xy1-2", "Charizard-EX", List.of("Basic", "EX"));
        assertEquals(2, r3.getSubtypes().size());

        r3.setSupertype("Pokémon");
        assertEquals("Pokémon", r3.getSupertype());
    }

    // ======================================================= WS DTOs

    @Test
    void gameEventMessage_constructorsAndGetters() {
        GameEventMessage r = new GameEventMessage();
        assertNull(r.getGameId());

        LocalDateTime now = LocalDateTime.now();
        GameEventMessage r2 = new GameEventMessage("game-1", "CARD_DRAWN", "payload", now);
        assertEquals("game-1", r2.getGameId());
        assertEquals("CARD_DRAWN", r2.getEventType());
        assertEquals("payload", r2.getPayload());
        assertEquals(now, r2.getTimestamp());

        r2.setGameId("game-2");
        r2.setEventType("ATTACK");
        r2.setPayload("pay2");
        r2.setTimestamp(LocalDateTime.now());
        assertEquals("game-2", r2.getGameId());
    }

    @Test
    void gameStateChangedMessage_constructorsAndGetters() {
        GameStateChangedMessage r = new GameStateChangedMessage();
        assertNull(r.getGameId());

        LocalDateTime now = LocalDateTime.now();
        GameStateChangedMessage r2 = new GameStateChangedMessage("game-1", "END_TURN", "ACTIVE", now);
        assertEquals("game-1", r2.getGameId());
        assertEquals("END_TURN", r2.getActionType());
        assertEquals("ACTIVE", r2.getStatus());
        assertEquals(now, r2.getTimestamp());

        r2.setGameId("g2");
        r2.setActionType("DRAW");
        r2.setStatus("FINISHED");
        r2.setTimestamp(LocalDateTime.now());
        assertEquals("g2", r2.getGameId());
    }

    // ======================================================= COMMON DTOs

    @Test
    void errorApi_builderAndGetters() {
        ErrorApi r = ErrorApi.builder()
                .timestamp("2024-01-01T00:00:00")
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/test")
                .build();
        assertEquals("2024-01-01T00:00:00", r.getTimestamp());
        assertEquals(404, r.getStatus());
        assertEquals("Not Found", r.getError());
        assertEquals("Resource not found", r.getMessage());
        assertEquals("/api/test", r.getPath());
        assertNotNull(r.toString());

        ErrorApi r2 = new ErrorApi();
        r2.setTimestamp("ts");
        r2.setStatus(500);
        r2.setError("err");
        r2.setMessage("msg");
        r2.setPath("/path");
        assertEquals("ts", r2.getTimestamp());
    }

    @Test
    void errorApi_equalsAndHashCode() {
        ErrorApi a = ErrorApi.builder().timestamp("t").status(404).error("e").message("m").path("p").build();
        ErrorApi b = ErrorApi.builder().timestamp("t").status(404).error("e").message("m").path("p").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
