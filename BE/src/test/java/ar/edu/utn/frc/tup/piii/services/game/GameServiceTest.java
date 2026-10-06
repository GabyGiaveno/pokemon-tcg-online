package ar.edu.utn.frc.tup.piii.services.game;

import ar.edu.utn.frc.tup.piii.dtos.request.GameActionApiRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.*;
import ar.edu.utn.frc.tup.piii.engine.GameEngineFacade;
import ar.edu.utn.frc.tup.piii.engine.GameEngineResult;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenException;
import ar.edu.utn.frc.tup.piii.exceptions.DeckValidationException;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.state.*;
import ar.edu.utn.frc.tup.piii.repositories.*;
import ar.edu.utn.frc.tup.piii.services.AchievementUnlockService;
import ar.edu.utn.frc.tup.piii.services.BadgeUnlockService;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private GameStateRepository gameStateRepository;
    @Mock private GameActionRepository gameActionRepository;
    @Mock private DeckService deckService;
    @Mock private PlayerRepository playerRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private CardCacheService cardCacheService;
    @Mock private GameEventPublisher gameEventPublisher;
    @Mock private GameEngineFacade gameEngineFacade;
    @Mock private PlayerStatsRepository playerStatsRepository;
    @Mock private BadgeUnlockService badgeUnlockService;
    @Mock private AchievementUnlockService achievementUnlockService;
    @Mock private ar.edu.utn.frc.tup.piii.services.GameActionLogService gameActionLogService;

    @Captor private ArgumentCaptor<GameState> stateCaptor;

    private ObjectMapper objectMapper;
    private GameService gameService;

    private Player player1;
    private Player player2;
    private Deck deck1;
    private Deck deck2;
    private UUID gameId;
    private GameSession session;
    private GameState gameState;
    private Card dummyPikachu;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gameService = new GameService(
                gameSessionRepository, gameStateRepository, gameActionRepository,
                playerRepository, deckRepository, cardCacheService,
                gameEventPublisher, gameEngineFacade, objectMapper,
                playerStatsRepository, badgeUnlockService, achievementUnlockService,
                gameActionLogService
        );

        player1 = Player.builder().id(1L).username("player1").build();
        player2 = Player.builder().id(2L).username("player2").build();
        gameId = UUID.randomUUID();

        dummyPikachu = Card.builder()
                .id("xy1-1")
                .name("Pikachu")
                .supertype("Pok\u00e9mon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .attacks("[{\"name\":\"Tackle\",\"damage\":\"30\"}]")
                .build();

        deck1 = Deck.builder()
                .id(1L)
                .name("Deck 1")
                .player(player1)
                .isValid(true)
                .cards(List.of(
                        DeckCard.builder()
                                .cardId(dummyPikachu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        deck2 = Deck.builder()
                .id(2L)
                .name("Deck 2")
                .player(player2)
                .isValid(true)
                .cards(List.of(
                        DeckCard.builder()
                                .cardId(dummyPikachu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        session = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .player2(player2)
                .deck1(deck1)
                .deck2(deck2)
                .status(GameStatus.ACTIVE)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        gameState = GameState.builder()
                .id(1L)
                .gameSession(session)
                .stateJson("{}")
                .turnNumber(1)
                .turnPhase(TurnPhase.DRAW)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private GameBoardState minimalBoard() {
        return GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L)
                .player2Id(2L)
                .currentPlayerId(1L)
                .turnNumber(1)
                .phase(TurnPhase.MAIN)
                .player1State(new PlayerBoardState())
                .player2State(new PlayerBoardState())
                .build();
    }

    private GameEngineResult successResult(GameBoardState updatedState) {
        GameEngineResult r = new GameEngineResult();
        r.setSuccess(true);
        r.setUpdatedState(updatedState != null ? updatedState : minimalBoard());
        r.setEvents(new ArrayList<>());
        return r;
    }

    private GameEngineResult failResult(String errorMessage) {
        GameEngineResult r = new GameEngineResult();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        return r;
    }

    @Test
    void createGame_createsWaitingGame() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck1));
        when(gameSessionRepository.save(any())).thenAnswer(i -> {
            GameSession s = i.getArgument(0);
            s.setId(gameId);
            return s;
        });

        GameSessionResponse response = gameService.createGame(1L, 1L);

        assertEquals(GameStatus.WAITING, response.getStatus());
        assertEquals("player1", response.getPlayer1Username());
        assertNull(response.getPlayer2Username());
    }

    @Test
    void createGame_invalidDeck_throwsDeckValidationException() {
        Deck invalidDeck = Deck.builder()
                .id(99L)
                .name("Invalid Deck")
                .player(player1)
                .isValid(false)
                .cards(List.of())
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findById(99L)).thenReturn(Optional.of(invalidDeck));

        assertThrows(DeckValidationException.class, () -> gameService.createGame(1L, 99L));
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void joinGame_setsStatusActive() {
        Card filler = Card.builder()
                .id("energy-1")
                .name("Fire Energy")
                .supertype("Energy")
                .subtypes(List.of("Basic"))
                .hp(null)
                .attacks("[]")
                .build();
        Deck bigDeck = Deck.builder()
                .id(10L)
                .name("Big Deck")
                .player(player2)
                .isValid(true)
                .cards(List.of(
                        DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build(),
                        DeckCard.builder().cardId(filler.getId()).quantity(59).build()
                ))
                .build();

        GameSession waitingSession = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .deck1(bigDeck)
                .status(GameStatus.WAITING)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waitingSession));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(deckRepository.findById(10L)).thenReturn(Optional.of(bigDeck));
        when(gameSessionRepository.save(any())).thenAnswer(i -> {
            GameSession s = i.getArgument(0);
            s.setId(gameId);
            return s;
        });
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        GameSessionResponse response = gameService.joinGame(gameId, 2L, 10L);

        assertEquals(GameStatus.READY_CHECK, response.getStatus());
        assertEquals("player2", response.getPlayer2Username());
    }

    @Test
    void joinGame_cannotJoinOwnGame() {
        GameSession waitingSession = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .deck1(deck1)
                .status(GameStatus.WAITING)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waitingSession));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));

        assertThrows(ForbiddenException.class, () -> gameService.joinGame(gameId, 1L, 1L));
        verify(deckRepository, never()).findById(anyLong());
    }

    @Test
    void joinGame_invalidDeck_throwsDeckValidationException() {
        Deck invalidDeck = Deck.builder()
                .id(10L)
                .name("Bad Deck")
                .player(player2)
                .isValid(false)
                .cards(List.of())
                .build();

        GameSession waitingSession = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .deck1(deck1)
                .status(GameStatus.WAITING)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waitingSession));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(deckRepository.findById(10L)).thenReturn(Optional.of(invalidDeck));

        assertThrows(DeckValidationException.class, () -> gameService.joinGame(gameId, 2L, 10L));
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void getGameState_player1SeesOwnHand() {
        CardInstanceState handCard = CardInstanceState.builder()
                .instanceId("inst-xy1-1")
                .cardId("xy1-1")
                .name("Pikachu")
                .build();
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L)
                .hand(new ArrayList<>(List.of(handCard)))
                .deck(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L)
                .hand(new ArrayList<>())
                .deck(new ArrayList<>())
                .prizeCards(new ArrayList<>())
                .discardPile(new ArrayList<>())
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(1).phase(TurnPhase.DRAW)
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertNotNull(state.getMyField().getHand());
        assertEquals(1, state.getMyField().getHand().size());
        assertEquals("xy1-1", state.getMyField().getHand().get(0).getCardId());
        assertEquals("inst-xy1-1", state.getMyField().getHand().get(0).getInstanceId());
    }

    @Test
    void getGameState_waitingReturnsEmptyBoard() {
        GameSession waitingSession = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .player2(player2)
                .deck1(deck1)
                .deck2(deck2)
                .status(GameStatus.WAITING)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waitingSession));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertEquals(GameStatus.WAITING, state.getStatus());
        assertFalse(state.isMyTurn());
        assertEquals(0, state.getTurnNumber());
        assertNotNull(state.getMyField());
        assertNotNull(state.getOpponentField());
    }

    @Test
    void getGameState_readyCheckReturnsReadyFlags() {
        GameSession readySession = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .player2(player2)
                .deck1(deck1)
                .deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .player1Ready(true)
                .player2Ready(false)
                .coinFlipWinnerId(1L)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertEquals(GameStatus.READY_CHECK, state.getStatus());
        assertTrue(state.isMyReady());
        assertFalse(state.isOpponentReady());
        assertEquals(1L, state.getCoinFlipWinnerId());
    }

    @Test
    void getGameState_player1DoesNotSeeOpponentHand() {
        CardInstanceState p2HandCard = CardInstanceState.builder()
                .instanceId("sec-1").cardId("sec-1").build();
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L).hand(new ArrayList<>(List.of(p2HandCard)))
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(1).phase(TurnPhase.DRAW)
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertEquals(1, state.getOpponentField().getHandSize());
        assertTrue(state.getMyField().getHand().isEmpty());
    }

    @Test
    void getGameState_exposesPendingSelection() {
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(3).phase(TurnPhase.ATTACK)
                .player1State(p1State).player2State(p2State)
                .pendingSelection(ar.edu.utn.frc.tup.piii.models.game.PendingSelection.builder()
                        .type(ar.edu.utn.frc.tup.piii.models.game.SelectionType.CHOOSE_ACTIVE_ON_KO)
                        .ownerPlayerId(2L)
                        .validOptions(new ArrayList<>(List.of("xy1-5", "xy1-9")))
                        .prompt("Choose which Benched Pokémon to promote to Active.")
                        .build())
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 2L);

        assertNotNull(state.getPendingSelection(), "pendingSelection must be exposed to the FE");
        assertEquals("CHOOSE_ACTIVE_ON_KO", state.getPendingSelection().getType());
        assertEquals(2L, state.getPendingSelection().getOwnerPlayerId());
        assertEquals(List.of("xy1-5", "xy1-9"), state.getPendingSelection().getValidOptions());
    }

    @Test
    void getGameState_exposesGameOutcome() {
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(2L).turnNumber(9).phase(TurnPhase.BETWEEN_TURNS)
                .matchState(ar.edu.utn.frc.tup.piii.models.cards.GameStatus.FINISHED)
                .winnerPlayerId(2L)
                .finishedReason("ALL_PRIZES_TAKEN")
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertEquals(ar.edu.utn.frc.tup.piii.models.cards.GameStatus.FINISHED, state.getStatus());
        assertEquals(2L, state.getWinnerId());
        assertEquals("ALL_PRIZES_TAKEN", state.getFinishedReason());
    }

    @Test
    void getGameState_player1DoesNotSeeOpponentExactPrizeCards() {
        CardInstanceState p2Prize = CardInstanceState.builder()
                .instanceId("prize-sec").cardId("prize-sec").build();
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L)
                .prizeCards(new ArrayList<>(List.of(p2Prize)))
                .hand(new ArrayList<>()).deck(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(1).phase(TurnPhase.DRAW)
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertNotNull(state.getOpponentField().getPrizeCards());
        assertEquals(1, state.getOpponentField().getPrizeCards().size());
        assertNull(state.getOpponentField().getPrizeCards().get(0));
    }

    @Test
    void getGameState_nullBoardFields_handlesGracefully() throws Exception {
        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L)
                // hand, deck, prizeCards, discardPile all null
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L)
                .build();
        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(1).phase(TurnPhase.MAIN)
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        // Should not throw — null collections default to empty lists
        assertNotNull(state.getMyField().getHand());
        assertNotNull(state.getMyField().getPrizeCards());
        assertNotNull(state.getMyField().getDiscardPile());
        assertNotNull(state.getOpponentField().getPrizeCards());
        assertNotNull(state.getOpponentField().getDiscardPile());
    }

    @Test
    void getGameState_player1SeesOpponentPublicInfo() {
        PokemonInPlayState active = PokemonInPlayState.builder()
                .instanceId("active-xy1-2").cardId("xy1-2")
                .currentHp(80).maxHp(80).build();
        PokemonInPlayState benchPoke = PokemonInPlayState.builder()
                .instanceId("bench-xy1-3").cardId("xy1-3")
                .currentHp(50).maxHp(50).build();
        CardInstanceState discard = CardInstanceState.builder()
                .instanceId("fire-1").cardId("fire-1").build();

        PlayerBoardState p1State = PlayerBoardState.builder()
                .playerId(1L).hand(new ArrayList<>())
                .deck(new ArrayList<>()).prizeCards(new ArrayList<>()).discardPile(new ArrayList<>())
                .build();
        PlayerBoardState p2State = PlayerBoardState.builder()
                .playerId(2L).hand(new ArrayList<>())
                .deck(new ArrayList<>(List.of(CardInstanceState.builder().instanceId("deck-1").cardId("deck-1").build())))
                .prizeCards(new ArrayList<>(List.of(CardInstanceState.builder().instanceId("prize-1").cardId("prize-1").build())))
                .discardPile(new ArrayList<>(List.of(discard)))
                .activePokemon(active)
                .bench(new ArrayList<>(List.of(benchPoke)))
                .build();

        GameBoardState board = GameBoardState.builder()
                .gameId(gameId.toString())
                .player1Id(1L).player2Id(2L)
                .currentPlayerId(1L).turnNumber(1).phase(TurnPhase.DRAW)
                .player1State(p1State).player2State(p2State)
                .build();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        gameState.setStateJson(serialize(board));

        BoardStateDTO state = gameService.getGameState(gameId, 1L);

        assertNotNull(state.getOpponentField().getActivePokemon());
        assertEquals(80, state.getOpponentField().getActivePokemon().getHp());
        assertNotNull(state.getOpponentField().getBench());
        assertEquals(1, state.getOpponentField().getBench().size());
        assertEquals(1, state.getOpponentField().getDeckSize());
        assertEquals(1, state.getOpponentField().getDiscardPile().size());
    }

    @Test
    void performGameAction_nonParticipant_throwsForbidden() {
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();
        assertThrows(ForbiddenException.class, () ->
                gameService.performGameAction(gameId, req, 999L));
    }

    @Test
    void performGameAction_gameFinished_returnsError() {
        session.setStatus(GameStatus.FINISHED);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);
        assertFalse(response.isSuccess());
        assertEquals("Game is already finished", response.getError());
    }

    @Test
    void performGameAction_gameNotActive_returnsError() {
        session.setStatus(GameStatus.WAITING);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);
        assertFalse(response.isSuccess());
        verify(gameStateRepository, never()).findByGameSessionId(any());
    }

    @Test
    void performGameAction_gameFinished_awardsXpAndUpdatesLevel() throws Exception {
        session.setStatus(GameStatus.ACTIVE);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerStatsRepository.findByPlayerId(1L)).thenReturn(Optional.of(PlayerStats.builder()
                .player(player1)
                .wins(0)
                .losses(0)
                .streak(0)
                .totalCards(0)
                .decksCreated(0)
                .build()));
        when(playerStatsRepository.findByPlayerId(2L)).thenReturn(Optional.of(PlayerStats.builder()
                .player(player2)
                .wins(0)
                .losses(0)
                .streak(0)
                .totalCards(0)
                .decksCreated(0)
                .build()));

        GameBoardState board = minimalBoard();
        gameState.setStateJson(serialize(board));

        GameEngineResult finished = successResult(board);
        finished.setGameFinished(true);
        finished.setWinnerPlayerId(1L);
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any())).thenReturn(finished);

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD)
                .build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);

        assertTrue(response.isSuccess());
        verify(playerRepository, atLeastOnce()).save(argThat(p -> p.getId().equals(1L) && p.getXp() == 25 && p.getLevel() == 1));
        verify(playerRepository, atLeastOnce()).save(argThat(p -> p.getId().equals(2L) && p.getXp() == 10 && p.getLevel() == 1));
        verify(playerStatsRepository).save(argThat(s -> s.getWins() == 1 && s.getStreak() == 1));
        verify(playerStatsRepository).save(argThat(s -> s.getLosses() == 1 && s.getStreak() == 0));
    }

    @Test
    void concede_allowedWhenNotActive() {
        session.setStatus(GameStatus.SETUP);
        GameBoardState board = minimalBoard();

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        gameState.setStateJson(serialize(board));

        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any()))
                .thenReturn(successResult(board));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.CONCEDE).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);
        assertTrue(response.isSuccess());
    }

    // ================================================================
    //  performGameAction — logging
    // ================================================================

    @Test
    void performGameAction_engineFailure_logsFailureAndReturnsError() throws Exception {
        session.setStatus(GameStatus.ACTIVE);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        GameBoardState board = minimalBoard();
        gameState.setStateJson(serialize(board));
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any()))
                .thenReturn(failResult("Not your turn"));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);

        assertFalse(response.isSuccess());
        verify(gameActionLogService).logFailure(
                eq(session), eq(ActionType.DRAW_CARD), eq(1L), anyInt(), eq("Not your turn"));
        verify(gameActionLogService, never()).logSuccess(any(), any(), anyLong(), anyInt(), any());
    }

    @Test
    void performGameAction_success_logsSuccess() throws Exception {
        session.setStatus(GameStatus.ACTIVE);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        GameBoardState board = minimalBoard();
        gameState.setStateJson(serialize(board));
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any()))
                .thenReturn(successResult(board));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);

        assertTrue(response.isSuccess());
        verify(gameActionLogService).logSuccess(eq(session), any(), eq(1L), anyInt(), any());
        verify(gameActionLogService, never()).logFailure(any(), any(), anyLong(), anyInt(), any());
    }

    // ================================================================
    //  getActionLog
    // ================================================================

    @Test
    void getActionLog_returnsOrderedActions() {
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        ar.edu.utn.frc.tup.piii.entities.GameAction action =
                ar.edu.utn.frc.tup.piii.entities.GameAction.builder()
                        .id(1L)
                        .gameSession(session)
                        .turnNumber(1)
                        .playerId(1L)
                        .actionType(ActionType.DRAW_CARD)
                        .payload("{}")
                        .result("{\"status\":\"SUCCESS\"}")
                        .timestamp(java.time.LocalDateTime.now())
                        .build();
        when(gameActionRepository.findByGameSession_IdOrderByTurnNumberAscIdAsc(gameId))
                .thenReturn(List.of(action));

        List<ar.edu.utn.frc.tup.piii.dtos.response.GameActionDto> log =
                gameService.getActionLog(gameId, 1L);

        assertEquals(1, log.size());
        assertEquals(1L, log.get(0).getId());
        assertEquals(ActionType.DRAW_CARD, log.get(0).getActionType());
    }

    @Test
    void getActionLog_nonParticipant_throwsForbidden() {
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        assertThrows(ForbiddenException.class, () -> gameService.getActionLog(gameId, 999L));
    }

    @Test
    void getActionLog_gameNotFound_throwsException() {
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThrows(ar.edu.utn.frc.tup.piii.exceptions.GameNotFoundException.class,
                () -> gameService.getActionLog(gameId, 1L));
    }

    // ================================================================
    //  startSuddenDeath
    // ================================================================

    @Test
    void startSuddenDeath_incrementsRoundAndSetsOnePrize() throws Exception {
        // Arrange: session with prizeCardsCount=6 and no previous sudden death
        session.setPrizeCardsCount(6);
        session.setSuddenDeathRound(0);
        session.setCurrentPlayerId(1L);

        // Build a minimal 60-card deck so initializeGame can produce a board
        Card filler = Card.builder()
                .id("fire-energy").name("Fire Energy")
                .supertype("Energy").subtypes(List.of("Basic"))
                .hp(null).attacks("[]").build();
        List<DeckCard> sixtyCards = new ArrayList<>();
        sixtyCards.add(DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build());
        sixtyCards.add(DeckCard.builder().cardId(filler.getId()).quantity(59).build());
        deck1 = Deck.builder().id(1L).name("D1").player(player1).isValid(true).cards(sixtyCards).build();
        deck2 = Deck.builder().id(2L).name("D2").player(player2).isValid(true).cards(sixtyCards).build();
        session.setDeck1(deck1);
        session.setDeck2(deck2);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        // Act
        gameService.startSuddenDeath(gameId);

        // Assert: session updated with round=1 and prizeCardsCount=1
        assertEquals(1, session.getSuddenDeathRound());
        assertEquals(1, session.getPrizeCardsCount());
        assertEquals(GameStatus.SETUP, session.getStatus());

        // Assert: board was re-initialized (gameState saved at least once)
        verify(gameStateRepository, atLeastOnce()).save(stateCaptor.capture());
        GameState savedState = stateCaptor.getValue();
        GameBoardState savedBoard = objectMapper.readValue(savedState.getStateJson(), GameBoardState.class);
        assertEquals(GameStatus.SETUP, savedBoard.getMatchState());
        assertEquals(TurnPhase.SETUP, savedBoard.getPhase());
        assertEquals(1, savedBoard.getPlayer1State().getPrizeCards().size());
        assertEquals(1, savedBoard.getPlayer2State().getPrizeCards().size());

        // Assert: REST reload/reconnect can reconstruct Sudden Death without WebSocket.
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(savedState));
        BoardStateDTO reloadedState = gameService.getGameState(gameId, player1.getId());
        assertEquals(GameStatus.SETUP, reloadedState.getStatus());
        assertEquals(1, reloadedState.getSuddenDeathRound());

        // Assert: SUDDEN_DEATH_START event was published
        verify(gameEventPublisher).publishGameEvents(eq(gameId), argThat(events ->
                events.stream().anyMatch(e -> e.getType().name().equals("SUDDEN_DEATH_START"))));
        verify(gameEventPublisher).publishStateChanged(
                eq(gameId), eq("SUDDEN_DEATH_START"), eq(GameStatus.SETUP.name()));
    }

    @Test
    void startSuddenDeath_multipleDraws_incrementsRoundEachTime() throws Exception {
        // Second consecutive sudden death round
        session.setPrizeCardsCount(1);
        session.setSuddenDeathRound(1);
        session.setCurrentPlayerId(2L);

        Card filler = Card.builder()
                .id("fire-energy").name("Fire Energy")
                .supertype("Energy").subtypes(List.of("Basic"))
                .hp(null).attacks("[]").build();
        List<DeckCard> sixtyCards = new ArrayList<>();
        sixtyCards.add(DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build());
        sixtyCards.add(DeckCard.builder().cardId(filler.getId()).quantity(59).build());
        deck1 = Deck.builder().id(1L).name("D1").player(player1).isValid(true).cards(sixtyCards).build();
        deck2 = Deck.builder().id(2L).name("D2").player(player2).isValid(true).cards(sixtyCards).build();
        session.setDeck1(deck1);
        session.setDeck2(deck2);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        gameService.startSuddenDeath(gameId);

        assertEquals(2, session.getSuddenDeathRound());
        assertEquals(1, session.getPrizeCardsCount());
    }

    // ================================================================
    //  getWaitingGames
    // ================================================================

    @Test
    void getWaitingGames_returnsWaitingSessions() {
        GameSession waiting = GameSession.builder()
                .id(gameId).player1(player1).deck1(deck1)
                .status(GameStatus.WAITING).prizeCardsCount(6)
                .createdAt(LocalDateTime.now()).build();
        when(gameSessionRepository.findByStatus(GameStatus.WAITING))
                .thenReturn(List.of(waiting));

        List<GameSessionResponse> result = gameService.getWaitingGames();

        assertEquals(1, result.size());
        assertEquals(GameStatus.WAITING, result.get(0).getStatus());
    }

    // ================================================================
    //  createGame — deck ownership
    // ================================================================

    @Test
    void createGame_deckNotOwnedByPlayer_throws() {
        Player other = Player.builder().id(99L).username("other").build();
        Deck otherDeck = Deck.builder().id(99L).name("Other Deck")
                .player(other).isValid(true).cards(List.of()).build();
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findById(99L)).thenReturn(Optional.of(otherDeck));

        assertThrows(ForbiddenException.class, () -> gameService.createGame(1L, 99L));
    }

    // ================================================================
    //  joinGame — edge cases
    // ================================================================

    @Test
    void joinGame_notWaiting_throws() {
        GameSession activeSession = GameSession.builder()
                .id(gameId).player1(player1).deck1(deck1)
                .status(GameStatus.ACTIVE).prizeCardsCount(6)
                .createdAt(LocalDateTime.now()).build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(activeSession));

        assertThrows(IllegalStateException.class,
                () -> gameService.joinGame(gameId, 2L, 2L));
    }

    @Test
    void joinGame_deckNotOwnedByJoiner_throws() {
        Player other = Player.builder().id(99L).username("other").build();
        Deck otherDeck = Deck.builder().id(99L).name("Other Deck")
                .player(other).isValid(true).cards(List.of()).build();
        GameSession waiting = GameSession.builder()
                .id(gameId).player1(player1).deck1(deck1)
                .status(GameStatus.WAITING).prizeCardsCount(6)
                .createdAt(LocalDateTime.now()).build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waiting));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(deckRepository.findById(99L)).thenReturn(Optional.of(otherDeck));

        assertThrows(ForbiddenException.class,
                () -> gameService.joinGame(gameId, 2L, 99L));
    }

    // ================================================================
    //  markReady
    // ================================================================

    @Test
    void markReady_notReadyCheck_throws() {
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class,
                () -> gameService.markReady(gameId, 1L));
    }

    @Test
    void markReady_player1Ready_butNotBoth() {
        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .player1Ready(false).player2Ready(false)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Player 1 marks ready
        try (var tsm = mockStatic(org.springframework.transaction.support.TransactionSynchronizationManager.class)) {
            tsm.when(() -> org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(any()))
                .thenAnswer(i -> null);
            gameService.markReady(gameId, 1L);
        }

        assertTrue(readySession.isPlayer1Ready());
        assertFalse(readySession.isPlayer2Ready());
        assertNull(readySession.getCoinFlipWinnerId());
        verify(gameSessionRepository, atLeastOnce()).findById(gameId);
    }

    @Test
    void markReady_bothReady_flipsCoin() throws Exception {
        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .player1Ready(true).player2Ready(false)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Player 2 marks ready → both ready → coin flip
        try (var tsm = mockStatic(org.springframework.transaction.support.TransactionSynchronizationManager.class)) {
            tsm.when(() -> org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(any()))
                .thenAnswer(i -> null);
            gameService.markReady(gameId, 2L);
        }

        assertTrue(readySession.isPlayer1Ready());
        assertTrue(readySession.isPlayer2Ready());
        assertNotNull(readySession.getCoinFlipWinnerId(),
                "Coin flip winner must be set when both players are ready");
        verify(gameEventPublisher).publishGameEvents(eq(gameId), argThat(events ->
                events.stream().anyMatch(e -> e.getType().name().equals("COIN_FLIPPED"))));
    }

    // ================================================================
    //  chooseFirstPlayer
    // ================================================================

    @Test
    void chooseFirstPlayer_notReadyCheckOrNoCoinFlip_throws() {
        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.ACTIVE)
                .coinFlipWinnerId(null)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));

        assertThrows(IllegalStateException.class,
                () -> gameService.chooseFirstPlayer(gameId, 1L, "ME"));
    }

    @Test
    void chooseFirstPlayer_notWinner_throws() {
        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .coinFlipWinnerId(1L)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));

        assertThrows(ForbiddenException.class,
                () -> gameService.chooseFirstPlayer(gameId, 2L, "ME"));
    }

    @Test
    void chooseFirstPlayer_starterMe_initializesGame() throws Exception {
        Card filler = Card.builder()
                .id("energy-1").name("Fire Energy")
                .supertype("Energy").subtypes(List.of("Basic"))
                .hp(null).attacks("[]").build();
        List<DeckCard> sixtyCards = new ArrayList<>();
        sixtyCards.add(DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build());
        sixtyCards.add(DeckCard.builder().cardId(filler.getId()).quantity(59).build());
        deck1 = Deck.builder().id(1L).name("D1").player(player1).isValid(true).cards(sixtyCards).build();
        deck2 = Deck.builder().id(2L).name("D2").player(player2).isValid(true).cards(sixtyCards).build();

        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .coinFlipWinnerId(1L)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        try (var tsm = mockStatic(org.springframework.transaction.support.TransactionSynchronizationManager.class)) {
            tsm.when(() -> org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(any()))
                .thenAnswer(i -> null);
            BoardStateDTO dto = gameService.chooseFirstPlayer(gameId, 1L, "ME");
            assertEquals(GameStatus.SETUP, dto.getStatus());
        }
        verify(gameStateRepository, atLeastOnce()).save(any());
    }

    @Test
    void chooseFirstPlayer_starterOpponent_initializesGame() throws Exception {
        Card filler = Card.builder()
                .id("energy-1").name("Fire Energy")
                .supertype("Energy").subtypes(List.of("Basic"))
                .hp(null).attacks("[]").build();
        List<DeckCard> sixtyCards = new ArrayList<>();
        sixtyCards.add(DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build());
        sixtyCards.add(DeckCard.builder().cardId(filler.getId()).quantity(59).build());
        deck1 = Deck.builder().id(1L).name("D1").player(player1).isValid(true).cards(sixtyCards).build();
        deck2 = Deck.builder().id(2L).name("D2").player(player2).isValid(true).cards(sixtyCards).build();

        GameSession readySession = GameSession.builder()
                .id(gameId).player1(player1).player2(player2)
                .deck1(deck1).deck2(deck2)
                .status(GameStatus.READY_CHECK)
                .coinFlipWinnerId(1L)
                .prizeCardsCount(6).createdAt(LocalDateTime.now())
                .build();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(readySession));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        try (var tsm = mockStatic(org.springframework.transaction.support.TransactionSynchronizationManager.class)) {
            tsm.when(() -> org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(any()))
                .thenAnswer(i -> null);
            BoardStateDTO dto = gameService.chooseFirstPlayer(gameId, 1L, "OPPONENT");
            assertEquals(GameStatus.SETUP, dto.getStatus());
        }
        verify(gameStateRepository, atLeastOnce()).save(any());
    }

    // ================================================================
    //  performGameAction — status check (not ACTIVE/SETUP, not CONCEDE)
    // ================================================================

    @Test
    void performGameAction_readtCheckWithoutConcede_denies() {
        session.setStatus(GameStatus.READY_CHECK);
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);
        assertFalse(response.isSuccess());
    }

    // ================================================================
    //  performGameAction — SIMULTANEOUS_KO → Sudden Death
    // ================================================================

    @Test
    void performGameAction_simultaneousKo_triggersSuddenDeath() throws Exception {
        // Arrange: session in ACTIVE with 60-card decks for Sudden Death re-init
        Card filler = Card.builder()
                .id("energy-1").name("Fire Energy")
                .supertype("Energy").subtypes(List.of("Basic"))
                .hp(null).attacks("[]").build();
        List<DeckCard> sixtyCards = new ArrayList<>();
        sixtyCards.add(DeckCard.builder().cardId(dummyPikachu.getId()).quantity(1).build());
        sixtyCards.add(DeckCard.builder().cardId(filler.getId()).quantity(59).build());
        deck1 = Deck.builder().id(1L).name("D1").player(player1).isValid(true).cards(sixtyCards).build();
        deck2 = Deck.builder().id(2L).name("D2").player(player2).isValid(true).cards(sixtyCards).build();
        session.setDeck1(deck1);
        session.setDeck2(deck2);
        session.setStatus(GameStatus.ACTIVE);

        GameBoardState board = minimalBoard();
        gameState.setStateJson(serialize(board));

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(dummyPikachu.getId())).thenReturn(dummyPikachu);
        lenient().when(cardCacheService.findById(filler.getId())).thenReturn(filler);

        GameEngineResult simKo = successResult(board);
        simKo.setGameFinished(true);
        simKo.setFinishedReason("SIMULTANEOUS_KO");
        simKo.setWinnerPlayerId(null);
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any())).thenReturn(simKo);

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.USE_ATTACK).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);

        // SIMULTANEOUS_KO → early return success
        assertTrue(response.isSuccess());
        // Session should have incremented sudden death round
        assertEquals(1, session.getSuddenDeathRound());
        verify(gameEventPublisher).publishGameEvents(eq(gameId), argThat(events ->
                events.stream().anyMatch(e -> e.getType().name().equals("SUDDEN_DEATH_START"))));
    }

    // ================================================================
    //  performGameAction — SETUP → ACTIVE transition
    // ================================================================

    @Test
    void performGameAction_withEvents_convertsToDomainEvents() throws Exception {
        session.setStatus(GameStatus.ACTIVE);
        GameBoardState board = minimalBoard();
        gameState.setStateJson(serialize(board));

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));

        GameEventDTO event = new GameEventDTO();
        event.setType("DRAW_CARD");
        event.setPayload(java.util.Map.of("cardId", "xy1-1"));

        GameEngineResult result = successResult(board);
        result.setEvents(List.of(event));
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any())).thenReturn(result);

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);
        assertTrue(response.isSuccess());
        verify(gameEventPublisher).publishGameEvents(eq(gameId), anyList());
    }

    @Test
    void performGameAction_setupTransitionToActive() throws Exception {
        session.setStatus(GameStatus.SETUP);
        GameBoardState board = minimalBoard();
        board.setMatchState(GameStatus.ACTIVE);
        gameState.setStateJson(serialize(board));

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameStateRepository.findByGameSessionId(gameId)).thenReturn(Optional.of(gameState));
        when(gameStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cardCacheService.findById(any())).thenReturn(dummyPikachu);

        GameEngineResult result = successResult(board);
        result.setUpdatedState(board);
        when(gameEngineFacade.applyAction(any(), anyLong(), any(), any())).thenReturn(result);

        GameActionApiRequest req = GameActionApiRequest.builder()
                .type(ActionType.DRAW_CARD).build();

        GameActionApiResponse response = gameService.performGameAction(gameId, req, 1L);

        assertTrue(response.isSuccess());
        assertEquals(GameStatus.ACTIVE, session.getStatus());
    }

    private String serialize(GameBoardState board) {
        try { return objectMapper.writeValueAsString(board); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
