package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.entities.GameState;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameSessionRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameStateRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.engine.GameEngineFacade;
import ar.edu.utn.frc.tup.piii.services.AchievementUnlockService;
import ar.edu.utn.frc.tup.piii.services.BadgeUnlockService;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import ar.edu.utn.frc.tup.piii.services.GameActionLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc(addFilters = false)
class GameLifecycleIntegrationTest {

    private static final String BASIC_CARD_ID = "xy1-lifecycle-basic";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private GameStateRepository gameStateRepository;

    @MockitoBean
    private CurrentPlayerService currentPlayerService;

    @MockitoBean
    private CardCacheService cardCacheService;

    @MockitoBean
    private GameEventPublisher gameEventPublisher;

    @MockitoBean
    private GameEngineFacade gameEngineFacade;

    @MockitoBean
    private BadgeUnlockService badgeUnlockService;

    @MockitoBean
    private AchievementUnlockService achievementUnlockService;

    @MockitoBean
    private GameActionLogService gameActionLogService;

    private Player player1;
    private Player player2;
    private Deck deck1;
    private Deck deck2;

    @BeforeEach
    void setUp() {
        gameStateRepository.deleteAll();
        gameSessionRepository.deleteAll();
        deckRepository.deleteAll();
        playerRepository.deleteAll();
        reset(currentPlayerService, cardCacheService, gameEventPublisher, gameEngineFacade,
                badgeUnlockService, achievementUnlockService, gameActionLogService);

        Card basicCard = Card.builder()
                .id(BASIC_CARD_ID)
                .name("Bulbasaur")
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .build();

        when(cardCacheService.findById(BASIC_CARD_ID)).thenReturn(basicCard);
        when(cardCacheService.findById(anyString())).thenReturn(basicCard);

        player1 = playerRepository.save(player("ash", "ash@example.com"));
        player2 = playerRepository.save(player("misty", "misty@example.com"));
        deck1 = deckRepository.save(deck(player1, "Ash Deck"));
        deck2 = deckRepository.save(deck(player2, "Misty Deck"));
    }

    @Test
    void createJoinReadyAndSetupFlow_persistsInitialBoardState() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(player1.getId());

        JsonNode createResponse = objectMapper.readTree(mockMvc.perform(post("/api/games")
                        .contentType(APPLICATION_JSON)
                        .content("{\"deckId\":" + deck1.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        String gameId = createResponse.path("gameId").asText();
        assertNotNull(gameId);

        when(currentPlayerService.getCurrentPlayerId()).thenReturn(player2.getId());
        mockMvc.perform(post("/api/games/{id}/join", gameId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deckId\":" + deck2.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_CHECK"));

        when(currentPlayerService.getCurrentPlayerId()).thenReturn(player1.getId());
        mockMvc.perform(post("/api/games/{id}/ready", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_CHECK"))
                .andExpect(jsonPath("$.myReady").value(true))
                .andExpect(jsonPath("$.opponentReady").value(false));

        when(currentPlayerService.getCurrentPlayerId()).thenReturn(player2.getId());
        JsonNode readyResponse = objectMapper.readTree(mockMvc.perform(post("/api/games/{id}/ready", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_CHECK"))
                .andExpect(jsonPath("$.coinFlipWinnerId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString());

        long coinFlipWinnerId = readyResponse.path("coinFlipWinnerId").asLong();
        long starterId = coinFlipWinnerId == player1.getId() ? player2.getId() : player1.getId();

        when(currentPlayerService.getCurrentPlayerId()).thenReturn(coinFlipWinnerId);
        mockMvc.perform(post("/api/games/{id}/choose-first", gameId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"starter\":\"OPPONENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETUP"))
                .andExpect(jsonPath("$.currentPlayerId").value(starterId));

        when(currentPlayerService.getCurrentPlayerId()).thenReturn(starterId);
        mockMvc.perform(get("/api/games/{id}/state", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETUP"))
                .andExpect(jsonPath("$.phase").value("SETUP"))
                .andExpect(jsonPath("$.currentPlayerId").value(starterId))
                .andExpect(jsonPath("$.myTurn").value(true))
                .andExpect(jsonPath("$.myField.hand.length()").value(7))
                .andExpect(jsonPath("$.myField.deckSize").value(47))
                .andExpect(jsonPath("$.myField.prizeCards.length()").value(6))
                .andExpect(jsonPath("$.opponentField.handSize").value(7))
                .andExpect(jsonPath("$.opponentField.deckSize").value(47))
                .andExpect(jsonPath("$.opponentField.prizeCards.length()").value(6));

        GameSession session = gameSessionRepository.findById(java.util.UUID.fromString(gameId)).orElseThrow();
        GameState state = gameStateRepository.findByGameSessionId(session.getId()).orElseThrow();
        assertNotNull(state.getStateJson());
    }

    private Player player(String username, String email) {
        return Player.builder()
                .username(username)
                .email(email)
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Deck deck(Player player, String name) {
        Deck deck = Deck.builder()
                .name(name)
                .player(player)
                .isValid(true)
                .cards(new ArrayList<>())
                .build();

        DeckCard deckCard = DeckCard.builder()
                .deck(deck)
                .cardId(BASIC_CARD_ID)
                .quantity(60)
                .build();

        deck.setCards(List.of(deckCard));
        return deck;
    }
}
