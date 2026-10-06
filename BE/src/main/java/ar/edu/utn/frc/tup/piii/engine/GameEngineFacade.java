package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.GameEventDTO;
import ar.edu.utn.frc.tup.piii.engine.mappers.EngineStateMapper;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.game.state.GameBoardState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GameEngineFacade acts as an Adapter between the persistence/DTO layer and the pure Engine layer.
 */
@Component
public class GameEngineFacade {

    private final TurnManager turnManager;

    public GameEngineFacade(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    /**
     * Apply a game action and return the result.
     *
     * @param state      the current mutable board state
     * @param playerId   the player performing the action
     * @param request    the action request (type + payload)
     * @param cardLookup bridge to the DB to fetch card details dynamically
     * @return engine result with updated state, events, and finish info
     */
    public GameEngineResult applyAction(GameBoardState state, Long playerId,
                                        ActionRequest request, CardLookup cardLookup) {

        // 1. DTO -> Domain Mapper
        BoardState domainState = EngineStateMapper.toDomainState(state);

        // 2. Execute Engine Logic (TurnManager takes Domain Request)
        ActionResult engineResult = turnManager.processAction(request, domainState, playerId, cardLookup);

        // 3. Domain -> DTO Mapper
        if (engineResult.isSuccess()) {
            EngineStateMapper.updateDbState(domainState, state);
        }

        // 4. Convert generic GameEvent to GameEventDTO
        List<GameEventDTO> eventDTOs = null;
        if (engineResult.getEvents() != null) {
            eventDTOs = engineResult.getEvents().stream()
                    .map(e -> {
                        GameEventDTO dto = new GameEventDTO();
                        dto.setType(e.getType() != null ? e.getType().toString() : "UNKNOWN");
                        dto.setPayload(e.getData());
                        dto.setTimestamp(LocalDateTime.now());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } else {
            eventDTOs = List.of();
        }

        // 5. Build the Response
        GameEngineResult result = new GameEngineResult();
        result.setSuccess(engineResult.isSuccess());
        
        if (!engineResult.isSuccess()) {
            result.setErrorCode("ERROR");
            result.setErrorMessage(engineResult.getError());
            return result;
        }

        result.setUpdatedState(state);
        result.setEvents(eventDTOs);
        
        // MVP: if engineResult doesn't have gameFinished getters, we infer it from matchState
        // Or if TurnManager has them, we map them here. TurnManager doesn't expose winner on ActionResult yet
        // so we check state.getFinishedReason()
        if (state.getFinishedReason() != null) {
            result.setGameFinished(true);
            result.setWinnerPlayerId(state.getWinnerPlayerId());
            result.setFinishedReason(state.getFinishedReason());
        }

        return result;
    }
}
