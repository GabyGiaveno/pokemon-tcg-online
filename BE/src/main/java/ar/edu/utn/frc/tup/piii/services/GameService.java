package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.GameActionApiRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.*;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.GameEngineFacade;
import ar.edu.utn.frc.tup.piii.engine.GameEngineResult;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.exceptions.*;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.game.state.*;
import ar.edu.utn.frc.tup.piii.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameSessionRepository gameSessionRepository;
    private final GameStateRepository gameStateRepository;
    private final GameActionRepository gameActionRepository;
    private final PlayerRepository playerRepository;
    private final DeckRepository deckRepository;
    private final CardCacheService cardCacheService;
    private final GameEventPublisher gameEventPublisher;
    private final GameEngineFacade gameEngineFacade;
    private final ObjectMapper objectMapper;
    private final PlayerStatsRepository playerStatsRepository;
    private final BadgeUnlockService badgeUnlockService;
    private final AchievementUnlockService achievementUnlockService;
    private final GameActionLogService gameActionLogService;
    private final Random random = new Random();

    public GameService(GameSessionRepository gameSessionRepository,
                       GameStateRepository gameStateRepository,
                       GameActionRepository gameActionRepository,
                       PlayerRepository playerRepository,
                       DeckRepository deckRepository,
                        CardCacheService cardCacheService,
                        GameEventPublisher gameEventPublisher,
                        GameEngineFacade gameEngineFacade,
                        ObjectMapper objectMapper,
                        PlayerStatsRepository playerStatsRepository,
                        BadgeUnlockService badgeUnlockService,
                        AchievementUnlockService achievementUnlockService,
                        GameActionLogService gameActionLogService) {
        this.gameSessionRepository = gameSessionRepository;
        this.gameStateRepository = gameStateRepository;
        this.gameActionRepository = gameActionRepository;
        this.playerRepository = playerRepository;
        this.deckRepository = deckRepository;
        this.cardCacheService = cardCacheService;
        this.gameEventPublisher = gameEventPublisher;
        this.gameEngineFacade = gameEngineFacade;
        this.objectMapper = objectMapper;
        this.playerStatsRepository = playerStatsRepository;
        this.badgeUnlockService = badgeUnlockService;
        this.achievementUnlockService = achievementUnlockService;
        this.gameActionLogService = gameActionLogService;
    }

    // ================================================================
    //  Game session lifecycle
    // ================================================================

    @Transactional
    public GameSessionResponse createGame(Long playerId, Long deckId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));

        if (!deck.getPlayer().getId().equals(player.getId())) {
            throw new ForbiddenException(
                    "Deck " + deckId + " does not belong to player " + player.getId());
        }

        if (!deck.isValid()) {
            throw new DeckValidationException(
                    "Deck " + deckId + " is invalid. Cannot create a game with an invalid deck.");
        }

        GameSession session = GameSession.builder()
                .player1(player)
                .deck1(deck)
                .status(GameStatus.WAITING)
                .prizeCardsCount(6)
                .createdAt(LocalDateTime.now())
                .build();

        session = gameSessionRepository.save(session);
        return toGameSessionResponse(session);
    }

    public List<GameSessionResponse> getWaitingGames() {
        return gameSessionRepository.findByStatus(GameStatus.WAITING).stream()
                .map(this::toGameSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GameSessionResponse joinGame(UUID gameId, Long playerId, Long deckId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException(
                    "Game " + gameId + " is not waiting for players. Current status: " + session.getStatus());
        }

        Player player2 = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        if (player2.getId().equals(session.getPlayer1().getId())) {
            throw new ForbiddenException("Player " + player2.getId() + " cannot join their own game " + gameId);
        }

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));

        if (!deck.getPlayer().getId().equals(player2.getId())) {
            throw new ForbiddenException(
                    "Deck " + deckId + " does not belong to player " + player2.getId());
        }

        if (!deck.isValid()) {
            throw new DeckValidationException(
                    "Deck " + deckId + " is invalid. Cannot join a game with an invalid deck.");
        }

        session.setPlayer2(player2);
        session.setDeck2(deck);
        // Both players must press "Listo" before the opening coin flip and setup (see markReady/chooseFirstPlayer).
        session.setStatus(GameStatus.READY_CHECK);
        session = gameSessionRepository.save(session);

        // Notify both players that the game state changed (opponent joined, status -> READY_CHECK)
        gameEventPublisher.publishStateChanged(session.getId(), "JOIN_GAME", session.getStatus().name());

        return toGameSessionResponse(session);
    }

    // ================================================================
    //  Ready check + opening coin flip (READY_CHECK phase)
    // ================================================================

    /**
     * Marks the requesting player as "Listo". Once BOTH players are ready, the opening coin
     * flip is performed: the winner is recorded (the only player allowed to choose who starts)
     * and a {@code COIN_FLIPPED} event is broadcast so both clients animate the same result.
     */
    @Transactional
    public BoardStateDTO markReady(UUID gameId, Long playerId) {
        GameSession session = requireParticipant(gameId, playerId);
        if (session.getStatus() != GameStatus.READY_CHECK) {
            throw new IllegalStateException("Game " + gameId + " is not in READY_CHECK.");
        }

        if (session.getPlayer1().getId().equals(playerId)) {
            session.setPlayer1Ready(true);
        } else {
            session.setPlayer2Ready(true);
        }

        // Both ready and no flip yet → flip the opening coin once.
        if (session.isPlayer1Ready() && session.isPlayer2Ready() && session.getCoinFlipWinnerId() == null) {
            boolean heads = random.nextBoolean();
            Long winnerId = heads ? session.getPlayer1().getId() : session.getPlayer2().getId();
            session.setCoinFlipWinnerId(winnerId);
            session = gameSessionRepository.save(session);

            gameEventPublisher.publishGameEvents(session.getId(), List.of(GameEvent.of(
                    session.getId(),
                    GameEventType.COIN_FLIPPED,
                    heads ? "Heads!" : "Tails!",
                    Map.of("flip", heads ? "HEADS" : "TAILS",
                            "reason", "INITIAL_TURN_ORDER",
                            "coinFlipWinnerId", winnerId))));
        } else {
            session = gameSessionRepository.save(session);
        }

        UUID sessionIdForEvent = session.getId();
        String statusForEvent = session.getStatus().name();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        gameEventPublisher.publishStateChanged(sessionIdForEvent, "READY", statusForEvent);
                    }
                });
        return getGameState(gameId, playerId);
    }

    /**
     * The coin-flip winner chooses who takes the first turn. Builds the SETUP board with the
     * chosen starter and advances the game to SETUP.
     */
    @Transactional
    public BoardStateDTO chooseFirstPlayer(UUID gameId, Long playerId, String starter) {
        GameSession session = requireParticipant(gameId, playerId);
        if (session.getStatus() != GameStatus.READY_CHECK || session.getCoinFlipWinnerId() == null) {
            throw new IllegalStateException("Game " + gameId + " is not awaiting a first-player choice.");
        }
        if (!session.getCoinFlipWinnerId().equals(playerId)) {
            throw new ForbiddenException("Only the coin-flip winner may choose who starts.");
        }

        // "ME" = the winner (caller) starts; "OPPONENT" = the other player starts.
        Long opponentId = session.getPlayer1().getId().equals(playerId)
                ? session.getPlayer2().getId()
                : session.getPlayer1().getId();
        Long starterId = "OPPONENT".equalsIgnoreCase(starter) ? opponentId : playerId;

        initializeGame(session, starterId);
        session.setCurrentPlayerId(starterId);
        session.setStatus(GameStatus.SETUP);
        gameSessionRepository.save(session);

        // Publish AFTER the transaction commits so REST reads see the updated status.
        UUID sessionId = session.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        gameEventPublisher.publishStateChanged(sessionId, "CHOOSE_FIRST", GameStatus.SETUP.name());
                    }
                });
        return getGameState(gameId, playerId);
    }

    /** Loads a session and asserts the player belongs to it. */
    private GameSession requireParticipant(UUID gameId, Long playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
        if (!session.getPlayer1().getId().equals(playerId)
                && (session.getPlayer2() == null || !session.getPlayer2().getId().equals(playerId))) {
            throw new ForbiddenException("Player " + playerId + " does not belong to game " + gameId);
        }
        return session;
    }

    // ================================================================
    //  Game state queries
    // ================================================================

    public BoardStateDTO getGameState(UUID gameId, Long playerId) {
        GameSession session = requireParticipant(gameId, playerId);

        if (session.getStatus() == GameStatus.WAITING) {
            BoardStateDTO dto = new BoardStateDTO();
            dto.setGameId(gameId.toString());
            dto.setCurrentPlayerId(null);
            dto.setPhase(null);
            dto.setTurnNumber(0);
            dto.setMyField(new PlayerFieldDTO());
            dto.setOpponentField(new OpponentFieldDTO());
            dto.setMyTurn(false);
            dto.setStatus(GameStatus.WAITING);
            dto.setSuddenDeathRound(session.getSuddenDeathRound());
            return dto;
        }

        if (session.getStatus() == GameStatus.READY_CHECK) {
            boolean isP1 = session.getPlayer1().getId().equals(playerId);
            BoardStateDTO dto = new BoardStateDTO();
            dto.setGameId(gameId.toString());
            dto.setStatus(GameStatus.READY_CHECK);
            dto.setMyField(new PlayerFieldDTO());
            dto.setOpponentField(new OpponentFieldDTO());
            dto.setMyReady(isP1 ? session.isPlayer1Ready() : session.isPlayer2Ready());
            dto.setOpponentReady(isP1 ? session.isPlayer2Ready() : session.isPlayer1Ready());
            dto.setCoinFlipWinnerId(session.getCoinFlipWinnerId());
            dto.setSuddenDeathRound(session.getSuddenDeathRound());
            return dto;
        }

        // Load state from DB
        GameState stateEntity = gameStateRepository.findByGameSessionId(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game state not found for game: " + gameId));

        GameBoardState board;
        try {
            board = objectMapper.readValue(stateEntity.getStateJson(), GameBoardState.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize game state", e);
        }

        boolean isPlayer1 = session.getPlayer1().getId().equals(playerId);
        PlayerBoardState myState = isPlayer1 ? board.getPlayer1State() : board.getPlayer2State();
        PlayerBoardState opponentState = isPlayer1 ? board.getPlayer2State() : board.getPlayer1State();

        BoardStateDTO dto = new BoardStateDTO();
        dto.setGameId(gameId.toString());
        dto.setCurrentPlayerId(board.getCurrentPlayerId());
        dto.setPhase(board.getPhase());
        dto.setTurnNumber(board.getTurnNumber());
        dto.setMyField(toPlayerFieldDTO(myState));
        dto.setOpponentField(toOpponentFieldDTO(opponentState));
        dto.setMyTurn(board.getCurrentPlayerId() != null && board.getCurrentPlayerId().equals(playerId));

        // Fase 6.2 — game lifecycle + paused selection, so the FE can route screens,
        // prompt the post-KO promotion, and show the winner with its precise reason.
        dto.setStatus(board.getMatchState());
        dto.setSuddenDeathRound(session.getSuddenDeathRound());
        dto.setWinnerId(board.getWinnerPlayerId());
        dto.setFinishedReason(board.getFinishedReason());
        if (board.getPendingSelection() != null) {
            var pending = board.getPendingSelection();
            dto.setPendingSelection(PendingSelectionDTO.builder()
                    .type(pending.getType() != null ? pending.getType().name() : null)
                    .ownerPlayerId(pending.getOwnerPlayerId())
                    .validOptions(pending.getValidOptions())
                    .prompt(pending.getPrompt())
                    .revealedCardIds(pending.getRevealedCardIds())
                    .selectionCount(pending.getSelectionCount())
                    .build());
        }

        return dto;
    }

    // ================================================================
    //  Action orchestration
    // ================================================================

    /**
     * Executes a game action from the frontend.
     *
     * <p>Translates the frontend-facing {@link GameActionApiRequest} into the
     * internal {@link ActionRequest} the facade expects, resolves
     * {@code cardInstanceId} to the correct ID for the engine, and adapts
     * the {@link CardLookup} so the engine can look up cards by either
     * instanceId or global cardId.
     */
    @Transactional
    public GameActionApiResponse performGameAction(UUID gameId, GameActionApiRequest apiRequest, Long playerId) {
        // 1. Load session + validate participant
        GameSession session = requireParticipant(gameId, playerId);

        // 2. Game already finished → reject
        if (session.getStatus() == GameStatus.FINISHED) {
            return GameActionApiResponse.fail(apiRequest.getType(), "Game is already finished");
        }

        // 3. CONCEDE allowed from any non-FINISHED state
        //    Other actions allowed during ACTIVE or SETUP
        if (apiRequest.getType() != ActionType.CONCEDE
                && session.getStatus() != GameStatus.ACTIVE
                && session.getStatus() != GameStatus.SETUP) {
            return GameActionApiResponse.fail(apiRequest.getType(),
                    "Game is not active. Current status: " + session.getStatus());
        }

        // 4. Load state from DB
        GameState stateEntity = gameStateRepository.findByGameSessionId(gameId)
                .orElseThrow(() -> new IllegalStateException("Game state not found"));

        try {
            GameBoardState boardState = objectMapper.readValue(stateEntity.getStateJson(), GameBoardState.class);

            // 5. Translate frontend request → internal ActionRequest
            ActionRequest request = toActionRequest(apiRequest, boardState);

            // 6. Build a smart CardLookup that resolves instanceIds → cardIds
            CardLookup lookup = buildCardLookup(boardState);

            // 7. Bridge to the engine (all game-rule resolution happens inside)
            GameEngineResult engineResult = gameEngineFacade.applyAction(boardState, playerId, request, lookup);

            // 8. Engine error → log failure and return without persisting
            if (!engineResult.isSuccess()) {
                int currentTurn = stateEntity.getTurnNumber();
                gameActionLogService.logFailure(session, apiRequest.getType(), playerId,
                        currentTurn, engineResult.getErrorMessage());
                return GameActionApiResponse.fail(apiRequest.getType(), engineResult.getErrorMessage());
            }

            // ======== Success path: persist + notify ========

            // 9. Save updated state to DB
            stateEntity.setStateJson(objectMapper.writeValueAsString(engineResult.getUpdatedState()));
            stateEntity.setTurnNumber(engineResult.getUpdatedState().getTurnNumber());
            stateEntity.setTurnPhase(engineResult.getUpdatedState().getPhase());
            stateEntity.setUpdatedAt(LocalDateTime.now());
            gameStateRepository.save(stateEntity);

            // 10. If game finished → update session
            if (engineResult.isGameFinished()) {
                // Simultaneous KO → Sudden Death (no winner yet, game continues)
                if ("SIMULTANEOUS_KO".equals(engineResult.getFinishedReason())) {
                    // State already persisted at step 9; log and notify, then start Sudden Death
                    gameActionLogService.logSuccess(session, request,
                            playerId, stateEntity.getTurnNumber(), engineResult.getEvents());

                    List<GameEvent> domainEvents = toDomainEvents(session.getId(), engineResult.getEvents());
                    gameEventPublisher.publishGameEvents(session.getId(), domainEvents);

                    startSuddenDeath(gameId);
                    return GameActionApiResponse.ok(apiRequest.getType(), engineResult.getEvents());
                }

                session.setStatus(GameStatus.FINISHED);
                session.setFinishedAt(LocalDateTime.now());
                Player winner = engineResult.getWinnerPlayerId() != null
                        ? playerRepository.findById(engineResult.getWinnerPlayerId()).orElse(null)
                        : null;
                session.setWinner(winner);
                gameSessionRepository.save(session);

                if (winner != null) {
                    Player loser = session.getPlayer1().getId().equals(winner.getId())
                            ? session.getPlayer2()
                            : session.getPlayer1();
                    updatePlayerStats(winner, loser);
                }
            }

            // 10b. If setup completed → transition session to ACTIVE
            if (session.getStatus() == GameStatus.SETUP
                    && engineResult.getUpdatedState().getMatchState() == GameStatus.ACTIVE) {
                session.setStatus(GameStatus.ACTIVE);
                gameSessionRepository.save(session);
            }

            // 11. Log action (logs the internal ActionRequest that was actually processed)
            gameActionLogService.logSuccess(session, request, playerId,
                    stateEntity.getTurnNumber(), engineResult.getEvents());

            // 12. Publish WebSocket events (convert DTOs → domain events for WS)
            List<GameEvent> domainEvents = toDomainEvents(session.getId(), engineResult.getEvents());
            gameEventPublisher.publishGameEvents(session.getId(), domainEvents);
            gameEventPublisher.publishStateChanged(
                    session.getId(),
                    request.getType().name(),
                    session.getStatus().name()
            );

            // 13. Return clean API response (uses GameEventDTO directly, no private state)
            return GameActionApiResponse.ok(apiRequest.getType(), engineResult.getEvents());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process JSON state", e);
        }
    }

    /**
     * Translates the frontend-facing {@link GameActionApiRequest} into the
     * internal {@link ActionRequest} that the facade/engine expects.
     *
     * <p>If the frontend provided a {@code cardInstanceId}, it is passed
     * through as the {@code cardId} in the internal request, because the
     * engine identifies cards by their instance UUID. The {@link CardLookup}
     * will resolve it back to the real cardId when looking up card details.
     */
    private ActionRequest toActionRequest(GameActionApiRequest apiReq, GameBoardState board) {
        // Determine which card id to pass to the engine:
        //   cardInstanceId (UUID) → the engine identifies cards in hand by instanceId
        //   cardId (pokemontcg.io) → fallback when frontend doesn't have instanceId
        String engineCardId = apiReq.getCardInstanceId() != null
                ? apiReq.getCardInstanceId()
                : apiReq.getCardId();

        return ActionRequest.builder()
                .type(apiReq.getType())
                .cardId(engineCardId)
                .targetPosition(apiReq.getTargetPosition())
                .attackIndex(apiReq.getAttackIndex())
                .benchIndex(apiReq.getBenchIndex())
                .prizeIndex(apiReq.getPrizeIndex())
                .orderedInstanceIds(apiReq.getOrderedInstanceIds())
                .build();
    }

    /**
     * Builds a smart {@link CardLookup} that:
     * <ol>
     *   <li>Tries the id as a global cardId directly via {@link CardRepository}.</li>
     *   <li>If not found, treats it as an instanceId and resolves it to a cardId
     *       from the current {@link GameBoardState}, then looks up in the repository.</li>
     * </ol>
     *
     * <p>This allows the engine to call {@code CardLookup.findById(request.cardId)}
     * regardless of whether the identifier is an instanceId or a global cardId.
     */
    private CardLookup buildCardLookup(GameBoardState boardState) {
        return id -> {
            // Try direct cardId lookup first via CardCacheService
            try {
                return cardCacheService.findById(id);
            } catch (IllegalArgumentException e) {
                // Not a cardId — try as instanceId
            }
            // Try as instanceId in the current board state
            CardInstanceState instance = findCardInstanceInBoard(boardState, id);
            if (instance != null) {
                return cardCacheService.findById(instance.getCardId());
            }
            throw new IllegalArgumentException("Card not resolvable: " + id);
        };
    }

    private void updatePlayerStats(Player winner, Player loser) {
        PlayerStats winnerStats = playerStatsRepository.findByPlayerId(winner.getId())
                .orElseGet(() -> PlayerStats.builder().player(winner).build());
        winnerStats.setWins(winnerStats.getWins() + 1);
        winnerStats.setStreak(winnerStats.getStreak() + 1);
        playerStatsRepository.save(winnerStats);

        awardMatchXp(winner, 25);

        int totalGames = winnerStats.getWins();
        if (loser != null) {
            PlayerStats loserStats = playerStatsRepository.findByPlayerId(loser.getId())
                    .orElseGet(() -> PlayerStats.builder().player(loser).build());
            loserStats.setLosses(loserStats.getLosses() + 1);
            loserStats.setStreak(0);
            playerStatsRepository.save(loserStats);
            totalGames += loserStats.getLosses();

            awardMatchXp(loser, 10);
        }

        badgeUnlockService.checkAndUnlock(winner, winnerStats.getStreak());
        achievementUnlockService.checkAndUnlock(winner,
                winnerStats.getWins(), totalGames, winner.getTotalCards());
    }

    private void awardMatchXp(Player player, int xpGain) {
        int newXp = Math.max(0, player.getXp() + xpGain);
        player.setXp(newXp);
        player.setLevel((newXp / 100) + 1);
        playerRepository.save(player);
    }

    /**
     * Searches the current {@link GameBoardState} for a {@link CardInstanceState}
     * matching the given instanceId.
     */
    private CardInstanceState findCardInstanceInBoard(GameBoardState board, String instanceId) {
        if (board == null || instanceId == null) return null;
        for (PlayerBoardState pbs : Arrays.asList(board.getPlayer1State(), board.getPlayer2State())) {
            if (pbs == null) continue;
            for (List<CardInstanceState> zone : Arrays.asList(
                    pbs.getHand(), pbs.getDeck(), pbs.getPrizeCards(), pbs.getDiscardPile())) {
                if (zone == null) continue;
                for (CardInstanceState cis : zone) {
                    if (cis != null && instanceId.equals(cis.getInstanceId())) {
                        return cis;
                    }
                }
            }
        }
        return null;
    }

    // ================================================================
    //  Sudden Death
    // ================================================================

    /**
     * Initiates a Sudden Death round after a simultaneous KO draw.
     *
     * <p>Reuses the same {@link GameSession}: increments {@code suddenDeathRound},
     * sets {@code prizeCardsCount=1}, re-initializes the board with 4-card hands
     * and 1 prize card each, leaves the persisted status in {@code SETUP}, and
     * publishes a {@code SUDDEN_DEATH_START} WebSocket event so clients can show the overlay.
     *
     * @param gameId the ID of the game session in a drawn state
     */
    @Transactional
    public void startSuddenDeath(UUID gameId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

        session.setSuddenDeathRound(session.getSuddenDeathRound() + 1);
        session.setPrizeCardsCount(1);
        session.setStatus(GameStatus.SETUP);
        gameSessionRepository.save(session);

        // Re-initialize the board: shuffle decks, deal 4 cards + 1 prize, set phase to SETUP
        initializeGame(session, session.getCurrentPlayerId());

        int round = session.getSuddenDeathRound();
        gameEventPublisher.publishGameEvents(gameId, List.of(GameEvent.of(
                gameId,
                GameEventType.SUDDEN_DEATH_START,
                "Sudden Death — Round " + round + ". 1 Prize Card each.",
                Map.of("suddenDeathRound", round, "prizeCardsCount", 1))));
        gameEventPublisher.publishStateChanged(gameId, "SUDDEN_DEATH_START", GameStatus.SETUP.name());
    }

    // ================================================================
    //  Game initialization (from actual deck cards)
    // ================================================================

    /** Builds and persists the initial SETUP board. {@code starterId} (chosen by the coin-flip winner) goes first. */
    private void initializeGame(GameSession session, Long starterId) {
        PlayerBoardState p1State = buildPlayerBoardFromDeck(session.getDeck1(), session.getPlayer1().getId(), session.getPrizeCardsCount());
        PlayerBoardState p2State = buildPlayerBoardFromDeck(session.getDeck2(), session.getPlayer2().getId(), session.getPrizeCardsCount());

        GameBoardState board = GameBoardState.builder()
                .gameId(session.getId().toString())
                .player1Id(session.getPlayer1().getId())
                .player2Id(session.getPlayer2().getId())
                .currentPlayerId(starterId)
                .turnNumber(0)
                .matchState(GameStatus.SETUP)
                .phase(TurnPhase.SETUP)
                .player1State(p1State)
                .player2State(p2State)
                .build();

        try {
            String json = objectMapper.writeValueAsString(board);
            // Upsert: if a GameState already exists for this session (idempotent re-init), update it.
            GameState stateEntity = gameStateRepository.findByGameSessionId(session.getId())
                    .orElseGet(() -> GameState.builder().gameSession(session).build());
            stateEntity.setStateJson(json);
            stateEntity.setTurnNumber(0);
            stateEntity.setTurnPhase(board.getPhase());
            stateEntity.setUpdatedAt(LocalDateTime.now());
            gameStateRepository.save(stateEntity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize initial game state", e);
        }
    }

    /**
     * Builds a PlayerBoardState from a player's actual deck cards.
     * Shuffles deck, draws {@code handSize} cards (with mulligan retries), sets {@code prizeCardsCount} prize cards.
     *
     * <p>For Sudden Death rounds: {@code prizeCardsCount=1} and {@code handSize=4} per official rules.
     */
    private PlayerBoardState buildPlayerBoardFromDeck(Deck deck, Long playerId, int prizeCardsCount) {
        int handSize = prizeCardsCount == 1 ? 4 : 7;

        // Expand DeckCard quantities to individual card IDs
        List<String> allCardIds = new ArrayList<>();
        for (DeckCard dc : deck.getCards()) {
            for (int i = 0; i < dc.getQuantity(); i++) {
                allCardIds.add(dc.getCardId());
            }
        }

        Collections.shuffle(allCardIds);

        // Mulligan: ensure hand has at least one Basic Pokémon
        List<String> deckPile = new ArrayList<>(allCardIds);
        List<String> handIds;
        List<String> remaining;

        int mulliganAttempts = 0;
        boolean validHand;

        do {
            handIds = new ArrayList<>(deckPile.subList(0, Math.min(handSize, deckPile.size())));
            remaining = new ArrayList<>(deckPile.subList(handIds.size(), deckPile.size()));

            validHand = hasBasicPokemon(handIds);
            if (!validHand) {
                deckPile = new ArrayList<>(allCardIds);
                Collections.shuffle(deckPile);
                mulliganAttempts++;
            }
        } while (!validHand && mulliganAttempts < 3);

        // If we exhausted retries, use whatever we have
        if (!validHand) {
            handIds = new ArrayList<>(deckPile.subList(0, Math.min(handSize, deckPile.size())));
            remaining = new ArrayList<>(deckPile.subList(handIds.size(), deckPile.size()));
        }

        // Convert hand IDs to CardInstanceState
        List<CardInstanceState> hand = handIds.stream()
                .map(cid -> CardInstanceState.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .cardId(cid)
                        .build())
                .collect(Collectors.toList());

        // Take prizeCardsCount from remaining → prize cards
        int prizeCount = Math.min(prizeCardsCount, remaining.size());
        List<String> prizeIds = new ArrayList<>(remaining.subList(0, prizeCount));
        List<String> deckIds = new ArrayList<>(remaining.subList(prizeCount, remaining.size()));

        List<CardInstanceState> prizeCards = prizeIds.stream()
                .map(cid -> CardInstanceState.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .cardId(cid)
                        .build())
                .collect(Collectors.toList());

        List<CardInstanceState> deckCards = deckIds.stream()
                .map(cid -> CardInstanceState.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .cardId(cid)
                        .build())
                .collect(Collectors.toList());

        return PlayerBoardState.builder()
                .playerId(playerId)
                .deck(deckCards)
                .hand(hand)
                .prizeCards(prizeCards)
                .discardPile(new ArrayList<>())
                .bench(new ArrayList<>())
                .hasAttachedEnergyThisTurn(false)
                .hasPlayedSupporterThisTurn(false)
                .build();
    }

    private boolean hasBasicPokemon(List<String> handCardIds) {
        for (String cardId : handCardIds) {
            try {
                Card card = cardCacheService.findById(cardId);
                if ("Pokémon".equals(card.getSupertype())
                        && card.getSubtypes() != null && card.getSubtypes().contains("Basic")) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // card not in cache, skip
            }
        }
        return false;
    }

    // ================================================================
    //  Action log query
    // ================================================================

    /**
     * Returns the ordered action log for a game session.
     * The requester must be player1 or player2; otherwise 403/404 is thrown.
     */
    public List<ar.edu.utn.frc.tup.piii.dtos.response.GameActionDto> getActionLog(UUID gameId, Long requesterId) {
        GameSession session = requireParticipant(gameId, requesterId);
        List<GameAction> actions = gameActionRepository.findByGameSession_IdOrderByTurnNumberAscIdAsc(session.getId());
        return actions.stream()
                .map(a -> ar.edu.utn.frc.tup.piii.dtos.response.GameActionDto.builder()
                        .id(a.getId())
                        .turnNumber(a.getTurnNumber())
                        .playerId(a.getPlayerId())
                        .actionType(a.getActionType())
                        .payload(a.getPayload())
                        .result(a.getResult())
                        .timestamp(a.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    // ================================================================
    //  DTO mappers (GameBoardState → BoardStateDTO)
    // ================================================================

    private GameSessionResponse toGameSessionResponse(GameSession session) {
        GameSessionResponse response = new GameSessionResponse();
        response.setGameId(session.getId());
        response.setStatus(session.getStatus());
        response.setPlayer1Username(session.getPlayer1().getUsername());
        response.setPlayer2Username(session.getPlayer2() != null ? session.getPlayer2().getUsername() : null);
        response.setCreatedAt(session.getCreatedAt());
        response.setPrizeCardsCount(session.getPrizeCardsCount());
        return response;
    }

    private PlayerFieldDTO toPlayerFieldDTO(PlayerBoardState state) {
        PlayerFieldDTO dto = new PlayerFieldDTO();
        dto.setActivePokemon(toActivePokemonDTO(state.getActivePokemon()));
        dto.setBench(toBenchPokemonDTOList(state.getBench()));
        dto.setHand(state.getHand() != null
                ? state.getHand().stream().map(this::toCardInstanceDTO).collect(Collectors.toList())
                : new ArrayList<>());
        dto.setDeckSize(state.getDeck() != null ? state.getDeck().size() : 0);
        // Prize cards are face-down — expose count and instanceId only, never cardId.
        dto.setPrizeCards(state.getPrizeCards() != null
                ? state.getPrizeCards().stream()
                        .map(c -> { CardInstanceDTO p = new CardInstanceDTO(); p.setInstanceId(c.getInstanceId()); return p; })
                        .collect(Collectors.toList())
                : new ArrayList<>());
        dto.setDiscardPile(state.getDiscardPile() != null
                ? state.getDiscardPile().stream().map(this::toCardInstanceDTO).collect(Collectors.toList())
                : new ArrayList<>());
        return dto;
    }

    private OpponentFieldDTO toOpponentFieldDTO(PlayerBoardState state) {
        OpponentFieldDTO dto = new OpponentFieldDTO();
        dto.setActivePokemon(toActivePokemonDTO(state.getActivePokemon()));
        dto.setBench(toBenchPokemonDTOList(state.getBench()));
        dto.setHandSize(state.getHand() != null ? state.getHand().size() : 0);
        dto.setDeckSize(state.getDeck() != null ? state.getDeck().size() : 0);
        // Opponent prize cards are hidden — expose as null array
        dto.setPrizeCards(state.getPrizeCards() != null
                ? Collections.nCopies(state.getPrizeCards().size(), (String) null)
                : new ArrayList<>());
        // Discard pile is public info
        dto.setDiscardPile(state.getDiscardPile() != null
                ? state.getDiscardPile().stream().map(this::toCardInstanceDTO).collect(Collectors.toList())
                : new ArrayList<>());
        return dto;
    }

    private ActivePokemonDTO toActivePokemonDTO(PokemonInPlayState pokemon) {
        if (pokemon == null) return null;
        ActivePokemonDTO dto = new ActivePokemonDTO();
        dto.setInstanceId(pokemon.getInstanceId());
        dto.setCardId(pokemon.getCardId());
        dto.setHp(pokemon.getCurrentHp());
        dto.setMaxHp(pokemon.getMaxHp());
        dto.setAttachedEnergies(pokemon.getAttachedEnergies() != null
                ? pokemon.getAttachedEnergies().stream()
                        .map(this::toCardInstanceDTO)
                        .collect(Collectors.toList())
                : new ArrayList<>());
        dto.setToolCard(pokemon.getAttachedTools() != null && !pokemon.getAttachedTools().isEmpty()
                ? toCardInstanceDTO(pokemon.getAttachedTools().get(0))
                : null);
        dto.setConditions(mapConditions(pokemon));
        return dto;
    }

    /**
     * Maps the engine-side status flags of a Pokémon into the short condition codes the
     * frontend renders as badges (ZZZ/PAR/CFZ/BRN/PSN). The primary special condition
     * (asleep/paralyzed/confused) is mutually exclusive; burn and poison are independent and
     * can coexist with it. An empty list means the Pokémon has no special conditions.
     */
    private List<String> mapConditions(PokemonInPlayState pokemon) {
        List<String> conditions = new ArrayList<>();
        String condition = pokemon.getCondition();
        if (condition != null) {
            switch (condition.toUpperCase()) {
                case "ASLEEP"    -> conditions.add("ZZZ");
                case "PARALYZED" -> conditions.add("PAR");
                case "CONFUSED"  -> conditions.add("CFZ");
                default          -> { /* NONE / unknown → no badge */ }
            }
        }
        if (pokemon.isBurned())   conditions.add("BRN");
        if (pokemon.isPoisoned()) conditions.add("PSN");
        return conditions;
    }

    private List<BenchPokemonDTO> toBenchPokemonDTOList(List<PokemonInPlayState> bench) {
        if (bench == null) return new ArrayList<>();
        return bench.stream().map(p -> {
            BenchPokemonDTO dto = new BenchPokemonDTO();
            dto.setInstanceId(p.getInstanceId());
            dto.setCardId(p.getCardId());
            dto.setHp(p.getCurrentHp());
            dto.setMaxHp(p.getMaxHp());
            dto.setAttachedEnergies(p.getAttachedEnergies() != null
                    ? p.getAttachedEnergies().stream()
                            .map(this::toCardInstanceDTO)
                            .collect(Collectors.toList())
                    : new ArrayList<>());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Maps a {@link CardInstanceState} (internal model) to a {@link CardInstanceDTO} (API DTO).
     * Enriches with card metadata (name, subtypes) from the card cache.
     */
    private CardInstanceDTO toCardInstanceDTO(CardInstanceState cis) {
        if (cis == null) return null;

        String name = cis.getName();
        List<String> subtypes = cis.getSubtypes();
        String supertype = null;

        // Enrich from card cache if not already populated
        try {
            Card card = cardCacheService.findById(cis.getCardId());
            if (card != null) {
                if (name == null) {
                    name = card.getName();
                }
                if (subtypes == null) {
                    subtypes = card.getSubtypes();
                }
                // supertype ("Pokémon"/"Energy"/"Trainer") lets the FE classify hand cards
                supertype = card.getSupertype();
            }
        } catch (IllegalArgumentException e) {
            // Card not in cache, use what we have
        }

        CardInstanceDTO dto = new CardInstanceDTO(cis.getInstanceId(), cis.getCardId(), name, subtypes);
        dto.setSupertype(supertype);
        return dto;
    }

    // ================================================================
    //  Event conversion (engine DTO → domain event)
    // ================================================================

    /**
     * Converts engine-emitted {@link GameEventDTO}s to domain {@link GameEvent}s
     * for the WebSocket publisher.
     */
    private List<GameEvent> toDomainEvents(UUID gameId, List<GameEventDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(dto -> {
            GameEventType type;
            try {
                type = GameEventType.valueOf(dto.getType());
            } catch (Exception e) {
                type = GameEventType.PHASE_CHANGED;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) dto.getPayload();
            return GameEvent.of(gameId, type, "Engine event", payload);
        }).collect(Collectors.toList());
    }
}
