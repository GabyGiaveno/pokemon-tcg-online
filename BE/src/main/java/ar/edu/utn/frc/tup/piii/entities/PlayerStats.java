package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: 1:1 competitive stats for a player. */
@Entity
@Table(name = "player_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats {

    @Id
    private Long playerId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(nullable = false)
    @Builder.Default
    private int wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private int losses = 0;

    @Column(nullable = false)
    @Builder.Default
    private int streak = 0;

    @Column(name = "tournaments_won", nullable = false)
    @Builder.Default
    private int tournamentsWon = 0;

    @Column(name = "packs_opened", nullable = false)
    @Builder.Default
    private int packsOpened = 0;

    @Column(name = "total_cards", nullable = false)
    @Builder.Default
    private int totalCards = 0;

    @Column(name = "decks_created", nullable = false)
    @Builder.Default
    private int decksCreated = 0;
}
