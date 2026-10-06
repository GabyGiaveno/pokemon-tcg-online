package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PlayerCustomization;
import ar.edu.utn.frc.tup.piii.entities.PlayerCustomizationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data JPA repository for PlayerCustomization junction. */
public interface PlayerCustomizationRepository extends JpaRepository<PlayerCustomization, PlayerCustomizationId> {
    List<PlayerCustomization> findByPlayerId(Long playerId);
}
