package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.entities.GameAction;
import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.repositories.GameActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists {@link GameAction} log entries in a separate transaction so that
 * the INSERT commits even when the outer business transaction rolls back.
 */
@Service
public class GameActionLogService {

    private final GameActionRepository gameActionRepository;
    private final ObjectMapper objectMapper;

    public GameActionLogService(GameActionRepository gameActionRepository, ObjectMapper objectMapper) {
        this.gameActionRepository = gameActionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Logs a successful action. Result JSON: {@code {"status":"SUCCESS","events":[...]}}.
     *
     * @param session    the game session this action belongs to
     * @param request    the internal action request that was processed
     * @param playerId   the player who performed the action
     * @param turnNumber the turn on which the action occurred
     * @param events     serializable list of events produced (may be null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(GameSession session, ActionRequest request,
                           Long playerId, int turnNumber, Object events) {
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(
                    new java.util.LinkedHashMap<String, Object>() {{
                        put("status", "SUCCESS");
                        put("events", events != null ? events : new java.util.ArrayList<>());
                    }}
            );
        } catch (JsonProcessingException e) {
            resultJson = "{\"status\":\"SUCCESS\"}";
        }
        save(session, request, playerId, turnNumber, resultJson);
    }

    /**
     * Logs a failed action. Result JSON: {@code {"status":"FAILURE","message":"..."}}.
     *
     * @param session    the game session this action belongs to
     * @param actionType the type of action that was attempted
     * @param playerId   the player who attempted the action
     * @param turnNumber the turn on which the attempt occurred
     * @param message    the engine error message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(GameSession session, ActionType actionType,
                           Long playerId, int turnNumber, String message) {
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(
                    new java.util.LinkedHashMap<String, Object>() {{
                        put("status", "FAILURE");
                        put("message", message != null ? message : "");
                    }}
            );
        } catch (JsonProcessingException e) {
            resultJson = "{\"status\":\"FAILURE\"}";
        }

        // Build a minimal ActionRequest so save() can extract the actionType.
        ActionRequest stub = ActionRequest.builder().type(actionType).build();
        save(session, stub, playerId, turnNumber, resultJson);
    }

    // ----------------------------------------------------------------
    //  Internal helpers
    // ----------------------------------------------------------------

    private void save(GameSession session, ActionRequest request,
                      Long playerId, int turnNumber, String resultJson) {
        String payloadStr;
        try {
            payloadStr = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            payloadStr = "{}";
        }

        GameAction action = GameAction.builder()
                .gameSession(session)
                .turnNumber(turnNumber)
                .playerId(playerId)
                .actionType(request.getType())
                .payload(payloadStr)
                .result(resultJson)
                .timestamp(LocalDateTime.now())
                .build();
        gameActionRepository.save(action);
    }
}
