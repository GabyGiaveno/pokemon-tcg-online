package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for GameAction. */
public interface GameActionRepository extends JpaRepository<GameAction, Long> {

    List<GameAction> findByGameSession_IdOrderByTurnNumberAscIdAsc(UUID gameSessionId);
}
