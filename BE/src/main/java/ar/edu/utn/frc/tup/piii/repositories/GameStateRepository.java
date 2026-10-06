package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for GameState; provides findByGameSessionId. */
public interface GameStateRepository extends JpaRepository<GameState, Long> {
    Optional<GameState> findByGameSessionId(UUID gameSessionId);
}
