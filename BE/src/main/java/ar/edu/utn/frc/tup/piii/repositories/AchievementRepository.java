package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for Achievement catalogue. */
public interface AchievementRepository extends JpaRepository<Achievement, String> {
}
