package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.response.GameEventDTO;
import ar.edu.utn.frc.tup.piii.models.game.state.GameBoardState;
import lombok.Data;

import java.util.List;

@Data
public class GameEngineResult {
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private GameBoardState updatedState;
    private List<GameEventDTO> events;
    private boolean gameFinished;
    private Long winnerPlayerId;
    private String finishedReason;
}
