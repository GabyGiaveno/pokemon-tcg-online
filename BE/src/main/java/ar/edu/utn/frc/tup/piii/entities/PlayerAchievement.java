package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** JPA entity: junction — which achievements a player has unlocked. */
@Entity
@Table(name = "player_achievement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAchievement {

    @EmbeddedId
    private PlayerAchievementId id;

    @ManyToOne(optional = false)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @MapsId("achievementId")
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @Column(name = "unlocked_at", nullable = false)
    @Builder.Default
    private LocalDateTime unlockedAt = LocalDateTime.now();
}
