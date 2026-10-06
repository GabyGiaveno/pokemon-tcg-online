package ar.edu.utn.frc.tup.piii.dtos.response;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Read-only projection of a {@link ar.edu.utn.frc.tup.piii.entities.GameAction} log entry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionDto {
    private Long id;
    private int turnNumber;
    private Long playerId;
    private ActionType actionType;
    private String payload;
    private String result;
    private LocalDateTime timestamp;
}
