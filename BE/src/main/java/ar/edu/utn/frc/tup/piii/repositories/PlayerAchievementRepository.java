package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PlayerAchievement;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievementId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data JPA repository for PlayerAchievement junction. */
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, PlayerAchievementId> {
    List<PlayerAchievement> findByPlayerId(Long playerId);
}
