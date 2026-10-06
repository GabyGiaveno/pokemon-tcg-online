package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/* JPA entity: game session with UUID id, player FKs, status enum, prizeCardsCount CHECK IN (1,6). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "game_session")
public class GameSession {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Player player1;

    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Player player2;

    @ManyToOne
    @JoinColumn(name = "deck1_id")
    private Deck deck1;

    @ManyToOne
    @JoinColumn(name = "deck2_id")
    private Deck deck2;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private Long currentPlayerId;

    /** READY_CHECK: whether each player has pressed "Listo". */
    @Column(nullable = false)
    private boolean player1Ready;

    @Column(nullable = false)
    private boolean player2Ready;

    /** Winner of the opening coin flip — the only player allowed to choose who starts. */
    private Long coinFlipWinnerId;

    @Column(nullable = false)
    private int prizeCardsCount;

    @Column(name = "sudden_death_round", nullable = false)
    private int suddenDeathRound;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    @OneToOne(mappedBy="gameSession")
    GameState gameState;

    @OneToMany(mappedBy="gameSession")
    List<GameAction> actions;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            this.id = UUID.randomUUID();
        }
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

}
