package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** JPA entity: immutable action log entry with actionType enum, payload JSON, result JSON. */
@Entity
@Table(name = "game_action")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "game_session_id", nullable = false, updatable = false)
	private GameSession gameSession;

	@Column(name = "turn_number", nullable = false, updatable = false)
	private int turnNumber;

	// store player id directly (FK exists in DB). Immutable after insert.
	@Column(name = "player_id", nullable = false, updatable = false)
	private Long playerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false, updatable = false)
	private ActionType actionType;

	@Column(name = "payload", nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String payload;

	@Column(name = "result", nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String result;

	@Column(name = "timestamp", nullable = false, updatable = false)
	private LocalDateTime timestamp;

}
