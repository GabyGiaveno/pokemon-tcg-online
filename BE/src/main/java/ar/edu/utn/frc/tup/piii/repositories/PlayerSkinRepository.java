package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PlayerSkin;
import ar.edu.utn.frc.tup.piii.entities.PlayerSkinId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data JPA repository for PlayerSkin junction. */
public interface PlayerSkinRepository extends JpaRepository<PlayerSkin, PlayerSkinId> {
    List<PlayerSkin> findByPlayerId(Long playerId);

    Optional<PlayerSkin> findByPlayerIdAndEquippedTrue(Long playerId);
}
