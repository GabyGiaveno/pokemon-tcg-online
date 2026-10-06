package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.ChooseFirstRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.CreateGameRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.GameActionApiRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.JoinGameRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.BoardStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.response.GameActionApiResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.GameActionDto;
import ar.edu.utn.frc.tup.piii.dtos.response.GameSessionResponse;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.services.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Handles game session creation, join, state, and action endpoints (/api/games). */
@Tag(name = "Game", description = "Game session lifecycle and real-time action execution. All endpoints require authentication.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final CurrentPlayerService currentPlayerService;

    public GameController(GameService gameService, CurrentPlayerService currentPlayerService) {
        this.gameService = gameService;
        this.currentPlayerService = currentPlayerService;
    }

    @Operation(summary = "Create game", description = "Creates a new game session in WAITING state. The creator is player 1.")
    @ApiResponse(responseCode = "201", description = "Game created")
    @PostMapping
    public ResponseEntity<GameSessionResponse> createGame(@RequestBody CreateGameRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        GameSessionResponse response = gameService.createGame(playerId, request.getDeckId());
        return ResponseEntity.created(URI.create("/api/games/" + response.getGameId()))
                .body(response);
    }

    @Operation(summary = "List waiting games", description = "Returns all game sessions in WAITING state (open lobby).")
    @ApiResponse(responseCode = "200", description = "List of waiting games")
    @GetMapping
    public ResponseEntity<List<GameSessionResponse>> getWaitingGames() {
        List<GameSessionResponse> games = gameService.getWaitingGames();
        return ResponseEntity.ok(games);
    }

    @Operation(summary = "Join game", description = "Joins a WAITING game as player 2. Transitions the session to COIN_FLIP.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined successfully"),
        @ApiResponse(responseCode = "409", description = "Game is not in WAITING state or player already in game")
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<GameSessionResponse> joinGame(
            @Parameter(description = "Game session UUID") @PathVariable UUID id,
            @RequestBody JoinGameRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        GameSessionResponse response = gameService.joinGame(id, playerId, request.getDeckId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get board state", description = "Returns the current board state filtered for the requesting player: full hand visible for self, hand size only for opponent.")
    @ApiResponse(responseCode = "200", description = "Filtered board state")
    @GetMapping("/{id}/state")
    public ResponseEntity<BoardStateDTO> getGameState(
            @Parameter(description = "Game session UUID") @PathVariable UUID id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        BoardStateDTO state = gameService.getGameState(id, playerId);
        return ResponseEntity.ok(state);
    }

    @Operation(summary = "Mark ready", description = "Marks the player as ready. When both players are ready, the server executes the opening coin flip.")
    @ApiResponse(responseCode = "200", description = "Updated board state")
    @PostMapping("/{id}/ready")
    public ResponseEntity<BoardStateDTO> markReady(
            @Parameter(description = "Game session UUID") @PathVariable UUID id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(gameService.markReady(id, playerId));
    }

    @Operation(summary = "Choose first player", description = "Called by the coin-flip winner to choose who goes first. Advances the game to SETUP phase.")
    @ApiResponse(responseCode = "200", description = "Updated board state")
    @PostMapping("/{id}/choose-first")
    public ResponseEntity<BoardStateDTO> chooseFirst(
            @PathVariable UUID id,
            @RequestBody ChooseFirstRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(gameService.chooseFirstPlayer(id, playerId, request.getStarter()));
    }

    /**
     * Executes a game action.
     *
     * <p>Accepts the flat frontend-facing {@link GameActionApiRequest}.
     * The service translates it to the internal {@code ActionRequest} the
     * facade expects, so the controller never deals with engine internals.
     *
     * <p>All game-rule validation (wrong phase, card not in hand, etc.) is
     * resolved inside {@code GameEngineFacade} and returned as
     * {@code success: false} — not as HTTP errors.
     *
     * <p>On success the response includes a list of {@code GameEventDTO}s.
     * For the full filtered state, the frontend should listen for the
     * {@code state-changed} WebSocket notification and call
     * {@code GET /api/games/{id}/state}.
     */
    @Operation(summary = "Get action log", description = "Returns the full immutable action log for a game. Requester must be a participant.")
    @ApiResponse(responseCode = "200", description = "Action log")
    @GetMapping("/{id}/actions")
    public ResponseEntity<List<GameActionDto>> getActionLog(
            @Parameter(description = "Game session UUID") @PathVariable UUID id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(gameService.getActionLog(id, playerId));
    }

    @Operation(summary = "Perform game action",
        description = "Executes a game action (PLAY_CARD, ATTACH_ENERGY, EVOLVE, RETREAT, ATTACK, USE_ABILITY, END_TURN, CONCEDE, RESOLVE_SELECTION). "
            + "Rule validation errors return success: false rather than HTTP error codes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Action result with events list"),
        @ApiResponse(responseCode = "403", description = "Player is not a participant in this game")
    })
    @PostMapping("/{id}/actions")
    public ResponseEntity<GameActionApiResponse> performGameAction(
            @Parameter(description = "Game session UUID") @PathVariable UUID id,
            @RequestBody GameActionApiRequest actionRequest) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        GameActionApiResponse response = gameService.performGameAction(id, actionRequest, playerId);
        return ResponseEntity.ok(response);
    }
}
