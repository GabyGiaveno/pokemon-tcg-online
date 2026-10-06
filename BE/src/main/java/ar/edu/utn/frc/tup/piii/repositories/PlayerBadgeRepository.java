package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PlayerBadge;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data JPA repository for PlayerBadge junction. */
public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, PlayerBadgeId> {
    List<PlayerBadge> findByPlayerId(Long playerId);
}
