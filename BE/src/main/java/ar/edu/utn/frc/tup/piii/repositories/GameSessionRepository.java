package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for GameSession; provides findByStatus. */
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    List<GameSession> findByStatus(GameStatus status);
}
