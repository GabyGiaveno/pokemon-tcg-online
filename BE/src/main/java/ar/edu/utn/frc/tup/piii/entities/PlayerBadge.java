package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** JPA entity: junction — which badges a player has unlocked. */
@Entity
@Table(name = "player_badge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBadge {

    @EmbeddedId
    private PlayerBadgeId id;

    @ManyToOne(optional = false)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @MapsId("badgeId")
    @JoinColumn(name = "badge_id")
    private Badge badge;

    @Column(name = "unlocked_at", nullable = false)
    @Builder.Default
    private LocalDateTime unlockedAt = LocalDateTime.now();
}
