package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateGameRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.GameActionApiRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.JoinGameRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.ChooseFirstRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.BoardStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.response.GameActionApiResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.GameActionDto;
import ar.edu.utn.frc.tup.piii.dtos.response.GameEventDTO;
import ar.edu.utn.frc.tup.piii.dtos.response.GameSessionResponse;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenException;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.exceptions.DeckValidationException;
import ar.edu.utn.frc.tup.piii.exceptions.GameNotFoundException;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import ar.edu.utn.frc.tup.piii.services.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GameController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private CurrentPlayerService currentPlayerService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createGame_returnsCreatedSession() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(gameService.createGame(7L, 11L)).thenReturn(gameSessionResponse(UUID.fromString("11111111-1111-1111-1111-111111111111"), GameStatus.WAITING));

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":11}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(gameService).createGame(7L, 11L);
    }

    @Test
    void createGame_whenInvalidDeck_returns422() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(gameService.createGame(7L, 11L)).thenThrow(new DeckValidationException("Deck 11 is invalid"));

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":11}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void getWaitingGames_returnsList() throws Exception {
        when(gameService.getWaitingGames()).thenReturn(List.of(gameSessionResponse(UUID.fromString("22222222-2222-2222-2222-222222222222"), GameStatus.WAITING)));

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameId").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void joinGame_whenForbidden_returns403() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(gameService.joinGame(eq(gameId), eq(7L), eq(11L)))
                .thenThrow(new ForbiddenException("Player 7 cannot join their own game"));

        mockMvc.perform(post("/api/games/{id}/join", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":11}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void joinGame_whenSessionNotWaiting_returns409() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(gameService.joinGame(eq(gameId), eq(7L), eq(11L)))
                .thenThrow(new IllegalStateException("Game is not waiting for players"));

        mockMvc.perform(post("/api/games/{id}/join", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":11}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void joinGame_returnsJoinedSession() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(gameService.joinGame(eq(gameId), eq(7L), eq(11L)))
                .thenReturn(gameSessionResponse(gameId, GameStatus.WAITING));

        mockMvc.perform(post("/api/games/{id}/join", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(gameService).joinGame(eq(gameId), eq(7L), eq(11L));
    }

    @Test
    void getGameState_returnsBoardState() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(gameService.getGameState(gameId, 7L)).thenReturn(boardState(gameId.toString()));

        mockMvc.perform(get("/api/games/{id}/state", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("44444444-4444-4444-4444-444444444444"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.myTurn").value(false));
    }

    @Test
    void getGameState_whenMissing_returns404() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(gameService.getGameState(gameId, 7L)).thenThrow(new GameNotFoundException("Game not found: " + gameId));

        mockMvc.perform(get("/api/games/{id}/state", gameId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void markReady_returnsReadyState() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(gameService.markReady(gameId, 7L)).thenReturn(readyState(gameId.toString(), true, false, 7L));

        mockMvc.perform(post("/api/games/{id}/ready", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_CHECK"))
                .andExpect(jsonPath("$.myReady").value(true))
                .andExpect(jsonPath("$.opponentReady").value(false))
                .andExpect(jsonPath("$.coinFlipWinnerId").value(7));
    }

    @Test
    void chooseFirst_returnsSetupState() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(gameService.chooseFirstPlayer(gameId, 7L, "ME")).thenReturn(setupState(gameId.toString()));

        mockMvc.perform(post("/api/games/{id}/choose-first", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"starter\":\"ME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETUP"))
                .andExpect(jsonPath("$.currentPlayerId").value(7));
    }

    @Test
    void performGameAction_returnsActionResponse() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(gameService.performGameAction(eq(gameId), org.mockito.ArgumentMatchers.any(GameActionApiRequest.class), eq(7L)))
                .thenReturn(GameActionApiResponse.ok(ActionType.END_TURN, List.of()));

        mockMvc.perform(post("/api/games/{id}/actions", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"END_TURN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.actionType").value("END_TURN"));
    }

    @Test
    void performGameAction_whenEngineRejects_returnsFailurePayload() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(gameService.performGameAction(eq(gameId), any(GameActionApiRequest.class), eq(7L)))
                .thenReturn(GameActionApiResponse.fail(ActionType.END_TURN, "Not your turn"));

        mockMvc.perform(post("/api/games/{id}/actions", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"END_TURN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not your turn"));
    }

    @Test
    void getActionLog_returnsListOfActions() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        UUID gameId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        GameActionDto entry = GameActionDto.builder()
                .id(1L)
                .turnNumber(2)
                .playerId(7L)
                .actionType(ActionType.END_TURN)
                .payload("{}")
                .result("{\"status\":\"SUCCESS\"}")
                .timestamp(java.time.LocalDateTime.of(2026, 6, 27, 10, 0, 0))
                .build();

        when(gameService.getActionLog(gameId, 7L)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/games/{id}/actions", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].turnNumber").value(2))
                .andExpect(jsonPath("$[0].playerId").value(7))
                .andExpect(jsonPath("$[0].actionType").value("END_TURN"));
    }

    @Test
    void getActionLog_whenNotParticipant_returns403() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(99L);
        UUID gameId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(gameService.getActionLog(gameId, 99L))
                .thenThrow(new ForbiddenException("Player 99 does not belong to game"));

        mockMvc.perform(get("/api/games/{id}/actions", gameId))
                .andExpect(status().isForbidden());
    }

    private GameSessionResponse gameSessionResponse(UUID id, GameStatus status) {
        GameSessionResponse response = new GameSessionResponse();
        response.setGameId(id);
        response.setStatus(status);
        response.setPlayer1Username("ash");
        response.setPlayer2Username("misty");
        response.setCreatedAt(LocalDateTime.of(2026, 6, 24, 18, 0, 0));
        response.setPrizeCardsCount(6);
        return response;
    }

    private BoardStateDTO boardState(String gameId) {
        BoardStateDTO dto = new BoardStateDTO();
        dto.setGameId(gameId);
        dto.setStatus(GameStatus.WAITING);
        dto.setMyTurn(false);
        return dto;
    }

    private BoardStateDTO readyState(String gameId, boolean myReady, boolean opponentReady, Long winnerId) {
        BoardStateDTO dto = new BoardStateDTO();
        dto.setGameId(gameId);
        dto.setStatus(GameStatus.READY_CHECK);
        dto.setMyReady(myReady);
        dto.setOpponentReady(opponentReady);
        dto.setCoinFlipWinnerId(winnerId);
        return dto;
    }

    private BoardStateDTO setupState(String gameId) {
        BoardStateDTO dto = new BoardStateDTO();
        dto.setGameId(gameId);
        dto.setStatus(GameStatus.SETUP);
        dto.setCurrentPlayerId(7L);
        dto.setMyTurn(true);
        return dto;
    }
}
