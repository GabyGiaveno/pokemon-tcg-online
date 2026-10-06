package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/* JPA entity: full board snapshot as JSONB in 1:1 relationship with GameSession. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "game_state")
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name="game_session_id")
    private GameSession gameSession;

    @Column(name = "state_json", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String stateJson;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private TurnPhase turnPhase;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

}
